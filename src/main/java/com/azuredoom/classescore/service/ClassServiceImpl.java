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

    public Optional<PlayerClassState> getPlayerState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return playerStateCache.computeIfAbsent(playerId, this::loadPlayerState);
    }

    public Optional<ClassDefinition> getSelectedClassDefinition(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        return getPlayerState(playerId)
            .flatMap(state -> classRegistry.get(state.classId()));
    }

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

    public boolean hasSelectedClass(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return getPlayerState(playerId).isPresent();
    }

    public boolean isWeaponAllowed(UUID playerId, String weaponId) {
        Objects.requireNonNull(playerId, "playerId");

        if (weaponId == null || weaponId.isBlank()) {
            return false;
        }

        return getSelectedClassDefinition(playerId)
            .map(definition -> definition.equipmentRules().isWeaponAllowed(weaponId))
            .orElse(true);
    }

    public boolean isArmorAllowed(UUID playerId, String armorId) {
        Objects.requireNonNull(playerId, "playerId");

        if (armorId == null || armorId.isBlank()) {
            return false;
        }

        return getSelectedClassDefinition(playerId)
            .map(definition -> definition.equipmentRules().isArmorAllowed(armorId))
            .orElse(true);
    }

    private Optional<PlayerClassState> loadPlayerState(UUID playerId) {
        var state = repository.findPlayerState(playerId);

        state.flatMap(playerState -> classRegistry.get(playerState.classId()))
            .ifPresent(classDefinition -> restrictionCache.setClass(playerId, classDefinition));

        return state;
    }

    public void evictPlayer(UUID playerId) {
        playerStateCache.remove(playerId);
        restrictionCache.clear(playerId);
    }
}
