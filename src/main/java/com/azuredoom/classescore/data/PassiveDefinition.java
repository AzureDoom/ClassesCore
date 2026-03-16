package com.azuredoom.classescore.data;

public record PassiveDefinition(
    String id,
    PassiveType type,
    String attribute,
    String damageType,
    float value
) {

}
