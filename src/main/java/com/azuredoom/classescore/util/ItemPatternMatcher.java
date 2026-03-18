package com.azuredoom.classescore.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A utility class for matching item IDs against a set of allowed patterns. This class provides methods to determine if
 * a specific item ID matches any pattern in a collection of patterns. Patterns may include the wildcard '*' to
 * represent any sequence of characters.
 */
public final class ItemPatternMatcher {

    private ItemPatternMatcher() {}

    /**
     * Checks if the given item ID matches any pattern in the provided set of allowed patterns.
     *
     * @param allowedPatterns A set of patterns to match against. Patterns may include the wildcard '*' to represent any
     *                        sequence of characters. If the set is null or empty, the method will always return
     *                        {@code true}.
     * @param itemId          The item ID to test against the patterns. If the item ID is null or blank, the method will
     *                        return {@code false}.
     * @return {@code true} if the item ID matches any pattern in the set, otherwise {@code false}.
     */
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

    /**
     * Checks if the given itemId matches the provided pattern.
     *
     * @param pattern The pattern to match against. It may include the wildcard '*' to represent any sequence of
     *                characters. If the pattern is null or blank, the method returns false.
     * @param itemId  The item ID to test against the pattern. A null or blank itemId will not be checked, and the
     *                behavior depends solely on the provided pattern logic.
     * @return {@code true} if the itemId matches the pattern, otherwise {@code false}. If the pattern does not contain
     *         a wildcard, the method performs a direct equality check.
     */
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

    /**
     * Converts a given glob pattern into a regular expression pattern.
     *
     * @param glob The glob pattern that needs to be converted into a regular expression. The glob pattern may include
     *             wildcards such as '*' to represent any sequence of characters.
     * @return A string representation of the regular expression that is equivalent to the provided glob pattern.
     */
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
