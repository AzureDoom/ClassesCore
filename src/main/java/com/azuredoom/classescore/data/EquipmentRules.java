package com.azuredoom.classescore.data;

import java.util.Collections;
import java.util.Set;

/**
 * Defines rules for allowed weapons and armor for a specific class.
 *
 * @param allowedWeapons A set of weapon identifiers that are allowed for use. If the set is empty, all weapons are
 *                       considered allowed.
 * @param allowedArmor   A set of armor identifiers that are allowed for use. If the set is empty, all armor is
 *                       considered allowed.
 */
public record EquipmentRules(
    Set<String> allowedWeapons,
    Set<String> allowedArmor
) {

    public EquipmentRules(Set<String> allowedWeapons, Set<String> allowedArmor) {
        this.allowedWeapons = allowedWeapons == null
            ? Collections.emptySet()
            : Set.copyOf(allowedWeapons);
        this.allowedArmor = allowedArmor == null
            ? Collections.emptySet()
            : Set.copyOf(allowedArmor);
    }

    /**
     * Determines whether a weapon with the specified identifier is allowed based on the configured rules. If the set of
     * allowed weapons is empty, all weapons are considered allowed.
     *
     * @param weaponId The unique identifier of the weapon to check.
     * @return true if the weapon is allowed, or if the set of allowed weapons is empty; false otherwise.
     */
    public boolean isWeaponAllowed(String weaponId) {
        if (allowedWeapons.isEmpty()) {
            return true;
        }
        return allowedWeapons.contains(weaponId);
    }

    /**
     * Checks whether specific armor, identified by its unique identifier, is allowed based on the configured equipment
     * rules. If the set of allowed armor is empty, all armor is considered allowed.
     *
     * @param armorId The unique identifier of the armor to be checked.
     * @return true if the armor is allowed, or if the set of allowed armor is empty; false otherwise.
     */
    public boolean isArmorAllowed(String armorId) {
        if (allowedArmor.isEmpty()) {
            return true;
        }
        return allowedArmor.contains(armorId);
    }
}
