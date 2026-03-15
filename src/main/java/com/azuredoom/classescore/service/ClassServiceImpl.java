package com.azuredoom.classescore.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.db.JdbcClassesRepository;

public final class ClassServiceImpl implements ClassService {

    private final JdbcClassesRepository repository;

    private final ClassRegistry classRegistry;

    public ClassServiceImpl(JdbcClassesRepository repository, ClassRegistry classRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.classRegistry = Objects.requireNonNull(classRegistry, "classRegistry");
    }

    @Override
    public Optional<PlayerClassState> getPlayerState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return repository.findPlayerState(playerId);
    }

    @Override
    public Optional<ClassDefinition> getSelectedClassDefinition(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        return repository.findPlayerState(playerId)
            .flatMap(state -> classRegistry.get(state.classId()));
    }

    @Override
    public void selectClass(UUID playerId, String classId) {
        Objects.requireNonNull(playerId, "playerId");

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }

        var definition = classRegistry.get(classId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + classId));

        var now = System.currentTimeMillis();

        PlayerClassState state = repository.findPlayerState(playerId)
            .map(
                existing -> new PlayerClassState(
                    existing.playerId(),
                    definition.getId(),
                    existing.createdAt(),
                    now
                )
            )
            .orElseGet(
                () -> new PlayerClassState(
                    playerId,
                    definition.getId(),
                    now,
                    now
                )
            );

        repository.savePlayerState(state);
    }

    @Override
    public void clearClass(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        repository.deletePlayerState(playerId);
    }

    @Override
    public boolean hasSelectedClass(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return repository.findPlayerState(playerId).isPresent();
    }

    @Override
    public boolean isWeaponAllowed(UUID playerId, String weaponId) {
        Objects.requireNonNull(playerId, "playerId");

        if (weaponId == null || weaponId.isBlank()) {
            return false;
        }

        return getSelectedClassDefinition(playerId)
            .map(definition -> definition.getEquipmentRules().isWeaponAllowed(weaponId))
            .orElse(true);
    }
}
