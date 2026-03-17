package com.azuredoom.classescore.util;

public enum StatType {

    STRENGTH("strength"),
    AGILITY("agility"),
    PERCEPTION("perception"),
    VITALITY("vitality"),
    INTELLIGENCE("intelligence"),
    CONSTITUTION("constitution");

    private final String jsonId;

    StatType(String jsonId) {
        this.jsonId = jsonId;
    }

    public String jsonId() {
        return jsonId;
    }

    public static StatType fromJson(String id) {
        for (var type : values()) {
            if (type.jsonId.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stat id: " + id);
    }
}
