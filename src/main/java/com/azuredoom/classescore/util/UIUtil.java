package com.azuredoom.classescore.util;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;

/**
 * A utility class providing helper methods for UI-related tasks, including string handling and sorting of class-related
 * data structures. This class is not meant to be instantiated.
 */
public final class UIUtil {

    private UIUtil() {}

    public static final int MAX_ROWS = 8;

    /**
     * Retrieves a sorted list of {@link ClassDefinition} objects. The sorting is performed based on the display name of
     * each class, ignoring the case. Any null entries in the list of classes are filtered out.
     *
     * @return a sorted {@link List} containing non-null {@link ClassDefinition} objects, ordered by their lowercased
     *         display names.
     */
    public static List<ClassDefinition> getSortedClasses() {
        return ClassesCore.getClassRegistryIfPresent()
            .map(ClassRegistry::all)
            .orElse(List.of())
            .stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(def -> safe(def.displayName()).toLowerCase()))
            .toList();
    }

    /**
     * Ensures that the provided string is not null by returning an empty string if the input is null, or the original
     * string otherwise.
     *
     * @param value the string to be checked; can be null.
     * @return an empty string if the input is null, or the original string if it is non-null.
     */
    public static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
