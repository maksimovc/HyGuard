package dev.thenexusgates.hyguard.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerDirectory {

    public record StoredPlayer(String uuid, String username, long lastSeenAt) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path playersDirectory;
    private final Logger logger;
    private final ExecutorService saveExecutor;
    private final java.util.concurrent.ConcurrentHashMap<String, StoredPlayer> byUuid = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, String> uuidByName = new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerDirectory(Path dataDirectory, Logger logger) {
        this.playersDirectory = dataDirectory.resolve("players");
        this.logger = logger;
        this.saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HyGuard-PlayerDirectorySave");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void loadAll() {
        byUuid.clear();
        uuidByName.clear();
        try {
            Files.createDirectories(playersDirectory);
            try (var files = Files.list(playersDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(this::loadPlayer);
            }
        } catch (IOException ioException) {
            logger.log(Level.WARNING, "[HyGuard] Failed to load player directory.", ioException);
        }
    }

    public void remember(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null || playerRef.getUsername() == null || playerRef.getUsername().isBlank()) {
            return;
        }
        remember(playerRef.getUuid().toString(), playerRef.getUsername());
    }

    public void remember(String uuid, String username) {
        if (uuid == null || uuid.isBlank() || username == null || username.isBlank()) {
            return;
        }

        StoredPlayer previous = byUuid.get(uuid);
        if (previous != null && previous.username() != null) {
            uuidByName.remove(normalize(previous.username()), uuid);
        }

        StoredPlayer storedPlayer = new StoredPlayer(uuid, username, System.currentTimeMillis());
        byUuid.put(uuid, storedPlayer);
        uuidByName.put(normalize(username), uuid);
        saveExecutor.execute(() -> savePlayer(storedPlayer));
    }

    public StoredPlayer findByName(String username) {
        String uuid = uuidByName.get(normalize(username));
        return uuid == null ? null : byUuid.get(uuid);
    }

    public void flush() {
        try {
            saveExecutor.submit(() -> { }).get();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[HyGuard] Failed to flush player directory saves.", exception);
        }
    }

    public void shutdown() {
        flush();
        saveExecutor.shutdown();
    }

    private void loadPlayer(Path path) {
        try {
            StoredPlayer storedPlayer = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), StoredPlayer.class);
            if (storedPlayer == null || storedPlayer.uuid() == null || storedPlayer.username() == null) {
                return;
            }
            byUuid.put(storedPlayer.uuid(), storedPlayer);
            uuidByName.put(normalize(storedPlayer.username()), storedPlayer.uuid());
        } catch (Exception exception) {
            logger.log(Level.WARNING, "[HyGuard] Failed to load player data file " + path, exception);
        }
    }

    private void savePlayer(StoredPlayer storedPlayer) {
        try {
            Files.createDirectories(playersDirectory);
            Path file = playersDirectory.resolve(storedPlayer.uuid() + ".json");
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(storedPlayer), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioException) {
            logger.log(Level.WARNING, "[HyGuard] Failed to save player directory entry for " + storedPlayer.username(), ioException);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}