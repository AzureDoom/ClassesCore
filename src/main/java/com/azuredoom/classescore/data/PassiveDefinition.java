package com.azuredoom.classescore.data;

/**
 * Represents a passive effect definition that can be applied to a class or entity. This class defines the unique
 * characteristics of a passive effect, including its type, the affected attribute, the associated damage type, and the
 * size of its effect.
 *
 * @param id         The unique identifier for the passive effect.
 * @param type       The type of passive effect, represented as a {@link PassiveType}, which defines how the effect
 *                   modifies attributes or damage.
 * @param attribute  The name of the attribute that this passive effect influences. This can be used to target specific
 *                   characteristics of a class or entity, such as "strength" or "speed".
 * @param damageType The type of damage this passive effect applies to, such as "physical" or "magical". This is used to
 *                   scope the effect to certain damage categories.
 * @param value      The numeric value of the effect, which determines the size of its influence on the defined
 *                   attribute or damage type.
 */
public record PassiveDefinition(
    String id,
    PassiveType type,
    String attribute,
    String damageType,
    float value
) {

}
