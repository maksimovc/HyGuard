package dev.thenexusgates.hyguard.storage;

import dev.thenexusgates.hyguard.core.region.Region;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JsonRegionRepository implements RegionRepository {

    private final Path regionsDirectory;
    private final RegionSerializer serializer;
    private final MigrationManager migrationManager;
    private final Logger logger;
    private final ExecutorService saveExecutor;

    public JsonRegionRepository(Path dataDirectory, Logger logger) {
        this.regionsDirectory = dataDirectory.resolve("regions");
        this.serializer = new RegionSerializer();
        this.migrationManager = new MigrationManager();
        this.logger = logger;
        this.saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HyGuard-RegionSave");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public Collection<Region> loadAll() {
        ArrayList<Region> regions = new ArrayList<>();
        try {
            Files.createDirectories(regionsDirectory);
            try (var worldDirectories = Files.list(regionsDirectory)) {
                worldDirectories.filter(Files::isDirectory).forEach(worldDirectory -> {
                    try (var files = Files.list(worldDirectory)) {
                        files.filter(path -> path.getFileName().toString().endsWith(".json"))
                                .forEach(path -> loadRegion(path, regions));
                    } catch (IOException ioException) {
                        logger.log(Level.WARNING, "[HyGuard] Failed to list world region directory " + worldDirectory, ioException);
                    }
                });
            }
        } catch (IOException ioException) {
            logger.log(Level.WARNING, "[HyGuard] Failed to load region directory.", ioException);
        }
        return regions;
    }

    @Override
    public void saveRegionAsync(Region region) {
        saveExecutor.execute(() -> saveRegion(region));
    }

    @Override
    public void deleteRegionAsync(Region region) {
        saveExecutor.execute(() -> {
            try {
                Files.deleteIfExists(regionPath(region));
            } catch (IOException ioException) {
                logger.log(Level.WARNING, "[HyGuard] Failed to delete region " + region.getName(), ioException);
            }
        });
    }

    @Override
    public void flush() {
        try {
            saveExecutor.submit(() -> { }).get();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[HyGuard] Failed to flush pending region saves.", exception);
        }
    }

    @Override
    public void shutdown() {
        flush();
        saveExecutor.shutdown();
    }

    private void loadRegion(Path path, List<Region> target) {
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            Region region = serializer.deserialize(raw);
            if (region == null) {
                logger.log(Level.WARNING, "[HyGuard] Skipping unreadable region file " + path);
                return;
            }
            if (!migrationManager.supports(1)) {
                return;
            }
            target.add(region);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[HyGuard] Skipping corrupt region file " + path, exception);
        }
    }

    private void saveRegion(Region region) {
        Path temp = null;
        try {
            Path regionPath = regionPath(region);
            Files.createDirectories(regionPath.getParent());
            String json = serializer.serialize(region);
            temp = regionPath.resolveSibling(regionPath.getFileName() + ".tmp");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, regionPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                logger.log(Level.WARNING, "[HyGuard] Atomic region save unavailable for " + region.getName() + ". Falling back to replace move.", atomicMoveNotSupportedException);
                Files.move(temp, regionPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioException) {
            logger.log(Level.SEVERE, "[HyGuard] Failed to save region " + region.getName(), ioException);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException cleanupException) {
                    logger.log(Level.WARNING, "[HyGuard] Failed to clean temp region file " + temp, cleanupException);
                }
            }
        }
    }

    private Path regionPath(Region region) {
        return regionsDirectory.resolve(region.getWorldId()).resolve(region.getId() + ".json");
    }
}