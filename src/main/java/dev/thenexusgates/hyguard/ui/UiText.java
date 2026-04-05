package dev.thenexusgates.hyguard.ui;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Locale;

final class UiText {

    private UiText() {
    }

    static String choose(PlayerRef playerRef, String english, String ukrainian) {
        return isUkrainian(playerRef) ? ukrainian : english;
    }

    static String format(PlayerRef playerRef, String english, String ukrainian, Object... args) {
        return String.format(Locale.ROOT, choose(playerRef, english, ukrainian), args);
    }

    private static boolean isUkrainian(PlayerRef playerRef) {
        String language = playerRef == null ? null : playerRef.getLanguage();
        if (language == null || language.isBlank()) {
            return false;
        }
        return language.toLowerCase(Locale.ROOT).startsWith("uk");
    }
}