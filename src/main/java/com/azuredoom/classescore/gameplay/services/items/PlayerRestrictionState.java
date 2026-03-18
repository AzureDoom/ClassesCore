package com.azuredoom.classescore.gameplay.services.items;

import com.azuredoom.classescore.data.EquipmentRules;

/**
 * Represents the state of restrictions applied to a player based on their class. This record is used to manage the
 * allowable weapons and armor items that a player can use, according to their designated class.
 *
 * @param classId        The unique identifier of the player's class.
 * @param equipmentRules The equipment rules associated with the player's class.
 */
public record PlayerRestrictionState(
    String classId,
    EquipmentRules equipmentRules
) {}
