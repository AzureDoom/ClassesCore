package com.azuredoom.classescore.gameplay.services.armor;

import com.azuredoom.levelingcore.systems.equipment.ArmorBlockLevelSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
import com.azuredoom.classescore.util.NotificationsUtil;

public class ArmorBlockClassSystem extends ArmorBlockLevelSystem {

    private final PlayerRestrictionCache restrictionCache;

    public ArmorBlockClassSystem(PlayerRestrictionCache restrictionCache) {
        super();
        this.restrictionCache = restrictionCache;
    }

    @Override
    protected void rollbackArmorTransaction(
        @NotNull Player player,
        @NotNull ItemContainer armorContainer,
        @Nullable Transaction transaction,
        @NotNull Set<String> refundedKeys
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

    @Override
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
}
