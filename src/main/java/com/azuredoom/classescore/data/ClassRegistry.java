package com.azuredoom.classescore.data;

import java.util.*;

/**
 * A registry for managing {@link ClassDefinition} instances. This class provides functionality to register, retrieve,
 * and manage class definitions.
 */
public final class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new LinkedHashMap<>();

    /**
     * Registers a {@link ClassDefinition} instance in the registry. The class definition is associated with its unique
     * identifier, obtained via the {@code id()} method of the {@link ClassDefinition} instance. If a class definition
     * with the same identifier already exists, it will be overwritten.
     *
     * @param definition the {@link ClassDefinition} instance to register. Must not be null.
     */
    public void register(ClassDefinition definition) {
        classes.put(definition.id(), definition);
    }

    /**
     * Retrieves an {@link Optional} containing the {@link ClassDefinition} instance associated with the specified
     * unique identifier. If no class definition is found for the given identifier, an empty {@link Optional} is
     * returned.
     *
     * @param id the unique identifier of the {@link ClassDefinition} to retrieve.
     * @return an {@link Optional} containing the {@link ClassDefinition} if found, or an empty {@link Optional} if no
     *         class definition is associated with the given identifier.
     */
    public Optional<ClassDefinition> get(String id) {
        return Optional.ofNullable(classes.get(id));
    }

    /**
     * Retrieves a collection of all registered {@link ClassDefinition} instances in the registry.
     *
     * @return a collection containing all {@link ClassDefinition} objects currently registered.
     */
    public Collection<ClassDefinition> all() {
        return classes.values();
    }

    /**
     * Checks whether the registry of {@link ClassDefinition} instances is empty.
     *
     * @return true if no class definitions are present in the registry; false otherwise.
     */
    public boolean isEmpty() {
        return classes.isEmpty();
    }
}
