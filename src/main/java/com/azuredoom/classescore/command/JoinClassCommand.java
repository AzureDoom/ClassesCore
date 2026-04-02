package com.azuredoom.classescore.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;

public final class JoinClassCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;

    private final RequiredArg<String> classIdArg;

    public JoinClassCommand() {
        super("join", "Join a class");
        this.requirePermission("classescore.joinclass");
        this.playerArg = this.withRequiredArg(
            "player",
            "Player to join class.",
            ArgTypes.PLAYER_REF
        );
        this.classIdArg = this.withRequiredArg("classId", "Class id to select", ArgTypes.STRING);
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
        var service = ClassesCore.getClassService();

        if (
            service == null ||
                service.getSelectedClassDefinition(playerRef.getUuid()).isPresent()
        ) {
            playerRef.sendMessage(BaseLangMessages.ALREADY_HAS_CLASS);
            return;
        }
        playerRef = this.playerArg.get(commandContext);
        var classId = classIdArg.get(commandContext);

        var definition = ClassesCore.getClassRegistry().get(classId);
        if (definition.isEmpty()) {
            playerRef.sendMessage(BaseLangMessages.UNKNOWN_CLASS.param("classId", classId));
            return;
        }

        ClassesCore.getClassService().selectClass(playerRef.getUuid(), classId);
        playerRef.sendMessage(BaseLangMessages.JOINED_CLASS.param("className", definition.get().displayName()));
    }
}
