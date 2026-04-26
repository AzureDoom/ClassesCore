package com.azuredoom.classescore.util;

import com.azuredoom.tagcore.api.TagService;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A utility class for matching item IDs against a set of allowed patterns. This class provides methods to determine if
 * a specific item ID matches any pattern in a collection of patterns.
 * <p>
 * Supported pattern formats include:
 * </p>
 * <ul>
 * <li>Exact item identifiers (e.g., {@code Weapon_Sword_Wood})</li>
 * <li>Glob patterns using {@code *} as a wildcard (e.g., {@code Weapon_*})</li>
 * <li>TagCore tag references prefixed with {@code #} (e.g., {@code #tagcore:starter_weapons})</li>
 * </ul>
 * <p>
 * If a tag reference is provided, the TagCore {@link TagService} will be used to determine whether the item is a member
 * of the specified tag.
 * </p>
 */
public final class ItemPatternMatcher {

    private ItemPatternMatcher() {}

    /**
     * Checks if the given item ID matches any pattern in the provided set of allowed patterns.
     *
     * @param allowedPatterns A set of patterns to match against. Patterns may include exact identifiers, glob patterns
     *                        using {@code *}, or TagCore tag references prefixed with {@code #}. If the set is null or
     *                        empty, the method will always return {@code true}.
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

        for (var pattern : allowedPatterns) {
            if (matchesPattern(pattern, itemId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given item ID matches the provided pattern.
     *
     * @param pattern The pattern to match against. It may be an exact identifier, a glob pattern containing {@code *},
     *                or a TagCore tag reference prefixed with {@code #}. If the pattern is null or blank, the method
     *                returns {@code false}.
     * @param itemId  The item ID to test against the pattern.
     * @return {@code true} if the item ID matches the pattern, otherwise {@code false}. If the pattern does not contain
     *         a wildcard or tag reference, the method performs a direct equality check.
     */
    private static boolean matchesPattern(String pattern, String itemId) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }

        var trimmed = pattern.trim();

        if (isTagReference(trimmed)) {
            return matchesTag(trimmed, itemId);
        }

        if (!trimmed.contains("*")) {
            return trimmed.equals(itemId);
        }

        return Pattern.matches(toRegex(trimmed), itemId);
    }

    /**
     * Determines whether the provided value represents a TagCore tag reference.
     *
     * @param value The pattern value to evaluate.
     * @return {@code true} if the value is a tag reference (starts with {@code #} and has additional content),
     *         otherwise {@code false}.
     */
    private static boolean isTagReference(String value) {
        return value.startsWith("#") && value.length() > 1;
    }

    /**
     * Checks whether the given item ID is a member of the specified TagCore tag.
     *
     * @param tagReference The tag reference string, including the {@code #} prefix.
     * @param itemId       The item ID to check for membership.
     * @return {@code true} if the item exists in the specified tag, otherwise {@code false}. Returns {@code false} if
     *         the TagService is unavailable or the tag does not exist.
     */
    private static boolean matchesTag(String tagReference, String itemId) {
        var tagId = normalizeTagId(tagReference.substring(1));

        var tagService = TagService.getTagService().orElse(null);
        if (tagService == null) {
            return false;
        }

        if (!tagService.hasTag(tagId)) {
            return false;
        }

        var result = tagService.isInItemTag(tagId, itemId);
        return result.isSuccess() && Boolean.TRUE.equals(result.value());
    }

    /**
     * Normalizes a tag identifier into a fully-qualified TagCore tag ID.
     * <p>
     * If the provided tag ID does not include a namespace, the default {@code hytale} namespace will be applied.
     * </p>
     *
     * @param rawTagId The raw tag identifier without the {@code #} prefix.
     * @return A normalized tag ID including a namespace.
     */
    private static String normalizeTagId(String rawTagId) {
        var trimmed = rawTagId.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        return trimmed.contains(":") ? trimmed : "hytale:" + trimmed;
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
