package dev.thenexusgates.hyguard.asset;

import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.server.core.asset.AssetModule;
import dev.thenexusgates.hyguard.HyGuardPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class HyGuardAssetPack {

    private static final String PACK_ID = "thenexusgates:HyGuard";
    private static final String PACK_GROUP = "thenexusgates";
    private static final String PACK_NAME = "HyGuard";
    private static final String PACK_VERSION = "1.0.9";
    private static final String TARGET_SERVER_VERSION = "2026.03.26-89796e57b";
    private static final String PACK_DIRECTORY_NAME = "thenexusgates_HyGuard";
    private static final String DATA_DIRECTORY_NAME = "thenexusgates_HyGuardData";
    private static final Set<String> BUNDLED_PREFIXES = Set.of("Common/", "Server/");
    private static final List<String> MIGRATED_DATA_ENTRIES = List.of("config.json", "players", "regions", "backups");

    private final Logger logger;
    private final Path dataRoot;
    private final Path packRoot;

    private volatile boolean registered;

    private HyGuardAssetPack(Logger logger, Path dataRoot, Path packRoot) {
        this.logger = logger;
        this.dataRoot = dataRoot;
        this.packRoot = packRoot;
    }

    public static HyGuardAssetPack initialize(Logger logger) {
        try {
            Path pluginLocation = Paths.get(HyGuardPlugin.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path modsDirectory = Files.isDirectory(pluginLocation)
                    ? pluginLocation
                    : pluginLocation.getParent();
            Path dataRoot = modsDirectory.resolve(DATA_DIRECTORY_NAME);
            Path packRoot = modsDirectory.resolve(PACK_DIRECTORY_NAME);

            HyGuardAssetPack assetPack = new HyGuardAssetPack(logger, dataRoot, packRoot);
            assetPack.prepare(pluginLocation, modsDirectory);
            return assetPack;
        } catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException("Failed to initialize HyGuard asset pack", exception);
        }
    }

    public Path getDataRoot() {
        return dataRoot;
    }

    public Path getPackRoot() {
        return packRoot;
    }

    public void registerIfAvailable() {
        if (registered) {
            return;
        }

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            return;
        }
        if (assetModule.getAssetPack(PACK_ID) != null) {
            registered = true;
            return;
        }

        assetModule.registerPack(PACK_ID, packRoot, buildRuntimeManifest(), true);
        registered = true;
        logger.info("[HyGuard] Registered runtime asset pack: " + PACK_ID + " at " + packRoot);
    }

    private void prepare(Path pluginLocation, Path modsDirectory) throws IOException {
        Files.createDirectories(dataRoot);
        Files.createDirectories(packRoot);
        migrateLegacyData();
        writeBundledResources(pluginLocation);
        ensurePackEnabled(modsDirectory.getParent().resolve("config.json"));
        registerIfAvailable();
    }

    private void migrateLegacyData() throws IOException {
        if (dataRoot.equals(packRoot)) {
            return;
        }
        for (String entryName : MIGRATED_DATA_ENTRIES) {
            Path legacyPath = packRoot.resolve(entryName);
            if (!Files.exists(legacyPath)) {
                continue;
            }
            Path targetPath = dataRoot.resolve(entryName);
            moveRecursively(legacyPath, targetPath);
            logger.info("[HyGuard] Migrated legacy data entry from asset-pack path: " + entryName);
        }
    }

    private void writeBundledResources(Path pluginLocation) throws IOException {
        if (Files.isDirectory(pluginLocation)) {
            writeResourcesFromDirectory(resolveResourceRoot(pluginLocation));
            return;
        }

        try (JarFile jarFile = new JarFile(pluginLocation.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!shouldCopy(name)) {
                    continue;
                }
                Path destination = safeResolve(packRoot, name);
                Files.createDirectories(destination.getParent());
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path resolveResourceRoot(Path pluginLocation) throws IOException {
        URL manifestUrl = HyGuardAssetPack.class.getClassLoader().getResource("manifest.json");
        if (manifestUrl != null && "file".equalsIgnoreCase(manifestUrl.getProtocol())) {
            try {
                Path manifestPath = Paths.get(manifestUrl.toURI());
                Path resourceRoot = manifestPath.getParent();
                if (resourceRoot != null) {
                    return resourceRoot;
                }
            } catch (URISyntaxException ignored) {
            }
        }
        return pluginLocation;
    }

    private void writeResourcesFromDirectory(Path resourceRoot) throws IOException {
        for (String prefix : BUNDLED_PREFIXES) {
            Path sourceRoot = resourceRoot.resolve(prefix.replace('/', java.io.File.separatorChar));
            if (!Files.exists(sourceRoot)) {
                continue;
            }
            try (var paths = Files.walk(sourceRoot)) {
                for (Path current : (Iterable<Path>) paths::iterator) {
                    if (Files.isDirectory(current)) {
                        continue;
                    }
                    Path relative = resourceRoot.relativize(current);
                    String resourceName = relative.toString().replace(java.io.File.separatorChar, '/');
                    Path destination = safeResolve(packRoot, resourceName);
                    Files.createDirectories(destination.getParent());
                    Files.copy(current, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path manifestSource = resourceRoot.resolve("manifest.json");
        if (Files.exists(manifestSource)) {
            Files.copy(manifestSource, packRoot.resolve("manifest.json"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void ensurePackEnabled(Path configPath) {
        if (!Files.exists(configPath)) {
            return;
        }

        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            String updated = json;

            if (json.contains('"' + PACK_ID + '"')) {
                updated = json.replaceAll(
                        "(\\\"" + java.util.regex.Pattern.quote(PACK_ID) + "\\\"\\s*:\\s*\\{\\s*\\\"Enabled\\\"\\s*:\\s*)false",
                        "$1true");
            } else if (json.contains("\"Mods\": {")) {
                updated = json.replace(
                        "\"Mods\": {",
                        "\"Mods\": {\n    \"" + PACK_ID + "\": {\n      \"Enabled\": true\n    },");
            }

            if (!updated.equals(json)) {
                Files.writeString(configPath, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            logger.warning("[HyGuard] Failed to auto-enable HyGuard asset pack in config.json: "
                    + exception.getMessage());
        }
    }

    private static boolean shouldCopy(String resourceName) {
        if ("manifest.json".equals(resourceName)) {
            return true;
        }
        for (String prefix : BUNDLED_PREFIXES) {
            if (resourceName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Path safeResolve(Path root, String relativePath) {
        Path resolved = root.resolve(relativePath.replace('/', java.io.File.separatorChar)).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IllegalStateException("Blocked path traversal while writing asset-pack resource: " + relativePath);
        }
        return resolved;
    }

    private static void moveRecursively(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (var paths = Files.list(source)) {
                for (Path child : (Iterable<Path>) paths::iterator) {
                    moveRecursively(child, target.resolve(child.getFileName().toString()));
                }
            }
            Files.deleteIfExists(source);
            return;
        }

        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static PluginManifest buildRuntimeManifest() {
        PluginManifest manifest = new PluginManifest();
        manifest.setGroup(PACK_GROUP);
        manifest.setName(PACK_NAME);
        manifest.setVersion(Semver.fromString(PACK_VERSION));
        manifest.setDescription("HyGuard runtime asset pack");
        manifest.setWebsite("https://github.com/maksimovc/HyGuard");
        manifest.setServerVersion(TARGET_SERVER_VERSION);

        AuthorInfo author = new AuthorInfo();
        author.setName("maksimovc");
        manifest.setAuthors(List.of(author));
        return manifest;
    }
}