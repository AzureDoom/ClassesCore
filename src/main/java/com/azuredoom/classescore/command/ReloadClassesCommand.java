package com.azuredoom.classescore.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.TranslationUtil;

public final class ReloadClassesCommand extends AbstractAsyncCommand {

    private final ClassesCore plugin;

    public ReloadClassesCommand(ClassesCore plugin) {
        super("reload", "Reloads all available classes");
        this.plugin = plugin;
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext) {
        try {
            this.plugin.reloadClasses();

            commandContext.sendMessage(
                TranslationUtil.translate(
                    BaseLangMessages.RELOAD_SUCCESS,
                    msg -> msg.param("total", ClassesCore.getClassRegistry().all().size())
                )
            );
        } catch (Exception e) {
            commandContext.sendMessage(
                TranslationUtil.translate(BaseLangMessages.RELOAD_FAILED)
            );

            ClassesCore.LOGGER.atWarning()
                .withCause(e)
                .log("Failed to reload classes");
        }

        return CompletableFuture.completedFuture(null);
    }
}
