package dev.thenexusgates.hyguard.storage;

public final class MigrationManager {

    public static final int CURRENT_SCHEMA = 1;

    public boolean supports(int schemaVersion) {
        return schemaVersion == CURRENT_SCHEMA;
    }
}