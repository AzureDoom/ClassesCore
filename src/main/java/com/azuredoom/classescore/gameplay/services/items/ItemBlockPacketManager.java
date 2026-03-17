package com.azuredoom.classescore.gameplay.services.items;

import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.util.NotificationsUtil;

/**
 * Manages item and block interaction packets for players, implementing restrictions based on the player's item usage
 * level and other custom rules.
 */
public class ItemBlockPacketManager {

    private volatile boolean registered = false;

    private final Map<UUID, Long> notifyCooldownMs = new ConcurrentHashMap<>();

    private final Map<UUID, HandCheckState> handCheckState = new ConcurrentHashMap<>();

    private final PlayerRestrictionCache restrictionCache;

    public ItemBlockPacketManager(PlayerRestrictionCache restrictionCache) {
        this.restrictionCache = restrictionCache;
    }

    public Map<UUID, HandCheckState> getHandCheckState() {
        return handCheckState;
    }

    /**
     * Starts the packet management functionality for item level restrictions. This method registers a packet adapter
     * for intercepting and handling specific inbound packets related to item interactions. When enabled in the
     * configuration, the registered adapter inspects interaction packets to determine whether they should be blocked
     * based on the player's level and the interaction chain's state.
     */
    public void start() {
        if (registered)
            return;
        registered = true;

        PacketAdapters.registerInbound((PlayerPacketFilter) (playerRef, packet) -> {
            if (!ClassesCore.getConfig().get().isEnableClassItemRestrictions()) {
                return false;
            }

            if (packet instanceof SyncInteractionChains chainsPacket) {
                var cancel = false;
                for (var chain : chainsPacket.updates) {
                    if (shouldBlock(playerRef, chain)) {
                        cancel = true;
                        break;
                    }
                }
                return cancel;
            }

            if (packet instanceof SyncInteractionChain chain) {
                return shouldBlock(playerRef, chain);
            }

            return false;
        });
    }

    /**
     * This method disables the packet interception by setting the internal registration status to {@code false},
     * effectively halting the functionality of monitoring item-related packets. Additionally, it clears any stored
     * cooldown information for level requirement notifications, ensuring that the state is reset during this shutdown
     * process.
     */
    public void shutdown() {
        registered = false;
        notifyCooldownMs.clear();
    }

    /**
     * Determines whether an interaction should be blocked based on the player's state and the ongoing interaction
     * chain.
     *
     * @param playerRef The reference to the player performing the interaction. This parameter cannot be null.
     * @param chain     The interaction chain containing the details of the current interaction. This parameter cannot
     *                  be null.
     * @return {@code true} if the interaction should be blocked based on the player's class and the interaction chain's
     *         state; {@code false} otherwise.
     */
    private boolean shouldBlock(PlayerRef playerRef, SyncInteractionChain chain) {
        if (playerRef == null || chain == null)
            return false;

        switch (chain.interactionType) {
            case Primary, Secondary, Use, Ability1, Ability2, Ability3, ProjectileSpawn -> {}
            default -> {
                return false;
            }
        }

        if (chain.state != InteractionState.NotFinished)
            return false;

        var snap = handCheckState.get(playerRef.getUuid());
        if (snap == null || !snap.blocked())
            return false;

        var classId = restrictionCache.getClassId(playerRef.getUuid()).orElse(null);

        maybeNotify(playerRef, snap.itemStack(), classId);
        return true;
    }

    /**
     * Sends an item requirement notification to the player if certain conditions are met. The method ensures that
     * notifications are not sent too frequently by enforcing a cooldown period.
     *
     * @param playerRef   The reference to the player who should receive the notification.
     * @param hand        The item stack in the player's hand.
     * @param playerClass The player's current class.
     */
    private void maybeNotify(PlayerRef playerRef, ItemStack hand, String playerClass) {
        var now = System.currentTimeMillis();
        var nextOk = notifyCooldownMs.getOrDefault(playerRef.getUuid(), 0L);
        if (now < nextOk)
            return;
        notifyCooldownMs.put(playerRef.getUuid(), now + 750L);

        NotificationsUtil.sendItemClassRestrictionNotification(playerRef, hand, playerClass);
    }

    /**
     * Clears the state data associated with the specified player. This method removes the player's hand check state and
     * notification cooldown information from their respective tracking structures.
     *
     * @param playerId The UUID of the player whose data should be cleared. Cannot be null.
     */
    public void clearPlayer(UUID playerId) {
        handCheckState.remove(playerId);
        notifyCooldownMs.remove(playerId);
    }
}
