package com.azuredoom.classescore.data;

/**
 * Represents the type of passive effect that can be applied to a class or entity. This enum includes different types of
 * passive modifications such as altering attributes or damage.
 */
public enum PassiveType {

    ATTRIBUTE_MULTIPLIER,
    ATTRIBUTE_ADDITIVE,
    DAMAGE_MULTIPLIER;

    /**
     * Parses a JSON string to derive a PassiveType enum value.
     *
     * @param value the JSON string representing the type of passive effect, which will be normalized by trimming
     *              whitespace and converting to uppercase.
     * @return the corresponding PassiveType enum value.
     * @throws IllegalArgumentException if the input value does not match any PassiveType name.
     */
    public static PassiveType fromJson(String value) {
        return PassiveType.valueOf(value.trim().toUpperCase());
    }
}
