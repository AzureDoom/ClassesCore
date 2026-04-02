package com.azuredoom.classescore.ui;

import java.util.List;
import javax.annotation.Nullable;

import com.azuredoom.classescore.data.ClassDefinition;

/**
 * Represents the state of a page in the Class Selection UI. This immutable record is used to encapsulate the current
 * state, including the list of available classes, the selected class, and any preview or status information displayed
 * to the user.
 *
 * @param classes            A list of {@link ClassDefinition} objects defining the available classes for selection.
 * @param currentClassId     The unique identifier of the currently selected class, or null if no class is selected.
 * @param currentClassName   The display name of the currently selected class, or null if no class is selected.
 * @param previewClassId     The unique identifier of the class being previewed, or null if no preview is active.
 * @param previewName        The display name of the class being previewed.
 * @param previewDescription A textual description of the class being previewed.
 * @param badgeText          A text displayed as a badge, typically used to provide additional metadata for the
 *                           previewed class.
 * @param statusText         A text displayed to indicate the current status of the page or provide user feedback.
 * @param confirmDisabled    A flag indicating whether the confirmation button is disabled.
 */
public record PageState(
    List<ClassDefinition> classes,
    @Nullable String currentClassId,
    @Nullable String currentClassName,
    @Nullable String previewClassId,
    String previewName,
    String previewDescription,
    String badgeText,
    String statusText,
    boolean confirmDisabled
) {}
