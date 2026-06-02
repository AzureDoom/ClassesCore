package com.azuredoom.classescore.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerClassContextManager {

    private static final Map<UUID, ClassContext> PLAYER_CONTEXTS = new ConcurrentHashMap<>();

    public record ClassContext(
        Player player,
        World world,
        Store<EntityStore> store,
        Ref<EntityStore> entityRef
    ) {}

    private PlayerClassContextManager() {}

    /**
     * Registers a player's class context for later retrieval.
     * <p>
     * Silently no-ops if {@code player}, {@code store}, or {@code entityRef} is {@code null}, or if {@code entityRef}
     * is no longer valid at the time of the call.
     *
     * @param playerId  the UUID of the player to track
     * @param player    the {@link Player} instance
     * @param store     the {@link Store} holding the player's {@link EntityStore}
     * @param entityRef a {@link Ref} to the player's entity within the store
     */
    public static void trackPlayer(
        UUID playerId,
        Player player,
        Store<EntityStore> store,
        Ref<EntityStore> entityRef
    ) {
        if (player == null || store == null || entityRef == null || !entityRef.isValid()) {
            return;
        }

        var world = store.getExternalData().getWorld();

        PLAYER_CONTEXTS.put(
            playerId,
            new ClassContext(player, world, store, entityRef)
        );
    }

    /**
     * Returns the {@link ClassContext} associated with the given player, or {@code null} if no context has been
     * registered for that UUID.
     *
     * @param playerId the UUID of the player
     * @return the stored {@link ClassContext}, or {@code null} if absent
     */
    public static ClassContext getContext(UUID playerId) {
        return PLAYER_CONTEXTS.get(playerId);
    }

    /**
     * Removes the {@link ClassContext} for the given player, freeing any references held by the manager. Should be
     * called when the player leaves or their session ends.
     *
     * @param playerId the UUID of the player whose context should be discarded
     */
    public static void clear(UUID playerId) {
        PLAYER_CONTEXTS.remove(playerId);
    }
}
