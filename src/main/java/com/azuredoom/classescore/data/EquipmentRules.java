package com.azuredoom.classescore.data;

import java.util.Collections;
import java.util.Set;

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

    public boolean isWeaponAllowed(String weaponId) {
        if (allowedWeapons.isEmpty()) {
            return true;
        }
        return allowedWeapons.contains(weaponId);
    }

    public boolean isArmorAllowed(String armorId) {
        if (allowedArmor.isEmpty()) {
            return true;
        }
        return allowedArmor.contains(armorId);
    }
}
