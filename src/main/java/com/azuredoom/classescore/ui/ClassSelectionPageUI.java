package com.azuredoom.classescore.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.UIUtil;

public final class ClassSelectionPageUI extends InteractiveCustomUIPage<ClassSelectionPageUI.Data> {

    private static final String UI_DOCUMENT = "Pages/ClassesCore/ClassSelectionPageUI.ui";

    @Nullable
    private String previewClassId;

    @Nullable
    private String statusMessage;

    private int currentPage = 0;

    public ClassSelectionPageUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    /**
     * Builds and initializes the Class Selection UI components. This method constructs the UI by appending the base
     * document structure and binding events for user interaction. It also ensures that the UI reflects the current page
     * state by writing the state to the provided UI command builder.
     *
     * @param ref              A reference to the {@link EntityStore}, representing the player's specific data.
     * @param uiCommandBuilder The {@link UICommandBuilder} instance used to construct and apply changes to the
     *                         customizable UI components.
     * @param uiEventBuilder   The {@link UIEventBuilder} instance used to configure and register event bindings for the
     *                         interactive elements in the UI.
     * @param store            The {@link Store} instance that provides access to entity components and manages game
     *                         state related to this class selection process.
     */
    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder uiCommandBuilder,
        @Nonnull UIEventBuilder uiEventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        uiCommandBuilder.append(UI_DOCUMENT);

        bindEvents(uiEventBuilder);
        writeState(uiCommandBuilder, Objects.requireNonNull(getPageState(ref, store)));
    }

    /**
     * Handles a data event triggered from the Class Selection UI. This method processes various actions encapsulated in
     * the data object, such as refreshing the page, closing the UI, confirming a class selection, or previewing a
     * specific class. For invalid or unrecognized actions, the page is refreshed by default.
     *
     * @param ref   A reference to the {@link EntityStore}, representing the player's specific data.
     * @param store The {@link Store} instance that provides access to entity components and manages game state related
     *              to this class selection process.
     * @param data  The data object encapsulating the action to be performed. Valid actions include "close", "confirm",
     *              and "preview:<index>". If the action is null or unrecognized, the page is refreshed.
     */
    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull Data data
    ) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null || data.action.isBlank()) {
            refreshPage(ref, store);
            return;
        }

        switch (data.action) {
            case "close" -> {
                close();
                return;
            }
            case "confirm" -> {
                handleConfirm(ref, store);
                return;
            }
            case "page:prev" -> {
                currentPage = Math.max(0, currentPage - 1);
                refreshPage(ref, store);
                return;
            }
            case "page:next" -> {
                int totalPages = getTotalPages(UIUtil.getSortedClasses().size());
                currentPage = Math.min(totalPages - 1, currentPage + 1);
                refreshPage(ref, store);
                return;
            }
        }

        if (data.action.startsWith("preview:")) {
            var rawIndex = data.action.substring("preview:".length());
            try {
                var rowIndex = Integer.parseInt(rawIndex);
                handlePreviewByIndex(rowIndex, ref, store);
            } catch (NumberFormatException ignored) {
                statusMessage = BaseLangMessages.UI_INVALID_CLASS_SELECTION.getAnsiMessage();
                refreshPage(ref, store);
            }
            return;
        }

        refreshPage(ref, store);
    }

    /**
     * Binds UI events for various interactive elements in the Class Selection UI. This method defines event bindings
     * for close, confirm, and preview actions. The bindings are applied using the provided {@link UIEventBuilder}.
     *
     * @param uiEventBuilder The {@link UIEventBuilder} instance used to configure and register event bindings with the
     *                       associated UI elements.
     */
    private void bindEvents(@Nonnull UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#closebtn",
            new EventData().append("Action", "close")
        );

        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#confirmbtn",
            new EventData().append("Action", "confirm")
        );

        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#prevbtn",
            new EventData().append("Action", "page:prev")
        );

        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#nextbtn",
            new EventData().append("Action", "page:next")
        );

        for (var i = 1; i <= UIUtil.ROWS_PER_PAGE; i++) {
            uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#preview" + i,
                new EventData().append("Action", "preview:" + i)
            );
        }
    }

    /**
     * Handles the preview action for the class specified by the index in the Class Selection UI. This method calculates
     * the absolute index, validates its range, retrieves the corresponding class, and initiates the preview action. In
     * case of an invalid index, it updates the status message and refreshes the page.
     *
     * @param rowIndex The 1-based index of the class in the current UI page to be previewed. This value is converted to
     *                 an absolute index for validation and processing.
     * @param ref      A reference to the {@link EntityStore}, representing the player's specific data.
     * @param store    A reference to the {@link Store<EntityStore>}, used for accessing player data.
     */
    private void handlePreviewByIndex(int rowIndex, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var classes = UIUtil.getSortedClasses();
        var absoluteIndex = (currentPage * UIUtil.ROWS_PER_PAGE) + (rowIndex - 1);

        if (absoluteIndex < 0 || absoluteIndex >= classes.size()) {
            statusMessage = BaseLangMessages.UI_INVALID_CLASS_SELECTION.getAnsiMessage();
            refreshPage(ref, store);
            return;
        }

        handlePreview(classes.get(absoluteIndex).id(), ref, store);
    }

    /**
     * Handles the preview action for a selected class in the Class Selection UI. This method validates the given class
     * identifier, checks its existence in the system, and updates the UI to reflect the previewed class. If the class
     * is invalid or no longer registered, a status message is set and the page is refreshed.
     *
     * @param classId The identifier of the class to be previewed. Must be a non-null, non-blank string representing a
     *                valid class identifier.
     * @param ref     A reference to the {@link EntityStore}, representing the player's specific data.
     * @param store   A reference to the {@link Store<EntityStore>}, used for accessing player data.
     */
    private void handlePreview(
        @Nonnull String classId,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        if (classId.isBlank()) {
            statusMessage = "Invalid class selection.";
            refreshPage(ref, store);
            return;
        }

        if (
            ClassesCore.getClassRegistryIfPresent()
                .flatMap(registry -> registry.get(classId))
                .isEmpty()
        ) {
            statusMessage = BaseLangMessages.UI_CLASS_NO_LONGER_REGISTERED.getAnsiMessage();
            refreshPage(ref, store);
            return;
        }

        previewClassId = classId;
        statusMessage = null;
        refreshPage(ref, store);
    }

    /**
     * Handles the confirmation action for selecting a player's class in the Class Selection UI. This method validates
     * the selected class, ensures that a class is not already chosen, attempts the class selection process, provides
     * feedback to the user, and updates the UI as needed.
     *
     * @param ref   A reference to the {@link EntityStore}, representing the specific player's data.
     * @param store The {@link Store} instance that provides access to entity components and manages game state related
     *              to entities.
     */
    private void handleConfirm(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        var playerId = playerRef.getUuid();

        if (previewClassId == null || previewClassId.isBlank()) {
            statusMessage = BaseLangMessages.UI_CHOOSE_CLASS_FIRST.getAnsiMessage();
            refreshPage(ref, store);
            return;
        }

        if (
            ClassesCore.getClassService()
                .getPlayerState(playerId)
                .map(PlayerClassState::classId)
                .flatMap(ClassesCore.getClassRegistry()::get)
                .isPresent()
        ) {
            statusMessage = BaseLangMessages.UI_ALREADY_HAVE_CLASS.getAnsiMessage();
            refreshPage(ref, store);
            return;
        }

        try {
            ClassesCore.getClassService().selectClass(playerId, previewClassId);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusMessage = ex.getMessage();
            refreshPage(ref, store);
            return;
        }

        var chosenName = ClassesCore.getClassServiceIfPresent()
            .flatMap(s -> s.getSelectedClassDefinition(playerId))
            .map(ClassDefinition::displayName)
            .orElse(previewClassId);

        playerRef.sendMessage(BaseLangMessages.SELECTED_CLASS.param("className", chosenName));

        close();
    }

    /**
     * Refreshes and updates the Class Selection UI page to reflect the latest state. This method rebuilds the UI
     * components by fetching the current page state and applying the necessary updates using a
     * {@link UICommandBuilder}. The updated UI may include the currently selected class information, previewed class
     * details, status messages, and available class rows. The method ensures that the UI state is consistent with the
     * underlying page state.
     *
     * @param ref   A reference to the {@link EntityStore}, representing the player's specific data.
     * @param store the entity store used to access and modify components and data.
     */
    private void refreshPage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var builder = new UICommandBuilder();
        writeState(builder, Objects.requireNonNull(getPageState(ref, store)));
        this.sendUpdate(builder);
    }

    /**
     * Updates the state of the Class Selection UI by configuring UI elements to reflect the current page state,
     * including selected, previewed, and available classes.
     *
     * @param ui    The {@link UICommandBuilder} instance used to build and apply changes to the UI components.
     * @param state The {@link PageState} object that encapsulates the current state of the page, including class
     *              details, selection state, preview information, and status messages.
     */
    private void writeState(@Nonnull UICommandBuilder ui, @Nonnull PageState state) {
        setText(
            ui,
            "#currentclass",
            BaseLangMessages.UI_CURRENT_CLASS.param(
                "className",
                Optional.ofNullable(state.currentClassName()).orElse(BaseLangMessages.UI_NONE.getAnsiMessage())
            )
        );
        setText(ui, "#classcount", BaseLangMessages.UI_AVAILABLE_CLASSES.param("count", state.totalClassCount()));

        setText(ui, "#previewname", Message.raw(state.previewName()));
        setText(ui, "#previewdescription", Message.raw(state.previewDescription()));
        setText(ui, "#statuslabel", Message.raw(state.statusText()));

        setText(
            ui,
            "#confirmbtn",
            state.currentClassId() != null ? BaseLangMessages.UI_LOCKED_IN : BaseLangMessages.UI_CONFIRM
        );
        ui.set("#confirmbtn.Disabled", state.confirmDisabled());

        setText(
            ui,
            "#pagelabel",
            Message.raw("Page " + (state.currentPage() + 1) + " / " + state.totalPages())
        );

        ui.set("#prevbtn.Disabled", !state.hasPreviousPage());
        ui.set("#nextbtn.Disabled", !state.hasNextPage());

        for (var i = 1; i <= UIUtil.ROWS_PER_PAGE; i++) {
            var classIndex = i - 1;
            var visible = classIndex < state.classes().size();

            var rowId = "#row" + i;
            var nameId = "#classname" + i;
            var descId = "#classdescription" + i;
            var statusId = "#rowstatus" + i;
            var buttonId = "#preview" + i;

            ui.set(rowId + ".Visible", visible);

            if (!visible) {
                setText(ui, nameId, Message.empty());
                setText(ui, descId, Message.empty());
                setText(ui, statusId, Message.empty());
                setText(ui, buttonId, BaseLangMessages.UI_VIEW);
                ui.set(buttonId + ".Disabled", true);
                continue;
            }

            var def = state.classes().get(classIndex);
            var isPreview = def.id().equals(state.previewClassId());
            var isCurrent = def.id().equals(state.currentClassId());

            setText(ui, nameId, Message.raw(UIUtil.safe(def.displayName())));
            setText(ui, descId, Message.raw(UIUtil.safe(def.description())));
            setText(
                ui,
                statusId,
                isCurrent
                    ? BaseLangMessages.UI_SELECTED
                    : (isPreview ? BaseLangMessages.UI_PREVIEWING : Message.empty())
            );
            setText(ui, buttonId, isPreview ? BaseLangMessages.UI_VIEWING : BaseLangMessages.UI_VIEW);
            ui.set(buttonId + ".Disabled", isPreview);
        }
    }

    /**
     * Sets the text content for a specified UI element. The method updates the text spans of the given UI element
     * identified by the provided selector to display the specified text.
     *
     * @param ui       The {@link UICommandBuilder} instance used to construct and apply UI updates.
     * @param selector The selector string used to identify the target UI element.
     * @param message  The text content to be set for the target UI element.
     */
    private static void setText(@Nonnull UICommandBuilder ui, @Nonnull String selector, @Nonnull Message message) {
        ui.set(selector + ".TextSpans", message);
    }

    /**
     * Retrieves a subset of class definitions corresponding to the specified page. The classes are paginated based on a
     * fixed number of rows per page.
     *
     * @param allClasses The list of all available class definitions. Must not be null.
     * @param page       The page index (0-based) for which class definitions are to be retrieved. If the value is less
     *                   than zero or beyond the total number of pages, an empty list is returned.
     * @return A list of class definitions for the given page. The list may be empty if the page index is invalid or out
     *         of range.
     */
    private static List<ClassDefinition> getPageClasses(List<ClassDefinition> allClasses, int page) {
        var start = page * UIUtil.ROWS_PER_PAGE;
        var end = Math.min(start + UIUtil.ROWS_PER_PAGE, allClasses.size());

        if (start >= allClasses.size() || start < 0) {
            return List.of();
        }

        return allClasses.subList(start, end);
    }

    /**
     * Calculates the total number of pages required to display a specified number of classes. The calculation is based
     * on a fixed number of rows per page.
     *
     * @param totalClasses The total number of classes to be displayed. Must be a non-negative integer.
     * @return The total number of pages required, with a minimum value of 1.
     */
    private static int getTotalPages(int totalClasses) {
        return Math.max(1, (int) Math.ceil((double) totalClasses / UIUtil.ROWS_PER_PAGE));
    }

    /**
     * Retrieves the current state of the page, including information about available classes, pagination, and the
     * preview of the selected or highlighted class.
     *
     * @param ref   A reference to the {@link EntityStore}, representing the player's specific data.
     * @param store the entity store used to access and modify components and data
     * @return an instance of {@code PageState} containing the current page details, class information, and other
     *         necessary data for displaying the UI state.
     */
    private PageState getPageState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return null;
        }
        var playerId = playerRef.getUuid();
        var allClasses = UIUtil.getSortedClasses();
        var totalPages = getTotalPages(allClasses.size());

        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        var classes = getPageClasses(allClasses, currentPage);

        var currentClassId = ClassesCore.getClassServiceIfPresent()
            .flatMap(s -> s.getPlayerState(playerId))
            .map(PlayerClassState::classId)
            .orElse(null);

        if (previewClassId == null) {
            previewClassId = currentClassId != null
                ? currentClassId
                : (allClasses.isEmpty() ? null : allClasses.getFirst().id());
        }

        Optional<ClassDefinition> previewClass = previewClassId == null
            ? Optional.empty()
            : ClassesCore.getClassRegistryIfPresent().flatMap(registry -> registry.get(previewClassId));

        var currentClassName = currentClassId == null
            ? BaseLangMessages.UI_NONE.getAnsiMessage()
            : ClassesCore.getClassRegistryIfPresent()
                .flatMap(registry -> registry.get(currentClassId))
                .map(ClassDefinition::displayName)
                .orElse(currentClassId);

        String previewName;
        String previewDescription;
        String badgeText;
        String statusText;

        if (allClasses.isEmpty()) {
            previewName = BaseLangMessages.UI_NO_CLASSES_AVAILABLE.getAnsiMessage();
            previewDescription = BaseLangMessages.UI_NO_DESCRIPTION_AVAILABLE.getAnsiMessage();
            badgeText = "";
            statusText = statusMessage != null && !statusMessage.isBlank()
                ? statusMessage
                : "No classes found.";
        } else {
            previewName = previewClass.map(ClassDefinition::displayName)
                .orElse(BaseLangMessages.UI_UNKNOWN_CLASS.getAnsiMessage());
            previewDescription = previewClass.map(ClassDefinition::description)
                .orElse(BaseLangMessages.UI_NO_DESCRIPTION_AVAILABLE.getAnsiMessage());
            badgeText = previewClassId != null && previewClassId.equals(currentClassId)
                ? BaseLangMessages.UI_SELECTED.getAnsiMessage()
                : BaseLangMessages.UI_PREVIEW.getAnsiMessage();

            if (statusMessage != null && !statusMessage.isBlank()) {
                statusText = statusMessage;
            } else if (currentClassId != null) {
                statusText = BaseLangMessages.UI_ALREADY_CHOSEN_CLASS.getAnsiMessage();
            } else if (previewClassId != null) {
                statusText = BaseLangMessages.UI_PRESS_CONFIRM.getAnsiMessage();
            } else {
                statusText = BaseLangMessages.UI_SELECT_CLASS_TO_CONTINUE.getAnsiMessage();
            }
        }

        var confirmDisabled = allClasses.isEmpty() || currentClassId != null || previewClassId == null;

        return new PageState(
            classes,
            currentPage,
            totalPages,
            allClasses.size(),
            currentPage > 0,
            currentPage < totalPages - 1,
            currentClassId,
            currentClassName,
            previewClassId,
            previewName,
            previewDescription,
            badgeText,
            statusText,
            confirmDisabled
        );
    }

    /**
     * Represents a data structure encapsulating a single actionable field necessary for UI and backend interactions in
     * the Class Selection Page framework. The field can be serialized and deserialized using a specialized codec for
     * seamless communication and persistence.
     */
    public static final class Data {

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
            .append(
                new KeyedCodec<>("Action", Codec.STRING),
                (data, value) -> data.action = value,
                data -> data.action
            )
            .add()
            .build();

        @Nullable
        private String action;
    }
}
