package com.azuredoom.classescore.data;

public enum PassiveType {

    ATTRIBUTE_MULTIPLIER,
    ATTRIBUTE_ADDITIVE,
    DAMAGE_MULTIPLIER;

    public static PassiveType fromJson(String value) {
        return PassiveType.valueOf(value.trim().toUpperCase());
    }
}
