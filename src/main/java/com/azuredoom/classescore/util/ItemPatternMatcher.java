package com.azuredoom.classescore.util;

import java.util.Set;
import java.util.regex.Pattern;

public final class ItemPatternMatcher {

    private ItemPatternMatcher() {}

    public static boolean matches(Set<String> allowedPatterns, String itemId) {
        if (allowedPatterns == null || allowedPatterns.isEmpty()) {
            return true;
        }

        if (itemId == null || itemId.isBlank()) {
            return false;
        }

        for (String pattern : allowedPatterns) {
            if (matchesPattern(pattern, itemId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesPattern(String pattern, String itemId) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }

        if (!pattern.contains("*")) {
            return pattern.equals(itemId);
        }

        String regex = toRegex(pattern);
        return Pattern.matches(regex, itemId);
    }

    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");

        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '.' -> regex.append("\\.");
                case '(' -> regex.append("\\(");
                case ')' -> regex.append("\\)");
                case '[' -> regex.append("\\[");
                case ']' -> regex.append("\\]");
                case '{' -> regex.append("\\{");
                case '}' -> regex.append("\\}");
                case '+' -> regex.append("\\+");
                case '?' -> regex.append("\\?");
                case '^' -> regex.append("\\^");
                case '$' -> regex.append("\\$");
                case '|' -> regex.append("\\|");
                case '\\' -> regex.append("\\\\");
                default -> regex.append(c);
            }
        }

        regex.append("$");
        return regex.toString();
    }
}
