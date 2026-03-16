package com.azuredoom.classescore.gameplay.services.items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.azuredoom.classescore.data.ClassDefinition;

public class PlayerRestrictionCache {

    private final Map<UUID, PlayerRestrictionState> states = new ConcurrentHashMap<>();

    public void setClass(UUID playerId, ClassDefinition classDef) {
        Set<String> allowedWeapons = classDef.equipmentRules().allowedWeapons();
        if (allowedWeapons == null) {
            allowedWeapons = Collections.emptySet();
        }
        Set<String> allowedArmor = classDef.equipmentRules().allowedArmor();
        if (allowedArmor == null) {
            allowedArmor = Collections.emptySet();
        }

        states.put(
            playerId,
            new PlayerRestrictionState(classDef.id(), Set.copyOf(allowedWeapons), Set.copyOf(allowedArmor))
        );
    }

    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    public boolean canUseWeapon(UUID playerId, String itemId) {
        var state = states.get(playerId);
        if (state == null) {
            return true;
        }

        var allowedWeapons = state.allowedWeapons();
        return allowedWeapons.isEmpty() || allowedWeapons.contains(itemId);
    }

    public boolean canUseArmor(UUID playerId, String itemId) {
        var state = states.get(playerId);
        if (state == null) {
            return true;
        }

        var allowedArmor = state.allowedArmor();
        return allowedArmor.isEmpty() || allowedArmor.contains(itemId);
    }

    public Optional<String> getClassId(UUID playerId) {
        var state = states.get(playerId);
        return state == null ? Optional.empty() : Optional.of(state.classId());
    }

    public Optional<PlayerRestrictionState> getState(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }
}
