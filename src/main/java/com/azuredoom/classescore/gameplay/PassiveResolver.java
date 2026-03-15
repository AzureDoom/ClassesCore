package com.azuredoom.classescore.gameplay;

import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.PassiveDefinition;

public final class PassiveResolver {

    public ResolvedClassBonuses resolve(ClassDefinition definition) {
        var bonuses = new ResolvedClassBonuses();

        for (var passive : definition.getPassives()) {
            applyPassive(bonuses, passive);
        }

        return bonuses;
    }

    private void applyPassive(ResolvedClassBonuses bonuses, PassiveDefinition passive) {
        switch (passive.getType()) {
            case ATTRIBUTE_MULTIPLIER -> bonuses.getAttributeMultipliers()
                .merge(passive.getAttribute(), passive.getValue(), Double::sum);

            case ATTRIBUTE_FLAT -> bonuses.getAttributeFlats()
                .merge(passive.getAttribute(), passive.getValue(), Double::sum);

            case DAMAGE_MULTIPLIER -> bonuses.getDamageMultipliers()
                .merge(passive.getDamageType(), passive.getValue(), Double::sum);
        }
    }
}
