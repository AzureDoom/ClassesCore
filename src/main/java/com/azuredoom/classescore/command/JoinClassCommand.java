package com.azuredoom.classescore.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassIdArgumentType;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.PlayerClassContextManager;

public final class JoinClassCommand extends AbstractAsyncCommand {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;

    @Nonnull
    private final RequiredArg<ClassDefinition> classArg;

    public JoinClassCommand() {
        super("join", "Join a class");

        this.playerArg = this.withRequiredArg(
            "player",
            "Player to join class.",
            ArgTypes.PLAYER_REF
        );

        this.classArg = this.withRequiredArg(
            "classId",
            "Class to select.",
            ClassIdArgumentType.INSTANCE
        );
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext) {
        var playerRef = this.playerArg.get(commandContext);
        var playerUuid = playerRef.getUuid();
        var definition = this.classArg.get(commandContext);

        if (
            ClassesCore.getClassService()
                .getSelectedClassDefinition(playerUuid)
                .isPresent()
        ) {
            commandContext.sendMessage(BaseLangMessages.ALREADY_HAS_CLASS);
            return CompletableFuture.completedFuture(null);
        }

        var context = PlayerClassContextManager.getContext(playerUuid);

        if (context == null || context.entityRef() == null || !context.entityRef().isValid()) {
            commandContext.sendMessage(Message.raw("Could not find active player context."));
            return CompletableFuture.completedFuture(null);
        }

        var future = new CompletableFuture<Void>();

        context.world().execute(() -> {
            try {
                ClassesCore.getClassService().selectClass(playerUuid, definition.id());

                var message = BaseLangMessages.JOINED_CLASS.param(
                    "className",
                    definition.displayName()
                );

                commandContext.sendMessage(message);

                future.complete(null);
            } catch (Exception e) {
                ClassesCore.LOGGER.atWarning()
                    .withCause(e)
                    .log("Failed to select class for player {}", playerUuid);

                commandContext.sendMessage(Message.raw("Failed to join class. Check the server log."));

                future.complete(null);
            }
        });

        return future;
    }
}
