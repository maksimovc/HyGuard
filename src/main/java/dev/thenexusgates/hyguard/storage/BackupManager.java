package dev.thenexusgates.hyguard.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BackupManager {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final Path dataDirectory;
    private final Logger logger;
    private final ExecutorService backupExecutor;
    private final ScheduledExecutorService scheduler;

    private volatile ScheduledFuture<?> autoBackupTask;
    private volatile long configuredIntervalMinutes;
    private volatile int configuredMaxBackups = Integer.MAX_VALUE;

    public BackupManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.backupExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HyGuard-Backup");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HyGuard-BackupScheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void configureAutomaticBackups(long intervalMinutes, int maxBackups) {
        start(intervalMinutes, maxBackups);
    }

    public synchronized void start(long intervalMinutes, int maxBackups) {
        configuredIntervalMinutes = Math.max(0L, intervalMinutes);
        configuredMaxBackups = maxBackups <= 0 ? Integer.MAX_VALUE : maxBackups;
        if (autoBackupTask != null) {
            autoBackupTask.cancel(false);
            autoBackupTask = null;
        }
        if (configuredIntervalMinutes <= 0L) {
            logger.log(Level.INFO, "[HyGuard] Automatic backups disabled.");
            return;
        }

        autoBackupTask = scheduler.scheduleAtFixedRate(
                () -> performBackup("auto", null, null, configuredMaxBackups),
                configuredIntervalMinutes,
                configuredIntervalMinutes,
                TimeUnit.MINUTES
        );
        logger.log(Level.INFO, "[HyGuard] Automatic backups scheduled every " + configuredIntervalMinutes + " minute(s). Retention=" + configuredMaxBackups + ".");
    }

    public synchronized void stop() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel(false);
            autoBackupTask = null;
        }
    }

    public void performBackup(String label, Consumer<Path> onSuccess, Consumer<Throwable> onFailure) {
        performBackup(label, onSuccess, onFailure, configuredMaxBackups);
    }

    public void performBackup(String label,
                              Consumer<Path> onSuccess,
                              Consumer<Throwable> onFailure,
                              int maxBackups) {
        backupExecutor.execute(() -> {
            try {
                Path backupPath = createBackup(label);
                pruneBackups(maxBackups);
                logger.log(Level.INFO, "[HyGuard] Backup completed: " + backupPath);
                if (onSuccess != null) {
                    onSuccess.accept(backupPath);
                }
            } catch (Throwable throwable) {
                logger.log(Level.WARNING, "[HyGuard] Backup failed.", throwable);
                if (onFailure != null) {
                    onFailure.accept(throwable);
                }
            }
        });
    }

    public void flush() {
        try {
            backupExecutor.submit(() -> { }).get();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[HyGuard] Failed to flush backup executor.", exception);
        }
    }

    public void shutdown() {
        stop();
        flush();
        backupExecutor.shutdown();
        scheduler.shutdown();
    }

    private Path createBackup(String label) throws IOException {
        Path source = dataDirectory.resolve("regions");
        Path backupsDirectory = dataDirectory.resolve("backups");
        Files.createDirectories(backupsDirectory);

        String baseName = label + "_" + FORMATTER.format(LocalDateTime.now());
        Path destination = backupsDirectory.resolve(baseName);
        int suffix = 2;
        while (Files.exists(destination)) {
            destination = backupsDirectory.resolve(baseName + "_" + suffix++);
        }
        Files.createDirectories(destination);

        if (!Files.exists(source)) {
            return destination;
        }

        try (var paths = Files.walk(source)) {
            for (Path current : (Iterable<Path>) paths.sorted(Comparator.naturalOrder())::iterator) {
                Path relative = source.relativize(current);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(current)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(current, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return destination;
    }

    public void pruneOldBackups(int maxBackups) throws IOException {
        if (maxBackups <= 0) {
            return;
        }

        Path backupsDirectory = dataDirectory.resolve("backups");
        if (!Files.isDirectory(backupsDirectory)) {
            return;
        }

        ArrayList<Path> backups = new ArrayList<>();
        try (var children = Files.list(backupsDirectory)) {
            children.filter(Files::isDirectory).forEach(backups::add);
        }
        if (backups.size() <= maxBackups) {
            return;
        }

        backups.sort(Comparator.comparingLong(this::lastModified));
        for (int index = 0; index < backups.size() - maxBackups; index++) {
            deleteRecursively(backups.get(index));
        }
    }

    private void pruneBackups(int maxBackups) throws IOException {
        pruneOldBackups(maxBackups);
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ioException) {
            return Long.MIN_VALUE;
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path current : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(current);
            }
        }
    }
}