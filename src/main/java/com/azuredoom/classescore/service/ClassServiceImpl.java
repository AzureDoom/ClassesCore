package com.azuredoom.classescore.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;

public final class ClassServiceImpl {

    private final JdbcClassesRepository repository;

    private final ClassRegistry classRegistry;

    private final PlayerRestrictionCache restrictionCache;

    private final Map<UUID, Optional<PlayerClassState>> playerStateCache = new ConcurrentHashMap<>();

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

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }

        var definition = classRegistry.get(classId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + classId));

        var now = System.currentTimeMillis();

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

    public void clearClass(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        repository.deletePlayerState(playerId);
        playerStateCache.put(playerId, Optional.empty());
        restrictionCache.clear(playerId);
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
