package com.azuredoom.classescore.util;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Utility class for managing and modifying player statistics. Provides methods to apply and remove static modifiers on
 * player stats. This class is not meant to be instantiated.
 */
public final class StatsUtils {

    private StatsUtils() {}

    /**
     * Retrieves the {@link EntityStatMap} associated with the given player. This method fetches the stat component of
     * the player's reference from the provided store. If the player is null or their reference is null, the method
     * returns null.
     *
     * @param store  The store providing access to entity components, expected to include the player's stat map.
     * @param player The player whose {@link EntityStatMap} is to be retrieved. Must not be null.
     * @return The {@link EntityStatMap} of the player's reference if available, or null if the player or their
     *         reference is null.
     */
    private static @NullableDecl EntityStatMap getStatMap(@NonNullDecl Store<EntityStore> store, Player player) {
        if (player == null || player.getReference() == null) {
            return null;
        }
        return store.getComponent(player.getReference(), EntityStatMap.getComponentType());
    }

    /**
     * Applies a static modifier to a specific stat in the player's stat map.
     *
     * @param store           The store that provides access to the player's entity components.
     * @param player          The player whose stat is to be modified.
     * @param index           The index representing the specific stat to modify.
     * @param calculationType The method of calculation for the modifier (e.g., add, multiply).
     * @param value           The value of the modifier to apply.
     * @param modifierKey     The unique key identifying the modifier for this stat.
     */
    public static void doStatChange(
        Store<EntityStore> store,
        Player player,
        int index,
        StaticModifier.CalculationType calculationType,
        float value,
        String modifierKey
    ) {
        var playerStatMap = StatsUtils.getStatMap(store, player);
        if (playerStatMap == null)
            return;
        var modifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            calculationType,
            value
        );
        playerStatMap.putModifier(index, modifierKey, modifier);
    }

    /**
     * Removes a specific stat modifier from the player's stat map, resets the stat's value, and recalculates the
     * maximized stat value.
     *
     * @param store       The store that provides access to the player's entity components.
     * @param player      The player whose stat modifier is to be removed.
     * @param index       The index representing the specific stat to modify.
     * @param modifierKey The key identifying the specific modifier to remove.
     */
    public static void removeStatModifier(
        Store<EntityStore> store,
        Player player,
        int index,
        String modifierKey
    ) {
        var playerStatMap = StatsUtils.getStatMap(store, player);
        if (playerStatMap == null)
            return;
        playerStatMap.removeModifier(index, modifierKey);
        playerStatMap.resetStatValue(index);
        playerStatMap.maximizeStatValue(index);
    }
}
