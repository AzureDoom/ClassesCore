package com.azuredoom.classescore.gameplay.services.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import com.azuredoom.classescore.util.StatApplier;

public class StatsTickingSystem extends EntityTickingSystem<EntityStore> {

    public StatsTickingSystem() {}

    @Override
    public void tick(
        float var1,
        int index,
        @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        final var holder = store.copyEntity(archetypeChunk.getReferenceTo(index));
        final var player = holder.getComponent(Player.getComponentType());
        final var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        StatApplier.registerStatLevelListeners();
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            Player.getComponentType(),
            PlayerRef.getComponentType()
        );
    }
}
