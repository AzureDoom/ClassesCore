package com.azuredoom.classescore.gameplay.services.items;

import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Represents the state of the item held in a player's hand during a hand-check process. This record is used for
 * managing and storing details about a player's held item, including item data and whether it is blocked for use based
 * on game rules.
 *
 * @param itemStack The item stack representing the held item.
 * @param itemId    The identifier of the held item.
 * @param blocked   Indicates if the item use is restricted based on game rules.
 */
public record HandCheckState(
    ItemStack itemStack,
    String itemId,
    boolean blocked
) {}
