package dev.thenexusgates.hyguard.i18n;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.config.HyGuardConfig;
import dev.thenexusgates.hyguard.util.TextFormatter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HyGuardText {

    private final HyGuardConfig.Chat chatConfig;

    public HyGuardText(HyGuardConfig.Chat chatConfig) {
        this.chatConfig = chatConfig;
    }

    public String text(PlayerRef playerRef, String key, Map<String, String> replacements) {
        String template = resolve(playerRef, key);
        return TextFormatter.format(template, replacements);
    }

    public Message chat(PlayerRef playerRef, String key, Map<String, String> replacements, boolean includePrefix) {
        return rawChat(text(playerRef, key, replacements), includePrefix);
    }

    public Message rawChat(String rawText, boolean includePrefix) {
        String resolved = includePrefix ? chatConfig.prefix + rawText : rawText;
        return toMessage(resolved);
    }

    private String resolve(PlayerRef playerRef, String key) {
        I18nModule i18n = I18nModule.get();
        if (i18n == null || key == null || key.isBlank()) {
            return key == null ? "" : key;
        }

        String language = playerRef == null ? null : playerRef.getLanguage();
        List<String> candidates = candidateKeys(key);
        if (language != null && !language.isBlank()) {
            for (String candidate : candidates) {
                String translated = i18n.getMessage(language, candidate);
                if (isResolved(candidate, translated)) {
                    return translated;
                }
            }
        }

        for (String candidate : candidates) {
            String translated = i18n.getMessage("en-US", candidate);
            if (isResolved(candidate, translated)) {
                return translated;
            }
        }

        return key;
    }

    private static boolean isResolved(String candidate, String translated) {
        return translated != null && !translated.isBlank() && !translated.equals(candidate);
    }

    private static List<String> candidateKeys(String key) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(key);
        if (key.startsWith("hyguard.")) {
            keys.add(key.substring("hyguard.".length()));
        }
        return new ArrayList<>(keys);
    }

    private static Message toMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }

        List<Message> parts = new ArrayList<>();
        String currentColor = null;
        boolean bold = false;
        boolean italic = false;
        int index = 0;
        StringBuilder current = new StringBuilder();

        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == '\u00a7' && index + 1 < text.length()) {
                if (current.length() > 0) {
                    parts.add(styleSegment(current.toString(), currentColor, bold, italic));
                    current = new StringBuilder();
                }

                char code = Character.toLowerCase(text.charAt(index + 1));
                String nextColor = sectionColor(code);
                if (nextColor != null) {
                    currentColor = nextColor;
                    bold = false;
                    italic = false;
                } else if (code == 'l') {
                    bold = true;
                } else if (code == 'o') {
                    italic = true;
                } else if (code == 'r') {
                    currentColor = null;
                    bold = false;
                    italic = false;
                }
                index += 2;
                continue;
            }

            current.append(c);
            index++;
        }

        if (current.length() > 0) {
            parts.add(styleSegment(current.toString(), currentColor, bold, italic));
        }

        if (parts.isEmpty()) {
            return Message.raw(text);
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return Message.empty().insertAll(parts);
    }

    private static Message styleSegment(String text, String color, boolean bold, boolean italic) {
        Message segment = Message.raw(text);
        if (color != null) {
            segment = segment.color(color);
        }
        if (bold) {
            segment = segment.bold(true);
        }
        if (italic) {
            segment = segment.italic(true);
        }
        return segment;
    }

    private static String sectionColor(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default -> null;
        };
    }
}
