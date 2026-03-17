package com.azuredoom.classescore.gameplay.services.items;

import java.util.Set;

/**
 * Represents the state of restrictions applied to a player based on their class. This record is used to manage the
 * allowable weapons and armor items that a player can use, according to their designated class.
 *
 * @param classId        The unique identifier of the player's class.
 * @param allowedWeapons A set of item IDs representing the weapons allowed for the player's class.
 * @param allowedArmor   A set of item IDs representing the armor allowed for the player's class.
 */
public record PlayerRestrictionState(
    String classId,
    Set<String> allowedWeapons,
    Set<String> allowedArmor
) {}
