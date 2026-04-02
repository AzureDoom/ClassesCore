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
 * @param currentPage        The index of the current page being displayed.
 * @param totalPages         The total number of pages available based on the number of classes.
 * @param totalClassCount    The total number of classes available for selection.
 * @param hasPreviousPage    Indicates if there is a previous page available.
 * @param hasNextPage        Indicates if there is a next page available.
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
    int currentPage,
    int totalPages,
    int totalClassCount,
    boolean hasPreviousPage,
    boolean hasNextPage,
    @Nullable String currentClassId,
    @Nullable String currentClassName,
    @Nullable String previewClassId,
    String previewName,
    String previewDescription,
    String badgeText,
    String statusText,
    boolean confirmDisabled
) {}
