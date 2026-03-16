package com.azuredoom.classescore.data;

import java.util.Collections;
import java.util.List;

public record ClassDefinition(
    String id,
    String displayName,
    String description,
    List<PassiveDefinition> passives,
    EquipmentRules equipmentRules
) {

    public ClassDefinition(
        String id,
        String displayName,
        String description,
        List<PassiveDefinition> passives,
        EquipmentRules equipmentRules
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.passives = passives == null ? Collections.emptyList() : List.copyOf(passives);
        this.equipmentRules = equipmentRules == null
            ? new EquipmentRules(Collections.emptySet(), Collections.emptySet())
            : equipmentRules;
    }
}
