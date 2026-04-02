package com.azuredoom.classescore.command;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.ui.ClassSelectionPage;
import com.azuredoom.classescore.ui.ClassSelectionPageUI;

public final class ClassSelectionCommand extends AbstractPlayerCommand {

    public ClassSelectionCommand() {
        super("class", "opens UI to select class");
        this.requirePermission("classescore.class");
        this.setPermissionGroup(GameMode.Adventure);
        this.addSubCommand(new JoinClassCommand());
        this.addSubCommand(new LeaveClassCommand());
        this.addSubCommand(new ListClassesCommand());
    }

    @Override
    protected void execute(
        @NotNull CommandContext commandContext,
        @NotNull Store<EntityStore> store,
        @NotNull Ref<EntityStore> ref,
        @NotNull PlayerRef playerRef,
        @NotNull World world
    ) {
        if (
            ClassesCore.getClassService().getSelectedClassDefinition(playerRef.getUuid()).isPresent()
        ) {
            playerRef.sendMessage(BaseLangMessages.ALREADY_HAS_CLASS);
            return;
        }
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        if (player.getPageManager().getCustomPage() == null) {
            if (PluginManager.get().getPlugin(new PluginIdentifier("Ellie", "HyUI")) != null) {
                var page = new ClassSelectionPage();
                page.open(ref, store);
            } else {
                var page = new ClassSelectionPageUI(playerRef);
                player.getPageManager().openCustomPage(ref, store, page);
            }
        }
    }
}
