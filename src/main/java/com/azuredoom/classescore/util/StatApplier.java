package com.azuredoom.classescore.util;

import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.azuredoom.levelingcore.level.LevelServiceImpl;

import java.util.UUID;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.data.ClassDefinition;

/**
 * Utility class for managing and applying player statistics. This class provides several static methods for retrieving,
 * updating, modifying, and initializing player stats based on their class definitions and level progression.
 * <p>
 * StatApplier works in conjunction with the {@code LevelServiceImpl} for persisting and modifying player stats, and
 * uses player class data from the {@code ClassDefinition} to handle class-specific stat logic.
 * <p>
 * This class is intended to be used as a utility and cannot be instantiated.
 */
public final class StatApplier {

    private StatApplier() {}

    /**
     * Retrieves a specified stat value for a player based on the given stat type. The stat value is fetched using the
     * provided level service by delegating to the corresponding stat-specific method, depending on the provided
     * {@code statType}.
     *
     * @param levelService The service responsible for managing player stats.
     * @param playerUUID   The unique identifier of the player whose stat is being queried.
     * @param statType     The type of stat to retrieve (e.g., STRENGTH, AGILITY, PERCEPTION).
     * @return The value of the specified stat for the given player.
     */
    public static int getStat(LevelServiceImpl levelService, UUID playerUUID, StatType statType) {
        return switch (statType) {
            case STRENGTH -> levelService.getStr(playerUUID);
            case AGILITY -> levelService.getAgi(playerUUID);
            case PERCEPTION -> levelService.getPer(playerUUID);
            case VITALITY -> levelService.getVit(playerUUID);
            case INTELLIGENCE -> levelService.getInt(playerUUID);
            case CONSTITUTION -> levelService.getCon(playerUUID);
        };
    }

    /**
     * Updates a specific stat for a player with the given value. The stat to be updated is determined by the
     * {@code statType} parameter, and the update is applied using the appropriate method from the provided
     * {@link LevelServiceImpl}.
     *
     * @param levelService The service used to manage player stats.
     * @param playerUUID   The unique identifier of the player whose stat is being updated.
     * @param statType     The type of stat to update (e.g., STRENGTH, AGILITY).
     * @param value        The new value to set for the specified stat.
     */
    public static void setStat(LevelServiceImpl levelService, UUID playerUUID, StatType statType, int value) {
        switch (statType) {
            case STRENGTH -> levelService.setStr(playerUUID, value);
            case AGILITY -> levelService.setAgi(playerUUID, value);
            case PERCEPTION -> levelService.setPer(playerUUID, value);
            case VITALITY -> levelService.setVit(playerUUID, value);
            case INTELLIGENCE -> levelService.setInt(playerUUID, value);
            case CONSTITUTION -> levelService.setCon(playerUUID, value);
        }
    }

    /**
     * Adds a specified amount to a player's stat of the given type. The method retrieves the current value of the stat,
     * increments it by the specified amount, and updates the stat with the new value.
     *
     * @param levelService The service used to manage and retrieve player stats.
     * @param playerUUID   The unique identifier of the player whose stat is being adjusted.
     * @param statType     The type of stat to adjust (e.g., strength, agility, intelligence).
     * @param amount       The amount by which to increase the player's stat.
     */
    public static void addToStat(LevelServiceImpl levelService, UUID playerUUID, StatType statType, int amount) {
        var current = getStat(levelService, playerUUID, statType);
        setStat(levelService, playerUUID, statType, current + amount);
    }

    /**
     * Applies the initial stats for a player's selected class based on their current level. The method calculates the
     * stat increases by taking the base value of each stat from the class definition and adding scaling based on the
     * player's level.
     *
     * @param playerUUID      The unique identifier of the player whose stats are being applied.
     * @param classDefinition The class definition containing the base stats and scaling information to apply.
     */
    public static void applyInitialClassStats(UUID playerUUID, ClassDefinition classDefinition) {
        LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {
            var level = levelService.getLevel(playerUUID);

            for (var statDef : classDefinition.stats()) {
                var statType = StatType.fromJson(statDef.id());

                var amount = statDef.base();
                if (level > 1) {
                    amount += (level - 1) * statDef.perLevel();
                }

                StatApplier.addToStat(levelService, playerUUID, statType, amount);
            }
        });
    }

    /**
     * Registers listeners to handle level-up and level-down events for player stats. <br>
     * For both listeners:
     * <ul>
     * <li>The player's selected class is retrieved to determine the stats and their per-level values.</li>
     * <li>Only stats with a positive per-level value are adjusted.</li>
     * <li>The cumulative effect of the level changes is applied to the player's stats via the level service.</li>
     * </ul>
     * If the player has no selected class or the changes result in no net level difference, no adjustments are made.
     */
    public static void registerStatLevelListeners() {
        LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {
            levelService.registerLevelUpListener((playerUUID, newLevel, oldLevel) -> {
                var classDefinition = ClassesCore.getClassServiceIfPresent()
                    .flatMap(service -> service.getSelectedClassDefinition(playerUUID))
                    .orElse(null);
                if (classDefinition == null) {
                    return;
                }

                var levelsGained = Math.max(0, newLevel - oldLevel);
                if (levelsGained == 0) {
                    return;
                }

                for (var statDef : classDefinition.stats()) {
                    if (statDef.perLevel() <= 0) {
                        continue;
                    }

                    var statType = StatType.fromJson(statDef.id());
                    StatApplier.addToStat(levelService, playerUUID, statType, statDef.perLevel() * levelsGained);
                }
            });

            levelService.registerLevelDownListener((playerUUID, newLevel, oldLevel) -> {
                var classDefinition = ClassesCore.getClassServiceIfPresent()
                    .flatMap(service -> service.getSelectedClassDefinition(playerUUID))
                    .orElse(null);
                if (classDefinition == null) {
                    return;
                }

                var levelsLost = Math.max(0, oldLevel - newLevel);
                if (levelsLost == 0) {
                    return;
                }

                for (var statDef : classDefinition.stats()) {
                    if (statDef.perLevel() <= 0) {
                        continue;
                    }

                    var statType = StatType.fromJson(statDef.id());
                    StatApplier.addToStat(levelService, playerUUID, statType, -(statDef.perLevel() * levelsLost));
                }
            });
        });
    }
}
