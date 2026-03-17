package com.azuredoom.classescore.service;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
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

        var playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) {
            return;
        }
        var store = Objects.requireNonNull(playerRef.getReference()).getStore();
        var playerComponent = store.ensureAndGetComponent(playerRef.getReference(), Player.getComponentType());
        StatApplier.applyInitialClassStats(playerId, definition);
        for (var passive : definition.passives()) {
            StaticModifier.CalculationType type;
            var id = passive.id().toLowerCase();
            var index = STAT_INDEX_MAP.entrySet()
                .stream()
                .filter(e -> id.contains(e.getKey()))
                .findFirst()
                .map(e -> e.getValue().get())
                .orElse(null);
            if (index == null) {
                continue;
            }
            switch (passive.type()) {
                case ATTRIBUTE_MULTIPLIER -> type = StaticModifier.CalculationType.MULTIPLICATIVE;
                case ATTRIBUTE_ADDITIVE -> type = StaticModifier.CalculationType.ADDITIVE;
                default -> type = null;
            }
            if (type == null) {
                continue;
            }
            StatsUtils.doStatChange(store, playerComponent, index, type, passive.value(), passive.id());
        }

        var state = getPlayerState(playerId)
            .map(
                existing -> new PlayerClassState(
                    existing.playerId(),
                    definition.id(),
                    existing.createdAt(),
                    now
                )
            )
            .orElseGet(
                () -> new PlayerClassState(
                    playerId,
                    definition.id(),
                    now,
                    now
                )
            );

        repository.savePlayerState(state);
        playerStateCache.put(playerId, Optional.of(state));
        restrictionCache.setClass(playerId, definition);
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

        repository.deletePlayerState(playerId);
        playerStateCache.put(playerId, Optional.empty());
        restrictionCache.clear(playerId);

        var definition = classRegistry.get(classId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + classId));
        var playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) {
            return;
        }
        var store = Objects.requireNonNull(playerRef.getReference()).getStore();
        var playerComponent = store.ensureAndGetComponent(playerRef.getReference(), Player.getComponentType());
        for (var passive : definition.passives()) {
            var id = passive.id().toLowerCase();
            var index = STAT_INDEX_MAP.entrySet()
                .stream()
                .filter(e -> id.contains(e.getKey()))
                .findFirst()
                .map(e -> e.getValue().get())
                .orElse(null);
            if (index == null) {
                continue;
            }
            StatsUtils.removeStatModifier(store, playerComponent, index, passive.id());
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
        Objects.requireNonNull(playerId, "playerId");

        if (weaponId == null || weaponId.isBlank()) {
            return false;
        }

        return getSelectedClassDefinition(playerId)
            .map(definition -> definition.equipmentRules().isWeaponAllowed(weaponId))
            .orElse(true);
    }

    /**
     * Determines whether a specific armor is allowed for the player based on their selected class and the associated
     * equipment rules. If no class is selected for the player, all armor is allowed by default.
     *
     * @param playerId the unique identifier of the player whose armor permission is being checked
     * @param armorId  the unique identifier of the armor being checked
     * @return {@code true} if the armor is allowed for the player according to their class's equipment rules, or if no
     *         class is selected; {@code false} otherwise
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public boolean isArmorAllowed(UUID playerId, String armorId) {
        Objects.requireNonNull(playerId, "playerId");

        if (armorId == null || armorId.isBlank()) {
            return false;
        }

        return getSelectedClassDefinition(playerId)
            .map(definition -> definition.equipmentRules().isArmorAllowed(armorId))
            .orElse(true);
    }

    /**
     * Loads the state of a player's selected class identified by their unique identifier. The method retrieves the
     * player's class state from the repository and, if a valid state is found, fetches the corresponding class
     * definition from the class registry. If the class definition exists, it updates the restriction cache with the
     * class-specific equipment restrictions for the player.
     *
     * @param playerId the unique identifier of the player whose class state is to be loaded
     * @return an {@code Optional} containing the {@code PlayerClassState} if the player's class state exists, or an
     *         empty {@code Optional} if no class state is found
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    private Optional<PlayerClassState> loadPlayerState(UUID playerId) {
        var state = repository.findPlayerState(playerId);

        state.flatMap(playerState -> classRegistry.get(playerState.classId()))
            .ifPresent(classDefinition -> restrictionCache.setClass(playerId, classDefinition));

        return state;
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
}
