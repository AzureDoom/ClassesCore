package com.azuredoom.classescore.gameplay.services.items;

import com.hypixel.hytale.server.core.inventory.ItemStack;

public record HandCheckState(
    ItemStack itemStack,
    String itemId,
    boolean blocked
) {}
