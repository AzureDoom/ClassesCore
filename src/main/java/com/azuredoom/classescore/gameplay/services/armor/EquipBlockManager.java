package com.azuredoom.classescore.gameplay.services.armor;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
import com.azuredoom.classescore.util.NotificationsUtil;

@SuppressWarnings("removal")
public class EquipBlockManager {

    @Nullable
    private volatile EventRegistration<?, LivingEntityInventoryChangeEvent> inventoryChangeRegistration;

    private final Set<UUID> ignoreArmorEvents = ConcurrentHashMap.newKeySet();

    private volatile boolean restoringArmor = false;

    private final PlayerRestrictionCache restrictionCache;

    public EquipBlockManager(PlayerRestrictionCache restrictionCache) {
        this.restrictionCache = restrictionCache;
    }

    public void start() {
        if (inventoryChangeRegistration == null || !inventoryChangeRegistration.isRegistered()) {
            inventoryChangeRegistration = ClassesCore.getInstance()
                .getEventRegistry()
                .registerGlobal(LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
        }
    }

    public void shutdown() {
        EventRegistration<?, LivingEntityInventoryChangeEvent> inventoryRegistration = inventoryChangeRegistration;
        if (inventoryRegistration != null && inventoryRegistration.isRegistered()) {
            inventoryRegistration.unregister();
        }
        inventoryChangeRegistration = null;
    }

    /**
     * Validates and ensures that the armor equipped by a player conforms to their class restrictions. Any items in the
     * player's armor slots that are not allowed will be removed and returned to the player, either by adding them back
     * to their inventory or dropping them in the world if there is no space.
     *
     * @param player the player whose armor is being validated; must not be null
     */
    public void validateArmorOnReady(@Nonnull Player player) {
        ignoreArmorEvents.add(player.getUuid());
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> ignoreArmorEvents.remove(player.getUuid()),
            500L,
            TimeUnit.MILLISECONDS
        );

        var inventory = player.getInventory();
        var armor = inventory.getArmor();
        if (armor == null) {
            return;
        }

        var playerId = player.getUuid();

        restoringArmor = true;
        try {
            var capacity = armor.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                var stack = armor.getItemStack(slot);
                if (stack == null || ItemStack.isEmpty(stack)) {
                    continue;
                }

                var itemId = stack.getItemId();
                if (itemId.isEmpty()) {
                    continue;
                }

                if (restrictionCache.canUseArmor(playerId, itemId)) {
                    continue;
                }

                var classId = restrictionCache.getClassId(playerId).orElse("Unknown");

                NotificationsUtil.sendItemClassRestrictionNotification(
                    player.getPlayerRef(),
                    stack,
                    classId
                );

                armor.setItemStackForSlot(slot, null, true);
                giveOrDrop(player, stack);
            }
        } finally {
            restoringArmor = false;
        }
    }

    /**
     * Handles inventory change events and validates player armor to ensure compliance with class-specific restrictions.
     * If any invalid armor is identified in the player's armor slots, it initiates rollback operations to restore the
     * previous valid state.
     * <p>
     * The method reacts only to changes specifically in the armor container of a player entity and ensures that no
     * unnecessary processing occurs for unrelated events or non-player entities.
     *
     * @param event the inventory change event containing details about the changes that occurred; must not be null
     */
    private void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (restoringArmor) {
            return;
        }

        if (ignoreArmorEvents.contains(player.getUuid())) {
            return;
        }

        var inventory = player.getInventory();
        var armorContainer = inventory.getArmor();
        if (armorContainer == null) {
            return;
        }

        var changedContainer = event.getItemContainer();
        if (changedContainer == null || changedContainer != armorContainer) {
            return;
        }

        var transaction = event.getTransaction();
        if (transaction == null) {
            return;
        }

        restoringArmor = true;
        try {
            rollbackArmorTransaction(player, armorContainer, transaction, new HashSet<>());
        } finally {
            restoringArmor = false;
        }
    }

    /**
     * Rolls back transactions related to armor equipment for a player. This method ensures that any armor being
     * equipped is valid, according to class-specific restrictions, and if not, reverts the changes to restore the
     * previous valid state. Invalid items are refunded to the player's inventory or dropped if there's no available
     * space. Notifications are sent to inform the player about class-based restrictions on the armor items.
     *
     * @param player         the player whose armor transaction needs to be rolled back; must not be null
     * @param armorContainer the container holding the player's armor items; must not be null
     * @param transaction    the transaction representing the armor equipment operation; may be null if no transaction
     *                       exists
     * @param refundedKeys   a set of keys representing already refunded slots to avoid duplicate refunds; must not be
     *                       null
     */
    private void rollbackArmorTransaction(
        @Nonnull Player player,
        @Nonnull ItemContainer armorContainer,
        @Nullable Transaction transaction,
        @Nonnull Set<String> refundedKeys
    ) {
        if (transaction == null || !transaction.succeeded()) {
            return;
        }

        switch (transaction) {
            case MoveTransaction<?> moveTransaction -> {
                if (moveTransaction.getMoveType() == MoveType.MOVE_TO_SELF) {
                    rollbackArmorTransaction(player, armorContainer, moveTransaction.getAddTransaction(), refundedKeys);
                }
            }
            case ListTransaction<?> listTransaction -> {
                for (var nested : listTransaction.getList()) {
                    rollbackArmorTransaction(player, armorContainer, nested, refundedKeys);
                }
            }
            case ItemStackTransaction itemStackTransaction -> {
                for (var slotTransaction : itemStackTransaction.getSlotTransactions()) {
                    rollbackArmorTransaction(player, armorContainer, slotTransaction, refundedKeys);
                }
            }
            case SlotTransaction slotTransaction -> {
                var before = slotTransaction.getSlotBefore();
                var after = slotTransaction.getSlotAfter();

                if (after == null || ItemStack.isEmpty(after)) {
                    return;
                }

                if (sameStack(before, after)) {
                    return;
                }

                var itemId = after.getItemId();
                if (itemId.isEmpty()) {
                    return;
                }

                var playerId = player.getUuid();
                if (restrictionCache.canUseArmor(playerId, itemId)) {
                    return;
                }

                var classId = restrictionCache.getClassId(playerId).orElse("Unknown");

                NotificationsUtil.sendItemClassRestrictionNotification(
                    player.getPlayerRef(),
                    after,
                    classId
                );

                var swapping = (before != null && !ItemStack.isEmpty(before));

                armorContainer.setItemStackForSlot(slotTransaction.getSlot(), before, true);

                var key = "armorSlot:" + slotTransaction.getSlot();
                if (refundedKeys.add(key)) {
                    giveOrDrop(player, after);

                    if (swapping) {
                        var removeOne = oneOf(before);
                        player.getInventory().getCombinedHotbarFirst().removeItemStack(removeOne, false, true);
                    }
                }
            }
            default -> {}
        }
    }

    /**
     * Compares two ItemStack objects to determine if they are equivalent in terms of item type, quantity, and metadata.
     * Handles null and empty item cases.
     *
     * @param a the first ItemStack to compare; may be null
     * @param b the second ItemStack to compare; may be null
     * @return true if both ItemStack objects are equivalent, considering their item type, quantity, and metadata, or if
     *         both are null or empty; false otherwise
     */
    private static boolean sameStack(@Nullable ItemStack a, @Nullable ItemStack b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (ItemStack.isEmpty(a) && ItemStack.isEmpty(b)) {
            return true;
        }
        if (ItemStack.isEmpty(a) || ItemStack.isEmpty(b)) {
            return false;
        }

        if (!Objects.equals(a.getItemId(), b.getItemId())) {
            return false;
        }
        if (a.getQuantity() != b.getQuantity()) {
            return false;
        }
        return Objects.equals(a.getMetadata(), b.getMetadata());
    }

    /**
     * Creates a new {@code ItemStack} with a quantity of one, replicating the item type and metadata of the given
     * {@code ItemStack}.
     *
     * @param stack the {@code ItemStack} to base the new stack on; must not be null
     * @return a new {@code ItemStack} with the same item type and metadata as the input, but with a quantity of one
     */
    private static ItemStack oneOf(@Nonnull ItemStack stack) {
        return new ItemStack(stack.getItemId(), 1, stack.getMetadata());
    }

    /**
     * Attempts to give the specified {@link ItemStack} to the player. If the item cannot be added to the player's
     * inventory due to lack of space, it will be dropped in the world at the player's location.
     *
     * @param player the player who is receiving or dropping the item; must not be null
     * @param stack  the item stack being given or dropped; must not be null
     */
    private static void giveOrDrop(@Nonnull Player player, @Nonnull ItemStack stack) {
        if (ItemStack.isEmpty(stack)) {
            return;
        }

        var inv = player.getInventory().getCombinedHotbarFirst();

        var tx = inv.addItemStack(stack);
        var remainder = tx.getRemainder();

        if (remainder != null && !ItemStack.isEmpty(remainder)) {
            var ref = player.getReference();
            if (ref != null) {
                ItemUtils.dropItem(ref, stack, ref.getStore());
            }
        }
    }
}
