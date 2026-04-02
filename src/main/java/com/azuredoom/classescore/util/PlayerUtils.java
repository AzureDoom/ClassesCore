package com.azuredoom.classescore.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Optional;
import java.util.UUID;

public final class PlayerUtils {

    private PlayerUtils() {}

    /**
     * Retrieves the context of a player based on their unique identifier. The method fetches the player's reference
     * from the universe and ensures the necessary parts are loaded to create a {@code PlayerContext}.
     *
     * @param playerId the unique identifier of the player whose context is to be retrieved
     * @return an {@code Optional} containing the {@code PlayerContext} if the player exists and all required components
     *         are successfully loaded, or an empty {@code Optional} if the player does not exist or lacks a valid
     *         reference
     */
    public static Optional<PlayerContext> getPlayerContext(UUID playerId) {
        var playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || playerRef.getReference() == null) {
            return Optional.empty();
        }

        var reference = playerRef.getReference();
        var store = reference.getStore();
        var component = store.ensureAndGetComponent(reference, Player.getComponentType());
        return Optional.of(new PlayerContext(store, component, playerRef, reference));
    }
}
