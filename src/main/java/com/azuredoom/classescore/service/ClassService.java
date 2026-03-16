package com.azuredoom.classescore.service;

import java.util.Optional;
import java.util.UUID;

import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;

public interface ClassService {

    Optional<PlayerClassState> getPlayerState(UUID playerId);

    Optional<ClassDefinition> getSelectedClassDefinition(UUID playerId);

    void selectClass(UUID playerId, String classId);

    void clearClass(UUID playerId);

    boolean hasSelectedClass(UUID playerId);

    boolean isWeaponAllowed(UUID playerId, String weaponId);

    boolean isArmorAllowed(UUID playerId, String armorId);
}
