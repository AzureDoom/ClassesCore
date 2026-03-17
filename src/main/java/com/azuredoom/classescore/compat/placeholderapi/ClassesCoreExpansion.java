package com.azuredoom.classescore.compat.placeholderapi;

import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.azuredoom.classescore.api.ClassesCoreAPI;
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
    public @Nullable String onPlaceholderRequest(PlayerRef playerRef, @NotNull String params) {
        var classesService = ClassesCoreAPI.getClassServiceIfPresent().orElse(null);
        var classRegistry = ClassesCoreAPI.getClassRegistryIfPresent().orElse(null);
        if (classesService == null || classRegistry == null) {
            return null;
        }
        if (params.equalsIgnoreCase("class")) {
            return classesService.getPlayerState(playerRef.getUuid()).map(PlayerClassState::classId).orElse(null);
        }
        return null;
    }
}
