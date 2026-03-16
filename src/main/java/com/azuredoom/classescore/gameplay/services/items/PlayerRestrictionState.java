package com.azuredoom.classescore.gameplay.services.items;

import java.util.Set;

public record PlayerRestrictionState(
    String classId,
    Set<String> allowedWeapons,
    Set<String> allowedArmor
) {}
