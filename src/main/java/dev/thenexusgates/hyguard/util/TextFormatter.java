package dev.thenexusgates.hyguard.util;

import java.util.Map;

public final class TextFormatter {

    private TextFormatter() {
    }

    public static String format(String template, Map<String, String> replacements) {
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}