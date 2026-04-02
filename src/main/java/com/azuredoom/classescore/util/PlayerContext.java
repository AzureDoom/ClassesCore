package com.azuredoom.classescore.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Encapsulates the context of a player within the gaming framework. The context provides access to essential parts
 * required to manage and modify player-related data, including the player's entity store, player components, and
 * references to the player entity.
 *
 * @param store           The store providing access to {@link EntityStore} components. Used for managing and retrieving
 *                        data associated with various entities, including the player.
 * @param playerComponent The player-specific component providing access to the core functionality and state of the
 *                        player within the game. Represents the player entity.
 * @param playerRef       A reference to the player entity. Used to link a player component back to its entity data
 *                        within the {@link EntityStore}.
 * @param ref             A modifiable reference to the {@link EntityStore}. Provides an interface for accessing and
 *                        modifying entity-related stores.
 */
public record PlayerContext(
    Store<EntityStore> store,
    Player playerComponent,
    PlayerRef playerRef,
    Ref<EntityStore> ref
) {}
