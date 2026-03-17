package com.azuredoom.classescore.api.model;

import java.util.UUID;

/**
 * Represents the state of a player's selected class within a game.
 *
 * @param playerId  The unique identifier of the player.
 * @param classId   The unique identifier of the selected class.
 * @param createdAt The timestamp when the class selection was made.
 * @param updatedAt The timestamp when the class selection was last updated.
 */
public record PlayerClassState(
    UUID playerId,
    String classId,
    long createdAt,
    long updatedAt
) {}
