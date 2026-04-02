package com.azuredoom.classescore.service;

import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.data.PassiveDefinition;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
import com.azuredoom.classescore.util.PlayerUtils;
import com.azuredoom.classescore.util.StatApplier;
import com.azuredoom.classescore.util.StatsUtils;

public final class ClassServiceImpl {

    private final JdbcClassesRepository repository;

    private final ClassRegistry classRegistry;

    private final PlayerRestrictionCache restrictionCache;

    private final Map<UUID, Optional<PlayerClassState>> playerStateCache = new ConcurrentHashMap<>();

    private static final Map<String, Supplier<Integer>> STAT_INDEX_MAP = Map.of(
        "health",
        DefaultEntityStatTypes::getHealth,
        "stamina",
        DefaultEntityStatTypes::getStamina,
        "mana",
        DefaultEntityStatTypes::getMana
    );

    public ClassServiceImpl(
        JdbcClassesRepository repository,
        ClassRegistry classRegistry,
        PlayerRestrictionCache restrictionCache
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.classRegistry = Objects.requireNonNull(classRegistry, "classRegistry");
        this.restrictionCache = Objects.requireNonNull(restrictionCache, "restrictionCache");
    }

    /**
     * Retrieves the current state of the player's class based on their unique identifier. If the player's state is not
     * already cached, it attempts to load the state from the repository.
     *
     * @param playerId the unique identifier of the player whose class state is to be retrieved
     * @return an {@code Optional} containing the {@code PlayerClassState} if the player has a class state assigned, or
     *         an empty {@code Optional} if no such state exists
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public Optional<PlayerClassState> getPlayerState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return playerStateCache.computeIfAbsent(playerId, this::loadPlayerState);
    }

    /**
     * Retrieves the selected class definition for a player based on their unique identifier. The method fetches the
     * player's current class state and uses the associated class ID to retrieve the corresponding
     * {@code ClassDefinition} from the class registry.
     *
     * @param playerId the unique identifier of the player whose class definition is to be retrieved
     * @return an {@code Optional} containing the {@code ClassDefinition} if the player has a selected class, or an
     *         empty {@code Optional} if no class is selected or the player ID is not found
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public Optional<ClassDefinition> getSelectedClassDefinition(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        return getPlayerState(playerId)
            .flatMap(state -> classRegistry.get(state.classId()));
    }

    /**
     * Selects a class for the specified player based on the given class ID. The method ensures that the player has not
     * already selected a class and that the provided class ID is valid. Once the class is selected, the player's stats
     * are updated according to the class definition, and the player's state is persisted and cached.
     *
     * @param playerId the unique identifier of the player selecting a class
     * @param classId  the identifier of the class being selected
     * @throws NullPointerException     if {@code playerId} is null
     * @throws IllegalArgumentException if {@code classId} is null, blank, or invalid
     * @throws IllegalStateException    if the player has already selected a class
     */
    public void selectClass(UUID playerId, String classId) {
        Objects.requireNonNull(playerId, "playerId");

        if (hasSelectedClass(playerId)) {
            throw new IllegalStateException("Player already has a class selected");
        }

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }

        var definition = classRegistry.get(classId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + classId));

        var now = System.currentTimeMillis();
        var state = new PlayerClassState(playerId, definition.id(), now, now);

        repository.savePlayerState(state);

        try {
            applyClassEffects(playerId, definition);

            playerStateCache.put(playerId, Optional.of(state));
            restrictionCache.setClass(playerId, definition);
        } catch (RuntimeException ex) {
            try {
                repository.deletePlayerState(playerId);
            } catch (RuntimeException suppressed) {
                ex.addSuppressed(suppressed);
            }

            playerStateCache.put(playerId, Optional.empty());
            restrictionCache.clear(playerId);

            try {
                removeClassEffects(playerId, definition);
            } catch (RuntimeException suppressed) {
                ClassesCore.LOGGER.at(Level.WARNING)
                    .withCause(suppressed)
                    .log("Failed to remove class effects after selecting class");
                ex.addSuppressed(suppressed);
            }

            throw ex;
        }
    }

    /**
     * Clears the player's selected class and removes any associated state, restrictions, and stat modifiers defined by
     * the chosen class.
     *
     * @param playerId the unique identifier of the player whose class selection is to be cleared
     * @param classId  the identifier of the class being cleared from the player
     * @throws NullPointerException     if {@code playerId} is {@code null}
     * @throws IllegalArgumentException if {@code classId} is unknown or invalid
     */
    public void clearClass(UUID playerId, String classId) {
        Objects.requireNonNull(playerId, "playerId");

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }

        var definition = classRegistry.get(classId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + classId));

        removeClassEffects(playerId, definition);

        try {
            repository.deletePlayerState(playerId);
            playerStateCache.put(playerId, Optional.empty());
            restrictionCache.clear(playerId);
        } catch (RuntimeException ex) {
            try {
                applyClassEffects(playerId, definition);
            } catch (RuntimeException suppressed) {
                ClassesCore.LOGGER.at(Level.WARNING)
                    .withCause(suppressed)
                    .log("Failed to apply class effects after clearing class");
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
    }

    /**
     * Checks if the player with the specified unique identifier has selected a class.
     *
     * @param playerId the unique identifier of the player whose class selection status is being verified
     * @return {@code true} if the player has a selected class, {@code false} otherwise
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public boolean hasSelectedClass(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return getPlayerState(playerId).isPresent();
    }

    /**
     * Determines whether a specific weapon is allowed for the player based on the player's selected class and the
     * associated equipment rules. If no class is selected for the player, all weapons are allowed by default.
     *
     * @param playerId the unique identifier of the player whose weapon permission is being checked
     * @param weaponId the unique identifier of the weapon being checked
     * @return {@code true} if the weapon is allowed for the player according to their class's equipment rules, or if no
     *         class is selected; {@code false} otherwise
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public boolean isWeaponAllowed(UUID playerId, String weaponId) {
        return restrictionCache.canUseWeapon(playerId, weaponId);
    }

    /**
     * Determines whether specific armor is allowed for the player based on their selected class and the associated
     * equipment rules. If no class is selected for the player, all armor is allowed by default.
     *
     * @param playerId the unique identifier of the player whose armor permission is being checked
     * @param armorId  the unique identifier of the armor being checked
     * @return {@code true} if the armor is allowed for the player according to their class's equipment rules, or if no
     *         class is selected; {@code false} otherwise
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public boolean isArmorAllowed(UUID playerId, String armorId) {
        return restrictionCache.canUseArmor(playerId, armorId);
    }

    /**
     * Loads the player state for the given player ID. This method retrieves the player's state from the repository,
     * validates the associated class definition, and updates the restriction cache accordingly. If the player's state
     * or class definition is not found, it returns an empty {@code Optional}.
     *
     * @param playerId the unique identifier of the player whose state is being loaded
     * @return an {@code Optional} containing the player's state if found and valid, or an empty {@code Optional} if no
     *         valid state exists
     */
    private Optional<PlayerClassState> loadPlayerState(UUID playerId) {
        restrictionCache.clear(playerId);

        var state = repository.findPlayerState(playerId);
        if (state.isEmpty()) {
            return Optional.empty();
        }

        var playerState = state.get();
        var classDefinition = classRegistry.get(playerState.classId());

        if (classDefinition.isEmpty()) {
            repository.deletePlayerState(playerId);
            return Optional.empty();
        }

        restrictionCache.setClass(playerId, classDefinition.get());
        return Optional.of(playerState);
    }

    /**
     * Evicts a player from the system by removing their cached state entries. This operation clears the player's
     * class-related restrictions and unloaded state data.
     *
     * @param playerId the unique identifier of the player to be evicted
     */
    public void evictPlayer(UUID playerId) {
        playerStateCache.remove(playerId);
        restrictionCache.clear(playerId);
    }

    /**
     * Applies the effects of a given class to a specific player, including initial class stats and passive effects.
     *
     * @param playerId   The unique identifier of the player to whom the class effects will be applied.
     * @param definition The class definition containing the stats and passives to apply to the player.
     */
    private void applyClassEffects(UUID playerId, ClassDefinition definition) {
        var playerContext = PlayerUtils.getPlayerContext(playerId).orElseThrow();

        StatApplier.applyInitialClassStats(playerId, definition);

        for (var passive : definition.passives()) {
            var index = resolveStatIndex(passive.id());
            if (index == null) {
                continue;
            }

            var type = resolveCalculationType(passive);
            if (type == null) {
                continue;
            }

            StatsUtils.doStatChange(
                playerContext.store(),
                playerContext.playerComponent(),
                index,
                type,
                passive.value(),
                passive.id()
            );
        }
    }

    /**
     * Removes the effects of a specific class from a player's stats.
     *
     * @param playerId   The unique identifier of the player from whom the class effects will be removed.
     * @param definition The class definition containing the passive effects to be removed.
     */
    private void removeClassEffects(UUID playerId, ClassDefinition definition) {
        var playerContext = PlayerUtils.getPlayerContext(playerId).orElseThrow();

        for (var passive : definition.passives()) {
            var index = resolveStatIndex(passive.id());
            if (index == null) {
                continue;
            }

            StatsUtils.removeStatModifier(
                playerContext.store(),
                playerContext.playerComponent(),
                index,
                passive.id()
            );
        }
    }

    /**
     * Resolves the stat index associated with a given passive identifier. The method processes the identifier to check
     * its match within the pre-defined {@code STAT_INDEX_MAP}, using a case-insensitive comparison.
     *
     * @param passiveId the passive identifier as a {@code String} used to find the corresponding stat index
     * @return an {@code Integer} representing the resolved stat index if a match is found, or {@code null} if no match
     *         exists
     */
    private Integer resolveStatIndex(String passiveId) {
        var id = passiveId.toLowerCase();
        return STAT_INDEX_MAP.entrySet()
            .stream()
            .filter(e -> id.contains(e.getKey()))
            .findFirst()
            .map(e -> e.getValue().get())
            .orElse(null);
    }

    /**
     * Resolves the calculation type for the given passive definition. The method determines how the passive effect
     * modifies attributes based on its type, mapping the passive's type to a specific calculation type.
     *
     * @param passive the {@code PassiveDefinition} object containing the details of the passive effect, including its
     *                type and associated attributes or effects.
     * @return the {@code StaticModifier.CalculationType} representing the calculation method to be used for the passive
     *         effect. Returns {@code null} if the passive's type does not match any known types.
     */
    private StaticModifier.CalculationType resolveCalculationType(PassiveDefinition passive) {
        return switch (passive.type()) {
            case ATTRIBUTE_MULTIPLIER -> StaticModifier.CalculationType.MULTIPLICATIVE;
            case ATTRIBUTE_ADDITIVE -> StaticModifier.CalculationType.ADDITIVE;
            default -> null;
        };
    }
}
