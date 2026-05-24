package com.bst.server.common.utils;

import org.springframework.stereotype.Component;

@Component
public class StringOperation {
    /**
     * Trims whitespace and converts to uppercase.
     */
    public String normalizeNameInUppercase(String name) {
        return name.trim().toUpperCase();
    }

    /* Trims whitespace and check null. */
    public String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /**
     * Normalizes a name by trimming and collapsing internal whitespace,
     * then capitalizes the first letter of each word (Title Case).
     */
    public String toTitleCase(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        String[] words = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}

