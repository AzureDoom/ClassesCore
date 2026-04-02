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

/**
 * A utility class providing core API operations for managing and interacting with classes, class definitions, and
 * player class states in the system. This class acts as an intermediary between the underlying services and the
 * external modules.
 */
public final class ClassesCoreAPI {

    private ClassesCoreAPI() {}

    /**
     * Retrieves an instance of the {@link ClassServiceImpl} if present within the system.
     *
     * @return an {@code Optional} containing the {@code ClassServiceImpl} instance if available, or an empty
     *         {@code Optional} if the service is not present.
     */
    public static Optional<ClassServiceImpl> getClassServiceIfPresent() {
        return ClassesCore.getClassServiceIfPresent();
    }

    /**
     * Retrieves an instance of {@link ClassRegistry} if it is available in the system.
     *
     * @return an {@code Optional} containing the {@code ClassRegistry} instance if it is present, or an empty
     *         {@code Optional} if the registry is not available.
     */
    public static Optional<ClassRegistry> getClassRegistryIfPresent() {
        return ClassesCore.getClassRegistryIfPresent();
    }

    /**
     * Retrieves all available class definitions registered in the system. If no class registry is present, returns an
     * empty collection.
     *
     * @return a collection of {@code ClassDefinition} objects representing all registered classes, or an empty
     *         collection if no registry is available.
     */
    public static Collection<ClassDefinition> getClasses() {
        return getClassRegistryIfPresent()
            .map(ClassRegistry::all)
            .orElse(List.of());
    }

    /**
     * Retrieves the {@code ClassDefinition} associated with the specified class identifier.
     * <p>
     * The method attempts to fetch the {@link ClassDefinition} from the class registry if it is present. If no registry
     * is available, or if the specified class identifier does not exist in the registry, the method returns an empty
     * {@code Optional}.
     *
     * @param classId the unique identifier of the class whose definition is to be retrieved
     * @return an {@code Optional} containing the {@code ClassDefinition} for the given identifier, or an empty
     *         {@code Optional} if the class definition is not found or the registry is unavailable
     */
    public static Optional<ClassDefinition> getClassDefinition(String classId) {
        return getClassRegistryIfPresent().flatMap(registry -> registry.get(classId));
    }

    /**
     * This method determines if the specified player has a class assigned by checking the player's selected class ID
     * and verifying if it corresponds to a valid class definition in the system.
     *
     * @param playerId the unique identifier of the player whose class status is to be checked
     * @return {@code true} if the player has a selected class, and it exists in the class registry, {@code false}
     *         otherwise
     */
    public static boolean playerHasClass(UUID playerId) {
        return hasClass(getSelectedClassId(playerId).orElse(null));
    }

    /**
     * Checks if a class with the specified identifier exists in the class registry. The method verifies the
     * availability of a {@code ClassRegistry}, and if present, attempts to retrieve the class with the given
     * identifier.
     *
     * @param classId the unique identifier of the class to check
     * @return {@code true} if the class exists in the registry, {@code false} otherwise
     */
    public static boolean hasClass(String classId) {
        return getClassRegistryIfPresent().flatMap(registry -> registry.get(classId)).isPresent();
    }

    /**
     * Retrieves the current class state of a specific player. The method attempts to get an instance of the
     * {@code ClassServiceImpl} and, if available, fetches the {@code PlayerClassState} associated with the given
     * player.
     *
     * @param playerId the unique identifier of the player whose class state is to be retrieved
     * @return an {@code Optional} containing the {@code PlayerClassState} if the player has a class state assigned, or
     *         an empty {@code Optional} if no such state exists or the service is unavailable
     */
    public static Optional<PlayerClassState> getPlayerState(UUID playerId) {
        return getClassServiceIfPresent().flatMap(service -> service.getPlayerState(playerId));
    }

    /**
     * Retrieves the selected class ID associated with a specific player. This method fetches the player's current class
     * state and extracts the class ID if a class state is present.
     *
     * @param playerId the unique identifier of the player whose selected class ID is to be retrieved
     * @return an {@code Optional} containing the selected class ID if the player has a class state assigned, or an
     *         empty {@code Optional} if no class state exists or the service is unavailable
     */
    public static Optional<String> getSelectedClassId(UUID playerId) {
        return getPlayerState(playerId).map(PlayerClassState::classId);
    }

    /**
     * Retrieves the {@code ClassDefinition} associated with the given player's currently selected class. The method
     * fetches the player's class state, extracts the class ID if available, and then attempts to retrieve the
     * corresponding {@code ClassDefinition} from the class registry.
     *
     * @param playerId the unique identifier of the player whose selected class is to be retrieved
     * @return an {@code Optional} containing the {@code ClassDefinition} for the player's selected class, or an empty
     *         {@code Optional} if the player has no selected class or the class definition is not found in the registry
     */
    public static Optional<ClassDefinition> getSelectedClass(UUID playerId) {
        return getPlayerState(playerId)
            .map(PlayerClassState::classId)
            .flatMap(ClassesCoreAPI::getClassDefinition);
    }

    public static void selectClass(UUID playerId, String classId) {
        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }

        if (!hasClass(classId)) {
            throw new IllegalArgumentException("Unknown class id: " + classId);
        }

        var service = getClassServiceIfPresent()
            .orElseThrow(() -> new IllegalStateException("Class service unavailable"));

        service.selectClass(playerId, classId);
    }

    /**
     * Clears the class associated with a specific player and class ID.
     * <p>
     * This method interacts with the {@code ClassServiceImpl} to remove the association of a player with a class,
     * effectively clearing the player's current class state. If the service is unavailable, the operation will fail.
     *
     * @param playerId the unique identifier of the player whose class is to be cleared
     * @param classId  the unique identifier of the class to be cleared
     */
    public static void clearClass(UUID playerId, String classId) {
        var service = getClassServiceIfPresent().orElse(null);
        if (service == null) {
            return;
        }

        service.clearClass(playerId, classId);
    }

    /**
     * Determines whether a player is allowed to use a specific weapon based on their currently selected class. The
     * method retrieves the player's selected class definition and checks if the weapon is allowed according to the
     * equipment rules of that class. If the player has no selected class or the class definition is unavailable, the
     * method defaults to allowing the weapon.
     *
     * @param playerId the unique identifier of the player whose weapon usage is to be verified
     * @param weaponId the unique identifier of the weapon to check for usage allowance
     * @return {@code true} if the weapon is allowed for the player's selected class, or if no class is selected;
     *         {@code false} otherwise
     */
    public static boolean canUseWeapon(UUID playerId, String weaponId) {
        return getSelectedClass(playerId)
            .map(classDef -> classDef.equipmentRules().isWeaponAllowed(weaponId))
            .orElse(true);
    }

    /**
     * Determines whether a player is allowed to use a specific armor based on their currently selected class. The
     * method retrieves the player's selected class definition and verifies if the armor is permitted according to the
     * equipment rules of that class. If the player has no selected class, the method defaults to allowing the armor.
     *
     * @param playerId the unique identifier of the player whose armor usage is being verified
     * @param armorId  the unique identifier of the armor to check for usage allowance
     * @return {@code true} if the armor is allowed for the player's selected class, or if no class is selected;
     *         {@code false} otherwise
     */
    public static boolean canUseArmor(UUID playerId, String armorId) {
        return getSelectedClass(playerId)
            .map(classDef -> classDef.equipmentRules().isArmorAllowed(armorId))
            .orElse(true);
    }

}
