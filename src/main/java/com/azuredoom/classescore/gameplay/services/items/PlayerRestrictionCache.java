package com.azuredoom.classescore.gameplay.services.items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.azuredoom.classescore.data.ClassDefinition;

/**
 * A cache that manages player-specific class restrictions for using weapons and armor. The restrictions are stored and
 * retrieved based on unique player identifiers (UUIDs).
 */
public class PlayerRestrictionCache {

    private final Map<UUID, PlayerRestrictionState> states = new ConcurrentHashMap<>();

    /**
     * Assigns a class to a specific player and establishes equipment restrictions based on the class definition. The
     * restrictions include allowed weapons and armor specific to the player's class.
     *
     * @param playerId the unique identifier of the player to whom the class and restrictions are assigned
     * @param classDef the class definition containing the class ID and equipment restriction rules
     */
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

    /**
     * Removes the restriction state associated with the specified player. This effectively clears any class
     * restrictions (e.g., allowed weapons or armor) for the player identified by the provided UUID.
     *
     * @param playerId the unique identifier of the player whose restriction state is to be removed
     */
    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    /**
     * Determines whether a player can use a specific weapon based on their assigned class restrictions. If the player
     * has no restrictions or the weapon is allowed within their restrictions, the method returns true. Otherwise, it
     * returns false.
     *
     * @param playerId the unique identifier of the player
     * @param itemId   the identifier of the weapon item to check
     * @return {@code true} if the player is allowed to use the specified weapon; {@code false} otherwise
     */
    public boolean canUseWeapon(UUID playerId, String itemId) {
        var state = states.get(playerId);
        if (state == null) {
            return true;
        }

        var allowedWeapons = state.allowedWeapons();
        return allowedWeapons.isEmpty() || allowedWeapons.contains(itemId);
    }

    /**
     * Determines whether a player is allowed to use a specific armor item based on their assigned class restrictions.
     * If the player has no restrictions or the armor is allowed within their restrictions, the method returns true.
     * Otherwise, it returns false.
     *
     * @param playerId the unique identifier of the player whose armor usage is being checked
     * @param itemId   the identifier of the armor item to check
     * @return {@code true} if the player is allowed to use the specified armor; {@code false} otherwise
     */
    public boolean canUseArmor(UUID playerId, String itemId) {
        var state = states.get(playerId);
        if (state == null) {
            return true;
        }

        var allowedArmor = state.allowedArmor();
        return allowedArmor.isEmpty() || allowedArmor.contains(itemId);
    }

    /**
     * Retrieves the class ID associated with the specified player's restriction state. If the player does not have an
     * assigned state, returns an empty {@code Optional}.
     *
     * @param playerId the unique identifier of the player whose class ID is to be retrieved
     * @return an {@code Optional} containing the class ID if the player has a restriction state; otherwise, an empty
     *         {@code Optional}
     */
    public Optional<String> getClassId(UUID playerId) {
        var state = states.get(playerId);
        return state == null ? Optional.empty() : Optional.of(state.classId());
    }

    /**
     * Retrieves the restriction state associated with the specified player. The restriction state contains details such
     * as the player's class ID, permitted weapons, and allowed armor sets.
     *
     * @param playerId the unique identifier of the player whose restriction state is to be retrieved
     * @return an {@code Optional} containing the {@code PlayerRestrictionState} if the player has an associated state;
     *         otherwise, an empty {@code Optional}
     */
    public Optional<PlayerRestrictionState> getState(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }
}
