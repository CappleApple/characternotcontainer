package com.cappleapple.characternotcontainer.client;

import java.util.Locale;
import java.util.function.Function;

public final class AttributeDisplayName {
    private AttributeDisplayName() {}

    public static String resolve(String override, String translationKey, String attributeId,
                                 Function<String, String> localizer) {
        if (override != null && !override.isBlank()) return override;
        if (translationKey != null && !translationKey.isBlank()) {
            String localized = localizer.apply(translationKey);
            if (localized != null && !localized.isBlank() && !localized.equals(translationKey)) return localized;
        }
        return fallback(attributeId == null || attributeId.isBlank() ? translationKey : attributeId);
    }

    public static String fallback(String identifier) {
        if (identifier == null || identifier.isBlank()) return "Attribute";
        String value = identifier.strip();
        int namespace = value.indexOf(':');
        if (namespace >= 0 && namespace + 1 < value.length()) value = value.substring(namespace + 1);
        int category = Math.max(value.lastIndexOf('.'), value.lastIndexOf('/'));
        if (category >= 0 && category + 1 < value.length()) value = value.substring(category + 1);
        value = value.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("[_\\-.]+", " ")
                .strip();
        if (value.isEmpty()) return "Attribute";

        StringBuilder result = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            String lower = word.toLowerCase(Locale.ROOT);
            result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return result.isEmpty() ? "Attribute" : result.toString();
    }
}
