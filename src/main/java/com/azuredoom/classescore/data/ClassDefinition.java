package com.azuredoom.classescore.data;

import com.azuredoom.hytalecustomassetloader.model.AssetSource;

import java.util.Collections;
import java.util.List;

/**
 * Represents the definition of a class, including its unique identifier, display information, statistics, passive
 * effects, and equipment rules. This class is immutable and ensures consistent handling of null collections by
 * substituting them with empty versions.
 *
 * @param id             The unique identifier of the class.
 * @param displayName    The human-readable name of the class.
 * @param description    A textual description of the class, providing additional details.
 * @param stats          A list of {@link StatDefinition} objects defining the base statistics and growth rates for the
 *                       class.
 * @param passives       A list of {@link PassiveDefinition} objects representing passive abilities or effects
 *                       associated with the class.
 * @param equipmentRules The {@link EquipmentRules} instance that defines the allowed weapons and armor for the class.
 * @param source         The source the class was loaded from
 */
public record ClassDefinition(
    String id,
    String displayName,
    String description,
    List<StatDefinition> stats,
    List<PassiveDefinition> passives,
    EquipmentRules equipmentRules,
    AssetSource source
) {

    public ClassDefinition(
        String id,
        String displayName,
        String description,
        List<StatDefinition> stats,
        List<PassiveDefinition> passives,
        EquipmentRules equipmentRules,
        AssetSource source
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.stats = stats == null ? Collections.emptyList() : List.copyOf(stats);
        this.passives = passives == null ? Collections.emptyList() : List.copyOf(passives);
        this.equipmentRules = equipmentRules == null
            ? new EquipmentRules(Collections.emptySet(), Collections.emptySet())
            : equipmentRules;
        this.source = source;
    }
}
