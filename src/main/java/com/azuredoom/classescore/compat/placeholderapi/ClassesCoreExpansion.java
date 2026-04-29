package com.azuredoom.classescore.compat.placeholderapi;

import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.azuredoom.classescore.data.ClassDefinition;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;

public class ClassesCoreExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "classescore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AzureDoom";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(@NotNull PlayerRef playerRef, @NotNull String params) {
        if (params.isBlank()) {
            return null;
        }

        var classesService = ClassesCore.getClassService();
        var classRegistry = ClassesCore.getClassRegistry();

        if (classesService == null || classRegistry == null) {
            return null;
        }

        var state = classesService.getPlayerState(playerRef.getUuid()).orElse(null);

        if (state == null) {
            return switch (params.toLowerCase()) {
                case "class", "class_id", "class_name" -> "";
                case "has_class" -> "false";
                default -> null;
            };
        }

        return switch (params.toLowerCase()) {
            case "class", "class_id" -> state.classId();

            case "class_name" -> classesService.getSelectedClassDefinition(playerRef.getUuid())
                    .map(ClassDefinition::displayName)
                    .orElse(state.classId());

            default -> null;
        };
    }
}
