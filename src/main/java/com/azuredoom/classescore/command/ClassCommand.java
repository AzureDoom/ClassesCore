package com.azuredoom.classescore.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
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
import com.azuredoom.classescore.api.ClassesCoreAPI;

public final class ClassCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;

    private final RequiredArg<String> classIdArg;

    public ClassCommand() {
        super("class", "classescore.commands.class.desc");
        this.requirePermission("classescore.class");
        this.playerArg = this.withRequiredArg(
            "player",
            "Player to join class.",
            ArgTypes.PLAYER_REF
        );
        this.classIdArg = this.withRequiredArg("classId", "Class id to select", ArgTypes.STRING);
    }

    /*
     * TODO: Update Message.raw to translated messages instead
     */
    @Override
    protected void execute(
        @NotNull CommandContext commandContext,
        @NotNull Store<EntityStore> store,
        @NotNull Ref<EntityStore> ref,
        @NotNull PlayerRef playerRef,
        @NotNull World world
    ) {
        if (
            ClassesCoreAPI.playerHasClass(playerRef.getUuid()) || ClassesCoreAPI.hasClass(
                classIdArg.get(commandContext)
            )
        ) {
            return;
        }
        playerRef = this.playerArg.get(commandContext);
        var classId = classIdArg.get(commandContext);

        var definition = ClassesCore.getClassRegistry().get(classId);
        if (definition.isEmpty()) {
            playerRef.sendMessage(Message.raw("Unknown class: " + classId));
            return;
        }

        ClassesCoreAPI.selectClass(playerRef.getUuid(), classId);
        playerRef.sendMessage(Message.raw("Joined selected class: " + definition.get().displayName()));
    }
}
