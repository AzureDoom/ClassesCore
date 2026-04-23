package com.azuredoom.classescore.data;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.TranslationUtil;

/**
 * Represents an argument type for a {@link ClassDefinition}. This class is used in command parsing to validate and
 * handle class identifiers provided by users. It provides functionality for parsing class IDs, suggesting potential
 * completions during user input, and managing example identifiers for display in usage messages.
 * <ul>
 * <li>Each instance of this class ensures the argument corresponds to a valid registered class.</li>
 * <li>Defines error messages for invalid input or missing class identifiers.</li>
 * <li>Supports suggestion generation based on partial user input.</li>
 * </ul>
 * This class is implemented as a singleton, with the single available instance accessible via the {@code INSTANCE}
 * constant. It uses {@code ClassesCore}'s class registry to validate and suggest class IDs.
 */
public final class ClassIdArgumentType extends ArgumentType<ClassDefinition> {

    public static final ClassIdArgumentType INSTANCE = new ClassIdArgumentType();

    private ClassIdArgumentType() {
        super(
            "classescore.argument.class",
            Message.raw("A valid class id"),
            1,
            exampleIds()
        );
    }

    /**
     * Retrieves an array of all class IDs currently registered in the class registry.
     *
     * @return An array of strings, where each string represents the ID of a registered class.
     */
    private static String[] exampleIds() {
        return ClassesCore.getClassRegistry()
            .all()
            .stream()
            .map(ClassDefinition::id)
            .toArray(String[]::new);
    }

    /**
     * Parses an input array to identify and retrieve a {@link ClassDefinition} based on the class ID provided. If the
     * input is empty or the class ID is not found in the registry, the method reports a failure in the
     * {@link ParseResult} and returns {@code null}.
     *
     * @param input       The array of input strings, where the first element is expected to be the class ID.
     * @param parseResult The parsing context used to indicate success or failure during the parsing process. If parsing
     *                    fails, a failure message is recorded here.
     * @return The {@link ClassDefinition} corresponding to the provided class ID, or {@code null} if the input is
     *         invalid or the class ID is unknown.
     */
    @Override
    @Nullable
    public ClassDefinition parse(@Nonnull String[] input, @Nonnull ParseResult parseResult) {
        if (input.length == 0) {
            parseResult.fail(Message.raw("Missing class id."));
            return null;
        }

        var entered = input[0];

        return ClassesCore.getClassRegistry()
            .get(entered)
            .orElseGet(() -> {
                parseResult.fail(
                    TranslationUtil.translate(BaseLangMessages.UNKNOWN_CLASS, msg -> msg.param("classId", entered))
                );
                return null;
            });
    }

    /**
     * Generates suggestions for partially entered class IDs during user input.
     *
     * @param sender             the command sender requesting suggestions
     * @param textAlreadyEntered the text input that has already been entered by the user
     * @param numParametersTyped the number of parameters typed so far in the command
     * @param result             the suggestion result object used to collect and store possible completions
     */
    @Override
    public void suggest(
        @Nonnull CommandSender sender,
        @Nonnull String textAlreadyEntered,
        int numParametersTyped,
        @Nonnull SuggestionResult result
    ) {
        var entered = textAlreadyEntered.toLowerCase();

        ClassesCore.getClassRegistry()
            .all()
            .stream()
            .map(ClassDefinition::id)
            .filter(id -> id.toLowerCase().startsWith(entered))
            .forEach(result::suggest);
    }
}
