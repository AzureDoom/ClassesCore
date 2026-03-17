package com.azuredoom.classescore.compat;

import com.azuredoom.levelingcore.LevelingCore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.herolias.tooltips.api.DynamicTooltipsApiProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.ClassesCoreAPI;

public class DynamicTooltipsLibCompat {

    private static boolean registered = false;

    private static final Timer SCAN_TIMER = new Timer("classescore-dynamic-tooltips", true);

    private final Set<String> processedItems = ConcurrentHashMap.newKeySet();

    private final Map<String, List<String>> allowedClassesByItem = new ConcurrentHashMap<>();

    public static final DynamicTooltipsLibCompat INSTANCE = new DynamicTooltipsLibCompat();

    private DynamicTooltipsLibCompat() {}

    public static void register() {
        if (registered)
            return;
        registered = true;

        var api = DynamicTooltipsApiProvider.get();
        if (api == null)
            return;

        SCAN_TIMER.schedule(new TimerTask() {

            {
                Objects.requireNonNull(DynamicTooltipsLibCompat.INSTANCE);
            }

            @Override
            public void run() {
                DynamicTooltipsLibCompat.INSTANCE.scanForItems();
            }
        }, 10000L);
    }

    /**
     * Scans through all available items and processes them to dynamically generate tooltips based on their allowed
     * classes. Items that have been processed or contain invalid identifiers are skipped.
     */
    private void scanForItems() {
        try {
            var allItems = Item.getAssetMap().getAssetMap().values();

            for (var item : allItems) {
                var itemId = item.getId();
                if (itemId == null || this.processedItems.contains(itemId)) {
                    continue;
                }

                if (this.processItem(item)) {
                    this.processedItems.add(itemId);
                }
            }
        } catch (Exception e) {
            ClassesCore.LOGGER.at(Level.WARNING)
                .withCause(e)
                .log("Dynamic tooltips scan failed");
        }
    }

    /**
     * Processes the given item to determine its allowed classes and dynamically adds a tooltip line indicating the
     * allowed classes.
     *
     * @param item The item to process. It must have a valid non-blank ID.
     * @return {@code true} if the item was successfully processed and the tooltip was added; {@code false} otherwise,
     *         such as when the API is unavailable or when an exception occurs.
     */
    private boolean processItem(Item item) {
        var api = DynamicTooltipsApiProvider.get();
        if (api == null)
            return false;

        try {
            var itemId = item.getId();
            if (itemId == null || itemId.isBlank()) {
                return false;
            }

            var allowedClasses = allowedClassesByItem.computeIfAbsent(itemId, this::findAllowedClassesForItem);

            if (allowedClasses.isEmpty()) {
                return true;
            }

            var tooltipText = "Classes allowed: " + String.join(", ", allowedClasses);

            api.addGlobalLine(itemId, tooltipText);

            return true;
        } catch (Exception e) {
            LevelingCore.LOGGER.at(Level.WARNING)
                .withCause(e)
                .log("Failed to process tooltip for item " + item.getId());
            return false;
        }
    }

    /**
     * Finds and returns a sorted list of class names where the specified item is allowed based on its equipment rules.
     *
     * @param itemId The unique identifier of the item to check.
     * @return A list of class display names where the item is allowed, sorted in case-insensitive order.
     */
    private List<String> findAllowedClassesForItem(String itemId) {
        var result = new ArrayList<String>();

        for (var classDef : ClassesCoreAPI.getClasses()) {
            if (classDef == null || classDef.equipmentRules() == null) {
                continue;
            }

            var allowed =
                classDef.equipmentRules().isWeaponAllowed(itemId) ||
                    classDef.equipmentRules().isArmorAllowed(itemId);

            if (allowed) {
                result.add(classDef.displayName());
            }
        }

        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }
}
