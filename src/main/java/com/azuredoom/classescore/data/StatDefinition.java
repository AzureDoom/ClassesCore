package com.azuredoom.classescore.data;

/**
 * Represents a definition of a statistic for a class or entity. This class defines the base value of the statistic as
 * well as its growth per level.
 *
 * @param id       The unique identifier for the statistic.
 * @param base     The base value of the statistic.
 * @param perLevel The amount by which the statistic increases per level.
 */
public record StatDefinition(
    String id,
    int base,
    int perLevel
) {}
