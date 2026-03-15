package com.azuredoom.classescore.gameplay;

import java.util.HashMap;
import java.util.Map;

public final class ResolvedClassBonuses {

    private final Map<String, Double> attributeMultipliers = new HashMap<>();

    private final Map<String, Double> attributeFlats = new HashMap<>();

    private final Map<String, Double> damageMultipliers = new HashMap<>();

    public Map<String, Double> getAttributeMultipliers() {
        return attributeMultipliers;
    }

    public Map<String, Double> getAttributeFlats() {
        return attributeFlats;
    }

    public Map<String, Double> getDamageMultipliers() {
        return damageMultipliers;
    }
}
