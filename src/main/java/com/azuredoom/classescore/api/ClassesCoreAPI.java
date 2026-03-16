package com.azuredoom.classescore.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.service.ClassServiceImpl;

public final class ClassesCoreAPI {

    private ClassesCoreAPI() {}

    public static Optional<ClassServiceImpl> getClassServiceIfPresent() {
        return Optional.ofNullable(ClassesCore.getClassService());
    }

    public static Optional<ClassRegistry> getClassRegistryIfPresent() {
        return Optional.ofNullable(ClassesCore.getClassRegistry());
    }

    public static Collection<ClassDefinition> getClasses() {
        return getClassRegistryIfPresent()
            .map(ClassRegistry::all)
            .orElse(List.of());
    }

    public static Optional<ClassDefinition> getClassDefinition(String classId) {
        return getClassRegistryIfPresent().flatMap(registry -> registry.get(classId));
    }

    public static boolean playerHasClass(UUID playerId) {
        return hasClass(getSelectedClassId(playerId).orElse(null));
    }

    public static boolean hasClass(String classId) {
        return getClassRegistryIfPresent().flatMap(registry -> registry.get(classId)).isPresent();
    }

    public static Optional<PlayerClassState> getPlayerState(UUID playerId) {
        return getClassServiceIfPresent().flatMap(service -> service.getPlayerState(playerId));
    }

    public static Optional<String> getSelectedClassId(UUID playerId) {
        return getPlayerState(playerId).map(PlayerClassState::classId);
    }

    public static Optional<ClassDefinition> getSelectedClass(UUID playerId) {
        return getPlayerState(playerId)
            .map(PlayerClassState::classId)
            .flatMap(ClassesCoreAPI::getClassDefinition);
    }

    public static boolean selectClass(UUID playerId, String classId) {
        if (classId == null || classId.isBlank()) {
            return false;
        }
        if (!hasClass(classId)) {
            return false;
        }

        var service = getClassServiceIfPresent().orElse(null);
        if (service == null) {
            return false;
        }

        service.selectClass(playerId, classId);
        return true;
    }

    public static boolean clearClass(UUID playerId, String classId) {
        var service = getClassServiceIfPresent().orElse(null);
        if (service == null) {
            return false;
        }

        service.clearClass(playerId, classId);
        return true;
    }

    public static boolean canUseWeapon(UUID playerId, String weaponId) {
        return getSelectedClass(playerId)
            .map(classDef -> classDef.equipmentRules().isWeaponAllowed(weaponId))
            .orElse(true);
    }

    public static boolean canUseArmor(UUID playerId, String armorId) {
        return getSelectedClass(playerId)
            .map(classDef -> classDef.equipmentRules().isArmorAllowed(armorId))
            .orElse(true);
    }

}
