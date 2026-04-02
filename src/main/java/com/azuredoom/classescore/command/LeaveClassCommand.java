package com.azuredoom.classescore.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.lang.BaseLangMessages;

public class LeaveClassCommand extends AbstractPlayerCommand {

    @Nonnull
    private final OptionalArg<PlayerRef> playerArg;

    public LeaveClassCommand() {
        super("leave", "Leave a class");
        this.requirePermission("classescore.leaveclass");
        this.playerArg = this.withOptionalArg(
            "player",
            "Player to leave class.",
            ArgTypes.PLAYER_REF
        );
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
        if (
            ClassesCore.getClassService().getSelectedClassDefinition(playerRef.getUuid()).isEmpty()
        ) {
            playerRef.sendMessage(BaseLangMessages.NO_CLASS_SELECTED);
            return;
        }
        if (this.playerArg.get(commandContext) != null)
            playerRef = this.playerArg.get(commandContext);
        var classId = ClassesCore.getClassService()
            .getPlayerState(playerRef.getUuid())
            .map(PlayerClassState::classId)
            .orElseThrow();

        var definition = ClassesCore.getClassRegistry().get(classId);
        if (definition.isEmpty()) {
            playerRef.sendMessage(BaseLangMessages.UNKNOWN_CLASS.param("classId", classId));
            return;
        }

        ClassesCore.getClassService().clearClass(playerRef.getUuid(), classId);
        playerRef.sendMessage(BaseLangMessages.LEFT_CLASS.param("className", definition.get().displayName()));
    }
}
