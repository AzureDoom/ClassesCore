package com.azuredoom.classescore.util;

/**
 * Represents different types of player statistics in a game or application. Each stat type is associated with a
 * specific identifier string (jsonId), allowing for serialization, deserialization, and stat identification.
 */
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

    /**
     * Retrieves the JSON identifier string associated with this enum constant.
     *
     * @return the JSON identifier string that represents this {@link StatType}.
     */
    public String jsonId() {
        return jsonId;
    }

    /**
     * Converts a JSON identifier string to its corresponding {@link StatType} enum constant.
     * <p>
     * This method iterates through all the enum constants of {@link StatType} and performs a case-insensitive
     * comparison to match the provided identifier string with the associated `jsonId` of each enum constant. If no
     * match is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param id the JSON identifier string to be converted into a {@link StatType} constant; must not be null or empty.
     * @return the {@link StatType} constant corresponding to the provided JSON identifier string.
     * @throws IllegalArgumentException if the provided JSON identifier string does not match any {@link StatType}.
     */
    public static StatType fromJson(String id) {
        for (var type : values()) {
            if (type.jsonId.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stat id: " + id);
    }
}
