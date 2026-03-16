package com.azuredoom.classescore;

import com.azuredoom.classescore.gameplay.services.damage.ClassDamageSystem;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.util.Config;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

import com.azuredoom.classescore.api.ClassesCoreAPI;
import com.azuredoom.classescore.bootstrap.ClassesBootstrap;
import com.azuredoom.classescore.command.ClassCommand;
import com.azuredoom.classescore.command.LeaveClassCommand;
import com.azuredoom.classescore.compat.DynamicTooltipsLibCompat;
import com.azuredoom.classescore.config.ClassesCoreConfig;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.exceptions.ClassesCoreException;
import com.azuredoom.classescore.gameplay.services.armor.EquipBlockManager;
import com.azuredoom.classescore.gameplay.services.items.HandGateTickingSystem;
import com.azuredoom.classescore.gameplay.services.items.ItemBlockPacketManager;
import com.azuredoom.classescore.gameplay.services.items.PlayerRestrictionCache;
import com.azuredoom.classescore.service.ClassServiceImpl;

public class ClassesCore extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ClassesCore INSTANCE;

    private static Config<ClassesCoreConfig> config = null;

    private static ClassServiceImpl classService;

    private static ClassRegistry classRegistry;

    private static final PlayerRestrictionCache PLAYER_RESTRICTION_CACHE = new PlayerRestrictionCache();

    public static final ItemBlockPacketManager ITEM_BLOCK_PACKET_MANAGER = new ItemBlockPacketManager(
        PLAYER_RESTRICTION_CACHE
    );

    public static final EquipBlockManager equipBlockManager = new EquipBlockManager(PLAYER_RESTRICTION_CACHE);

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

        this.getCommandRegistry().registerCommand(new ClassCommand());
        this.getCommandRegistry().registerCommand(new LeaveClassCommand());

        if (config.get().isEnableClassItemRestrictions()) {
            ITEM_BLOCK_PACKET_MANAGER.start();
            this.registerAllSystems();
            this.getEventRegistry()
                .registerGlobal(PlayerReadyEvent.class, (event) -> {
                    var playerId = event.getPlayer().getUuid();
                    ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> {
                        if (service.hasSelectedClass(playerId)) {
                            service.getSelectedClassDefinition(playerId)
                                .ifPresentOrElse(
                                    classDef -> PLAYER_RESTRICTION_CACHE.setClass(playerId, classDef),
                                    () -> PLAYER_RESTRICTION_CACHE.clear(playerId)
                                );
                        } else {
                            PLAYER_RESTRICTION_CACHE.clear(playerId);
                        }
                        equipBlockManager.validateArmorOnReady(event.getPlayer());
                    });
                });
            this.getEventRegistry()
                .registerGlobal(PlayerDisconnectEvent.class, (event) -> {
                    var playerId = event.getPlayerRef().getUuid();
                    ClassesCore.getPlayerRestrictionCache().clear(playerId);
                    ITEM_BLOCK_PACKET_MANAGER.getHandCheckState().remove(playerId);
                    ITEM_BLOCK_PACKET_MANAGER.clearPlayer(playerId);
                    PLAYER_RESTRICTION_CACHE.clear(playerId);
                    ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> service.evictPlayer(playerId));
                });
            ClassesCore.equipBlockManager.start();
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
        LOGGER.at(Level.INFO).log("Starting classescore!");
    }

    @Override
    protected void shutdown() {
        if (config.get().isEnableClassItemRestrictions()) {
            ITEM_BLOCK_PACKET_MANAGER.shutdown();
            equipBlockManager.shutdown();
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
            new HandGateTickingSystem(ITEM_BLOCK_PACKET_MANAGER.getHandCheckState(), PLAYER_RESTRICTION_CACHE)
        );
        getEntityStoreRegistry().registerSystem(new ClassDamageSystem());
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
        return PLAYER_RESTRICTION_CACHE;
    }

    public static ClassesCore getInstance() {
        return INSTANCE;
    }
}
