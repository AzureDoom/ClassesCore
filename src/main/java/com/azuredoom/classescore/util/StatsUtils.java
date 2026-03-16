package com.azuredoom.classescore.util;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class StatsUtils {

    private StatsUtils() {}

    private static @NullableDecl EntityStatMap getStatMap(@NonNullDecl Store<EntityStore> store, Player player) {
        if (player == null || player.getReference() == null) {
            return null;
        }
        return store.getComponent(player.getReference(), EntityStatMap.getComponentType());
    }

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
