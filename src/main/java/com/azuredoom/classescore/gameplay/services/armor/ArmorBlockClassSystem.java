package com.azuredoom.classescore.gameplay.services.armor;

import com.azuredoom.levelingcore.systems.equipment.ArmorBlockLevelSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        @NotNull Set<String> refundedKeys,
        @NotNull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (transaction == null || !transaction.succeeded()) {
            return;
        }

        switch (transaction) {
            case MoveTransaction<?> moveTransaction -> {
                if (moveTransaction.getMoveType() == MoveType.MOVE_TO_SELF) {
                    rollbackArmorTransaction(player, armorContainer, moveTransaction.getAddTransaction(), refundedKeys, commandBuffer);
                }
            }
            case ListTransaction<?> listTransaction -> {
                for (var nested : listTransaction.getList()) {
                    rollbackArmorTransaction(player, armorContainer, nested, refundedKeys, commandBuffer);
                }
            }
            case ItemStackTransaction itemStackTransaction -> {
                for (var slotTransaction : itemStackTransaction.getSlotTransactions()) {
                    rollbackArmorTransaction(player, armorContainer, slotTransaction, refundedKeys, commandBuffer);
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
                var playerRef = player.getReference();
                if (playerRef == null) {
                    return;
                }
                var playerRefComponent = playerRef.getStore()
                        .getComponent(playerRef, PlayerRef.getComponentType());
                if (playerRefComponent == null) {
                    return;
                }
                var playerUuid = playerRefComponent.getUuid();
                if (restrictionCache.canUseArmor(playerUuid, itemId)) {
                    return;
                }

                var classId = restrictionCache.getClassId(playerUuid).orElse("Unknown");

                NotificationsUtil.sendItemClassRestrictionNotification(
                        playerRefComponent,
                    after,
                    classId
                );

                var swapping = (before != null && !ItemStack.isEmpty(before));

                armorContainer.setItemStackForSlot(slotTransaction.getSlot(), before, true);

                var key = "armorSlot:" + slotTransaction.getSlot();
                if (refundedKeys.add(key)) {
                    var everythingInventoryComponent = InventoryComponent.getCombined(
                            commandBuffer,
                            playerRef,
                            InventoryComponent.EVERYTHING
                    );
                    giveOrDrop(player, after, everythingInventoryComponent);

                    if (swapping) {
                        var removeOne = oneOf(before);
                        everythingInventoryComponent.removeItemStack(removeOne, false, true);
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public void validateArmorOnReady(@Nonnull Player player) {
        var playerRef = player.getReference();
        if (playerRef == null) {
            return;
        }
        var playerRefComponent = playerRef.getStore()
                .getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        var playerUuid = playerRefComponent.getUuid();
        ignoreArmorEvents.add(playerUuid);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> ignoreArmorEvents.remove(playerUuid),
            500L,
            TimeUnit.MILLISECONDS
        );

        var armorComponent = playerRef.getStore().getComponent(playerRef, InventoryComponent.Armor.getComponentType());
        if (armorComponent == null) {
            return;
        }
        var armor = armorComponent.getInventory();
        if (armor == null) {
            return;
        }

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

                if (restrictionCache.canUseArmor(playerUuid, itemId)) {
                    continue;
                }

                var classId = restrictionCache.getClassId(playerUuid).orElse("Unknown");

                NotificationsUtil.sendItemClassRestrictionNotification(
                        playerRefComponent,
                    stack,
                    classId
                );

                armor.setItemStackForSlot(slot, null, true);
                var everythingInventoryComponent = InventoryComponent.getCombined(
                        playerRef.getStore(),
                        playerRef,
                        InventoryComponent.EVERYTHING
                );
                giveOrDrop(player, stack, everythingInventoryComponent);
            }
        } finally {
            restoringArmor = false;
        }
    }
}
