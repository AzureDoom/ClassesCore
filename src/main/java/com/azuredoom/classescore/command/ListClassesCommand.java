package com.azuredoom.classescore.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.lang.BaseLangMessages;

public class ListClassesCommand extends AbstractAsyncCommand {

    public ListClassesCommand() {
        super("list", "Lists all available classes");
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext) {
        commandContext.sendMessage(
            BaseLangMessages.CLASS_LIST.param(
                "classlist",
                ClassesCore.getClassRegistry()
                    .all()
                    .stream()
                    .map(ClassDefinition::id)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
            )
        );

        return CompletableFuture.completedFuture(null);
    }
}
