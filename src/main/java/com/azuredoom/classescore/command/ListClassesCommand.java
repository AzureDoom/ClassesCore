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
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.lang.BaseLangMessages;

public class ListClassesCommand extends AbstractPlayerCommand {

    public ListClassesCommand() {
        super("list", "Lists all available classes");
        this.requirePermission("classescore.listclasses");
        this.setPermissionGroup(GameMode.Creative);
    }

    @Override
    protected void execute(
        @NotNull CommandContext commandContext,
        @NotNull Store<EntityStore> store,
        @NotNull Ref<EntityStore> ref,
        @NotNull PlayerRef playerRef,
        @NotNull World world
    ) {
        playerRef.sendMessage(
            BaseLangMessages.CLASS_LIST.param(
                "classlist",
                ClassesCore.getClassRegistry()
                    .all()
                    .stream()
                    .map(ClassDefinition::id)
                    .toList()
                    .stream()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
            )
        );
    }
}
