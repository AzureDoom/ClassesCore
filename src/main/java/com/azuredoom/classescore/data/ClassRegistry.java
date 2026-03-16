package com.azuredoom.classescore.data;

import java.util.*;

public final class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new LinkedHashMap<>();

    public void register(ClassDefinition definition) {
        classes.put(definition.id(), definition);
    }

    public Optional<ClassDefinition> get(String id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Collection<ClassDefinition> all() {
        return classes.values();
    }

    public boolean isEmpty() {
        return classes.isEmpty();
    }
}
