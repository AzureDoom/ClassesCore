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
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.PlayerClassContextManager;
import com.azuredoom.classescore.util.TranslationUtil;

public final class LeaveClassCommand extends AbstractAsyncCommand {

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg;

    public LeaveClassCommand() {
        super("leave", "Leave a class");

        this.playerArg = this.withRequiredArg(
            "player",
            "Player to remove from their class.",
            ArgTypes.PLAYER_REF
        );
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext) {
        var playerRef = this.playerArg.get(commandContext);
        var playerUuid = playerRef.getUuid();

        if (
            ClassesCore.getClassService()
                .getSelectedClassDefinition(playerUuid)
                .isEmpty()
        ) {
            commandContext.sendMessage(BaseLangMessages.NO_CLASS_SELECTED);
            return CompletableFuture.completedFuture(null);
        }

        var classId = ClassesCore.getClassService()
            .getPlayerState(playerUuid)
            .map(PlayerClassState::classId)
            .orElseThrow();

        var definition = ClassesCore.getClassRegistry().get(classId);

        if (definition.isEmpty()) {
            var message = TranslationUtil.translate(
                BaseLangMessages.UNKNOWN_CLASS,
                msg -> msg.param("classId", classId)
            );

            commandContext.sendMessage(message);

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
                ClassesCore.getClassService().clearClass(playerUuid, classId);

                var message = BaseLangMessages.LEFT_CLASS.param(
                    "className",
                    definition.get().displayName()
                );

                commandContext.sendMessage(message);

                future.complete(null);
            } catch (Exception e) {
                ClassesCore.LOGGER.atWarning()
                    .withCause(e)
                    .log("Failed to clear class for player {}", playerUuid);

                commandContext.sendMessage(Message.raw("Failed to leave class. Check the server log."));

                future.complete(null);
            }
        });

        return future;
    }
}
