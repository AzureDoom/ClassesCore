package com.azuredoom.classescore.compat;

import com.azuredoom.levelingcore.LevelingCore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.herolias.tooltips.api.DynamicTooltipsApiProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.ClassesCoreAPI;
import com.azuredoom.classescore.data.ClassDefinition;

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

            var tooltipText = "Usable by: " + String.join(", ", allowedClasses);

            api.addGlobalLine(itemId, tooltipText);

            return true;
        } catch (Exception e) {
            LevelingCore.LOGGER.at(Level.WARNING)
                .withCause(e)
                .log("Failed to process tooltip for item " + item.getId());
            return false;
        }
    }

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
