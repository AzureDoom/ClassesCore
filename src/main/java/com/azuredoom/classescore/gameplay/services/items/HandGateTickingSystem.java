package com.azuredoom.classescore.gameplay.services.items;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

import com.azuredoom.classescore.ClassesCore;

public class HandGateTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final String WEAPON_PREFIX = "Weapon_";

    private final Map<UUID, HandCheckState> lastChecks;

    private final PlayerRestrictionCache restrictionCache;

    public HandGateTickingSystem(
        Map<UUID, HandCheckState> lastChecks,
        PlayerRestrictionCache restrictionCache
    ) {
        this.lastChecks = lastChecks;
        this.restrictionCache = restrictionCache;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @NotNull ArchetypeChunk<EntityStore> chunk,
        @NotNull Store<EntityStore> store,
        @NotNull CommandBuffer<EntityStore> cb
    ) {
        if (!ClassesCore.getConfig().get().isEnableClassItemRestrictions()) {
            return;
        }
        final var holder = store.copyEntity(chunk.getReferenceTo(index));
        var player = holder.getComponent(Player.getComponentType());
        if (player == null)
            return;
        var playerRef = player.getReference();
        if (playerRef == null) {
            return;
        }
        var playerRefComponent = playerRef.getStore()
                .getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }

        var playerId = playerRefComponent.getUuid();
        var hand = InventoryComponent.getItemInHand(cb, playerRef);
        if (hand == null || ItemStack.isEmpty(hand)) {
            lastChecks.remove(playerId);
            return;
        }

        var itemId = hand.getItemId();
        if (itemId.isEmpty()) {
            lastChecks.remove(playerId);
            return;
        }

        var startsWithPrefix = itemId.startsWith(WEAPON_PREFIX);
        var canUse = restrictionCache.canUseWeapon(playerId, itemId);
        var blocked = startsWithPrefix && !canUse;

        lastChecks.put(playerId, new HandCheckState(hand, itemId, blocked));
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            Player.getComponentType(),
            PlayerRef.getComponentType()
        );
    }
}
