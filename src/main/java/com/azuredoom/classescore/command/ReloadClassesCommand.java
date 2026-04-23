package com.azuredoom.classescore.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.TranslationUtil;

public class ReloadClassesCommand extends AbstractPlayerCommand {

    private final ClassesCore plugin;

    public ReloadClassesCommand(ClassesCore plugin) {
        super("reload", "Reloads all available classes");
        this.requirePermission("classescore.reloadclasses");
        this.setPermissionGroup(GameMode.Creative);
        this.plugin = plugin;
    }

    @Override
    protected void execute(
        @NotNull CommandContext commandContext,
        @NotNull Store<EntityStore> store,
        @NotNull Ref<EntityStore> ref,
        @NotNull PlayerRef playerRef,
        @NotNull World world
    ) {
        try {
            this.plugin.reloadClasses();
            commandContext.sendMessage(
                TranslationUtil.translate(
                    BaseLangMessages.RELOAD_SUCCESS,
                    msg -> msg.param("total", ClassesCore.getClassRegistry().all().size())
                )
            );
        } catch (Exception e) {
            commandContext.sendMessage(TranslationUtil.translate(BaseLangMessages.RELOAD_FAILED));
            ClassesCore.LOGGER.atWarning().log("Failed to reload classes", e);
        }
    }
}
