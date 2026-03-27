package com.azuredoom.classescore;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

import com.azuredoom.classescore.api.ClassesCoreAPI;
import com.azuredoom.classescore.bootstrap.ClassesBootstrap;
import com.azuredoom.classescore.command.JoinClassCommand;
import com.azuredoom.classescore.command.LeaveClassCommand;
import com.azuredoom.classescore.compat.DynamicTooltipsLibCompat;
import com.azuredoom.classescore.compat.placeholderapi.PlaceholderAPICompat;
import com.azuredoom.classescore.config.ClassesCoreConfig;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.exceptions.ClassesCoreException;
import com.azuredoom.classescore.gameplay.services.armor.ArmorBlockClassSystem;
import com.azuredoom.classescore.gameplay.services.damage.ClassDamageSystem;
import com.azuredoom.classescore.gameplay.services.items.HandGateTickingSystem;
import com.azuredoom.classescore.gameplay.services.items.ItemBlockPacketManager;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
import com.azuredoom.classescore.gameplay.services.stats.StatsTickingSystem;
import com.azuredoom.classescore.service.ClassServiceImpl;

public class ClassesCore extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ClassesCore INSTANCE;

    private static Config<ClassesCoreConfig> config = null;

    private static ClassServiceImpl classService;

    private static ClassRegistry classRegistry;

    private static final PlayerRestrictionCache playerRestrictionCache = new PlayerRestrictionCache();

    public static final ItemBlockPacketManager itemBlockPacketManager = new ItemBlockPacketManager(
        playerRestrictionCache
    );

    public static final ArmorBlockClassSystem equipBlockManager = new ArmorBlockClassSystem(playerRestrictionCache);

    public ClassesCore(@NotNull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
        config = this.withConfig("classescore", ClassesCoreConfig.CODEC);
    }

    @Override
    protected void setup() {
        INSTANCE = this;
        config.save();

        var bootstrap = new ClassesBootstrap(this, config.get()).bootstrap();

        classService = bootstrap.service();
        classRegistry = bootstrap.registry();

        this.getCommandRegistry().registerCommand(new JoinClassCommand());
        this.getCommandRegistry().registerCommand(new LeaveClassCommand());

        if (config.get().isEnableClassItemRestrictions()) {
            itemBlockPacketManager.start();
            this.registerAllSystems();
            this.getEventRegistry()
                .registerGlobal(PlayerReadyEvent.class, (event) -> {
                    var player = event.getPlayer();
                    ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> {
                        var playerRef = player.getReference();
                        if (playerRef == null) {
                            LOGGER.at(Level.WARNING).log("Player reference is null");
                            return;
                        }
                        var playerRefComponent = playerRef.getStore()
                            .getComponent(playerRef, PlayerRef.getComponentType());
                        if (playerRefComponent == null) {
                            LOGGER.at(Level.WARNING).log("Player ref component is null");
                            return;
                        }
                        var playerId = playerRefComponent.getUuid();
                        if (service.hasSelectedClass(playerId)) {
                            service.getSelectedClassDefinition(playerId)
                                .ifPresentOrElse(
                                    classDef -> playerRestrictionCache.setClass(playerId, classDef),
                                    () -> playerRestrictionCache.clear(playerId)
                                );
                        } else {
                            playerRestrictionCache.clear(playerId);
                        }
                        equipBlockManager.validateArmorOnReady(event.getPlayer());
                    });
                });
            this.getEventRegistry()
                .registerGlobal(PlayerDisconnectEvent.class, (event) -> {
                    var playerId = event.getPlayerRef().getUuid();
                    ClassesCore.getPlayerRestrictionCache().clear(playerId);
                    itemBlockPacketManager.getHandCheckState().remove(playerId);
                    itemBlockPacketManager.clearPlayer(playerId);
                    playerRestrictionCache.clear(playerId);
                    ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> service.evictPlayer(playerId));
                });
        }
        if (PluginManager.get().getPlugin(new PluginIdentifier("org.herolias", "DynamicTooltipsLib")) != null) {
            DynamicTooltipsLibCompat.register();
        }
        LOGGER.at(Level.INFO)
            .log("ClassesCore setup complete. Loaded " + classRegistry.all().size() + " classes.");
        LOGGER.at(Level.INFO)
            .log(
                "ClassesCore loaded the following classes: " + classRegistry.all()
                    .stream()
                    .map(ClassDefinition::id)
                    .toList()
            );
    }

    @Override
    protected void start() {
        if (PluginManager.get().getPlugin(new PluginIdentifier("HelpChat", "PlaceholderAPI")) != null) {
            PlaceholderAPICompat.register();
        }
    }

    @Override
    protected void shutdown() {
        if (config.get().isEnableClassItemRestrictions()) {
            itemBlockPacketManager.shutdown();
        }
        var bootstrap = new ClassesBootstrap(this, config.get()).bootstrap();
        try {
            bootstrap.closeable().close();
        } catch (Exception e) {
            throw new ClassesCoreException("Failed to close resources", e);
        }
    }

    public void registerAllSystems() {
        getEntityStoreRegistry().registerSystem(
            new HandGateTickingSystem(itemBlockPacketManager.getHandCheckState(), playerRestrictionCache)
        );
        getEntityStoreRegistry().registerSystem(equipBlockManager);
        getEntityStoreRegistry().registerSystem(new ClassDamageSystem());
        getEntityStoreRegistry().registerSystem(new StatsTickingSystem());
    }

    public static Config<ClassesCoreConfig> getConfig() {
        return config;
    }

    public static ClassServiceImpl getClassService() {
        return classService;
    }

    public static ClassRegistry getClassRegistry() {
        return classRegistry;
    }

    public static PlayerRestrictionCache getPlayerRestrictionCache() {
        return playerRestrictionCache;
    }

    public static ClassesCore getInstance() {
        return INSTANCE;
    }
}
