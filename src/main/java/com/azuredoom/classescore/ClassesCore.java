package com.azuredoom.classescore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class ClassesCore extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ClassesCore(@NotNull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting classescore!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up classescore!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down classescore!");
    }
}
