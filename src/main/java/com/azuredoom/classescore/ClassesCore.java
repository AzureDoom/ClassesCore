package com.azuredoom.classescore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

import com.azuredoom.classescore.config.ClassesConfig;

public class ClassesCore extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Config<ClassesConfig> config = null;

    public ClassesCore(@NotNull JavaPluginInit init) {
        super(init);
        config = this.withConfig("classescore", ClassesConfig.CODEC);
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up classescore!");
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting classescore!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down classescore!");
    }
}
