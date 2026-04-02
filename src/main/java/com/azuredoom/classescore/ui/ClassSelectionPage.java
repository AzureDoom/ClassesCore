package com.azuredoom.classescore.ui;

import au.ellie.hyui.builders.*;
import au.ellie.hyui.events.UIContext;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.lang.BaseLangMessages;
import com.azuredoom.classescore.util.UIUtil;

// TODO: Translations for UI
public final class ClassSelectionPage {

    private final PlayerRef playerRef;

    @Nullable
    private String previewClassId;

    @Nullable
    private String statusMessage;

    public ClassSelectionPage(@Nonnull PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    /**
     * Opens a connection or initializes resources using the provided reference and store.
     *
     * @param ref   a non-null reference to an {@code EntityStore}.
     * @param store a non-null store containing the {@code EntityStore}.
     */
    public void open(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        openInternal(ref, store);
    }

    /**
     * Opens an internal UI page for the player with specified event listeners and elements. This method configures the
     * page with a dismissible lifetime, adds UI elements, and attaches event listeners for specific actions such as
     * "close," "confirm," and "preview" buttons for multiple rows.
     *
     * @param ref   A reference to the {@link EntityStore}, which holds entity-related data.
     * @param store A store for managing the lifecycle and persistence of {@link EntityStore}.
     */
    private void openInternal(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        var state = getPageState();

        var page = PageBuilder.pageForPlayer(playerRef)
            .withLifetime(CustomPageLifetime.CanDismiss)
            .addElement(buildRoot(state))
            .addEventListener(
                "close-btn",
                CustomUIEventBindingType.Activating,
                (ignored, ctx) -> ctx.getPage().ifPresent(HyUIPage::close)
            )
            .addEventListener(
                "confirm-btn",
                CustomUIEventBindingType.Activating,
                (ignored, ctx) -> handleConfirm(ref, store, ctx)
            );

        for (var i = 1; i <= UIUtil.MAX_ROWS; i++) {
            final var rowIndex = i;
            page.addEventListener(
                "preview-" + rowIndex,
                CustomUIEventBindingType.Activating,
                (ignored, ctx) -> handlePreviewByIndex(rowIndex, ctx)
            );
        }

        page.open(store);
    }

    /**
     * Builds the root PageOverlayBuilder for the class selection screen.
     *
     * @param state the current state of the page, containing necessary context or data.
     * @return the constructed PageOverlayBuilder with the configured elements for the class selection overlay.
     */
    private PageOverlayBuilder buildRoot(PageState state) {
        var overlay = PageOverlayBuilder.pageOverlay().withId("class-overlay");

        var container = ContainerBuilder.decoratedContainer()
            .withId("class-container")
            .withTitleText("Choose Your Class")
            .withAnchor(size(900, 650));

        var content = GroupBuilder.group()
            .withId("class-content")
            .withLayoutMode("Top")
            .withAnchor(fill());

        content.addChild(buildTopBar(state));
        content.addChild(spacerH(8));
        content.addChild(buildMainContent(state));

        container.addContentChild(content);
        overlay.addChild(container);
        return overlay;
    }

    /**
     * Builds the top bar UI component for the page, which includes buttons, labels, and spacing. The top bar displays
     * information about the current class, the number of available classes, and includes a close button for user
     * interaction.
     *
     * @param state the current state of the page, which provides information about the current class name and the list
     *              of available classes.
     * @return a {@code GroupBuilder} object representing the constructed top bar UI component.
     */
    private GroupBuilder buildTopBar(PageState state) {
        var topBar = GroupBuilder.group()
            .withId("top-bar")
            .withLayoutMode("Left")
            .withAnchor(size(850, 40));

        topBar.addChild(
            ButtonBuilder.secondaryTextButton()
                .withId("close-btn")
                .withText("Close")
                .withAnchor(size(120, 34))
        );

        topBar.addChild(spacerW(24));

        topBar.addChild(
            LabelBuilder.label()
                .withId("current-class")
                .withText("Current Class: " + state.currentClassName())
                .withStyle(new HyUIStyle().setFontSize(14).setRenderBold(true))
                .withAnchor(size(380, 34))
        );

        topBar.addChild(spacerW(24));

        topBar.addChild(
            LabelBuilder.label()
                .withId("class-count")
                .withText("Available Classes: " + state.classes().size())
                .withStyle(new HyUIStyle().setFontSize(14).setRenderBold(true))
                .withAnchor(size(220, 34))
        );

        return topBar;
    }

    /**
     * Builds the main content of the page with specified layout and child components.
     *
     * @param state the current state of the page, providing necessary context for building the content
     * @return a GroupBuilder instance representing the main content structure
     */
    private GroupBuilder buildMainContent(PageState state) {
        var main = GroupBuilder.group()
            .withId("main-content")
            .withLayoutMode("Left")
            .withAnchor(size(850, 560));

        main.addChild(buildClassPanel(state));
        main.addChild(spacerW(10));
        main.addChild(buildPreviewPanel(state));

        return main;
    }

    /**
     * Builds a class panel UI component with defined properties and layout.
     *
     * @param state the current page state used for determining the content and behavior of the panel
     * @return a {@code GroupBuilder} instance representing the constructed class panel
     */
    private GroupBuilder buildClassPanel(PageState state) {
        var panel = GroupBuilder.group()
            .withId("class-panel")
            .withLayoutMode("Top")
            .withAnchor(size(550, 520))
            .withOutlineColor("#314b6a")
            .withOutlineSize(1.0f)
            .withPadding(new HyUIPadding().setLeft(0).setRight(0).setTop(12).setBottom(0));

        panel.addChild(spacerH(8));

        var list = GroupBuilder.group()
            .withId("class-list")
            .withLayoutMode("TopScrolling")
            .withAnchor(size(510, 480))
            .withKeepScrollPosition(true)
            .withPadding(new HyUIPadding().setRight(0).setBottom(10));

        for (var i = 1; i <= UIUtil.MAX_ROWS; i++) {
            list.addChild(buildRow(i, state));
            if (i < UIUtil.MAX_ROWS) {
                list.addChild(spacerH(8));
            }
        }

        panel.addChild(list);
        return panel;
    }

    /**
     * Builds a row UI component based on the specified index and page state.
     *
     * @param index the index of the row to be built
     * @param state the current state of the page, which includes details about the available classes, preview class,
     *              and current class
     * @return a GroupBuilder object that represents the constructed row with its layout, child components, and
     *         properties
     */
    private GroupBuilder buildRow(int index, PageState state) {
        var row = GroupBuilder.group()
            .withId("row-" + index)
            .withLayoutMode("Left")
            .withAnchor(size(468, 116))
            .withOutlineColor("#314b6a")
            .withOutlineSize(1.0f)
            .withPadding(
                new HyUIPadding()
                    .setLeft(14)
                    .setRight(12)
                    .setTop(8)
                    .setBottom(8)
            );

        var classIndex = index - 1;
        var visible = classIndex < state.classes().size();

        var name = "";
        var description = "";
        var status = "";
        var disableButton = true;
        var buttonText = "View";

        if (visible) {
            var def = state.classes().get(classIndex);
            var isPreview = def.id().equals(state.previewClassId());
            var isCurrent = def.id().equals(state.currentClassId());

            name = UIUtil.safe(def.displayName());
            description = UIUtil.safe(def.description());
            status = isCurrent ? "SELECTED" : (isPreview ? "PREVIEWING" : "");
            disableButton = isPreview;
            buttonText = isPreview ? "Viewing" : "View";
        }

        row.withVisible(visible);

        var textCol = GroupBuilder.group()
            .withId("row-text-" + index)
            .withLayoutMode("Top")
            .withAnchor(size(340, 100));

        textCol.addChild(
            LabelBuilder.label()
                .withId("class-name-" + index)
                .withText(name)
                .withStyle(rowNameStyle())
                .withAnchor(size(316, 20))
        );

        textCol.addChild(spacerH(4));

        textCol.addChild(
            LabelBuilder.label()
                .withId("class-description-" + index)
                .withText(description)
                .withStyle(rowDescriptionStyle()) // wrap enabled
                .withAnchor(size(316, 52))
        );

        textCol.addChild(spacerH(4));

        textCol.addChild(
            LabelBuilder.label()
                .withId("row-status-" + index)
                .withText(status)
                .withStyle(rowStatusStyle())
                .withAnchor(size(316, 16))
        );

        row.addChild(textCol);

        row.addChild(spacerW(16));

        row.addChild(
            ButtonBuilder.secondaryTextButton()
                .withId("preview-" + index)
                .withText(buttonText)
                .withDisabled(disableButton)
                .withAnchor(size(88, 34))
        );

        return row;
    }

    /**
     * Constructs and returns a preview panel UI component for displaying various information such as a title,
     * description, status, and a confirmation button. The layout and content are dynamically configured based on the
     * provided {@code PageState}.
     *
     * @param state the current state of the page containing data used to populate the preview panel, including preview
     *              name, description, status text, and button states.
     * @return a {@code GroupBuilder} instance representing the configured preview panel UI component.
     */
    private GroupBuilder buildPreviewPanel(PageState state) {
        var panel = GroupBuilder.group()
            .withId("preview-panel")
            .withLayoutMode("Top")
            .withAnchor(size(290, 520))
            .withOutlineColor("#314b6a")
            .withOutlineSize(1.0f)
            .withPadding(new HyUIPadding().setLeft(14).setRight(14).setTop(12).setBottom(12));

        panel.addChild(spacerH(10));

        panel.addChild(
            LabelBuilder.label()
                .withId("preview-title")
                .withText("Class Preview:")
                .withStyle(titleStyle())
                .withAnchor(size(262, 24))
        );

        panel.addChild(spacerH(10));

        panel.addChild(
            LabelBuilder.label()
                .withId("preview-name")
                .withText(state.previewName())
                .withStyle(previewNameStyle())
                .withAnchor(size(262, 24))
        );

        panel.addChild(spacerH(4));

        panel.addChild(
            LabelBuilder.label()
                .withId("description-title")
                .withText("Description")
                .withStyle(subtitleStyle())
                .withAnchor(size(262, 18))
        );

        panel.addChild(spacerH(6));

        panel.addChild(
            LabelBuilder.label()
                .withId("preview-description")
                .withText(state.previewDescription())
                .withStyle(previewDescriptionStyle())
                .withAnchor(size(262, 210))
        );

        panel.addChild(spacerH(12));

        panel.addChild(
            LabelBuilder.label()
                .withId("status-label")
                .withText(state.statusText())
                .withStyle(statusStyle())
                .withAnchor(size(222, 60))
        );

        panel.addChild(spacerH(-30));

        panel.addChild(
            ButtonBuilder.secondaryTextButton()
                .withId("confirm-btn")
                .withText(state.currentClassId() != null ? "Locked In" : "Confirm")
                .withDisabled(state.confirmDisabled())
                .withAnchor(size(170, 38))
        );

        return panel;
    }

    /**
     * Creates a horizontal spacer with the specified width and a fixed height of 1.
     *
     * @param width the width of the spacer
     * @return an instance of GroupBuilder with the specified spacer configuration
     */
    private GroupBuilder spacerW(int width) {
        return GroupBuilder.group().withAnchor(size(width, 1));
    }

    /**
     * Creates a horizontal spacer with the specified height.
     *
     * @param height the height of the spacer
     * @return a GroupBuilder instance with the defined horizontal spacer
     */
    private GroupBuilder spacerH(int height) {
        return GroupBuilder.group().withAnchor(size(1, height));
    }

    /**
     * Configures the size of a HyUIAnchor instance by setting its width and height.
     *
     * @param width  the width to set for the HyUIAnchor
     * @param height the height to set for the HyUIAnchor
     * @return a HyUIAnchor instance with the specified width and height
     */
    private static HyUIAnchor size(int width, int height) {
        return new HyUIAnchor()
            .setWidth(width)
            .setHeight(height);
    }

    /**
     * Creates and returns a configured instance of {@code HyUIAnchor} with its properties set.
     *
     * @return a new instance of {@code HyUIAnchor} with predefined configuration.
     */
    private static HyUIAnchor fill() {
        return new HyUIAnchor()
            .setFull(0);
    }

    /**
     * Configures and returns a HyUIStyle instance for styling titles.
     *
     * @return a HyUIStyle object with font size set to 16, bold rendering enabled, and the text color set to "#dbe7f5".
     */
    private static HyUIStyle titleStyle() {
        return new HyUIStyle()
            .setFontSize(16)
            .setRenderBold(true)
            .setTextColor("#dbe7f5");
    }

    /**
     * Configures and returns a HyUIStyle specifically styled for subtitles.
     *
     * @return a HyUIStyle object with preset properties: font size of 13, text color set to "#8fa8b7", and bold
     *         rendering enabled.
     */
    private static HyUIStyle subtitleStyle() {
        return new HyUIStyle()
            .setFontSize(13)
            .setTextColor("#8fa8b7")
            .setRenderBold(true);
    }

    /**
     * Creates and returns a predefined style configuration for displaying the name of a row on the Class Selection
     * page. This style ensures consistent formatting for row titles to enhance readability and visual hierarchy.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 16. - Bold rendering
     *         enabled. - Text color set to "#f1f6ff".
     */
    private static HyUIStyle rowNameStyle() {
        return new HyUIStyle()
            .setFontSize(16)
            .setRenderBold(true)
            .setTextColor("#f1f6ff");
    }

    /**
     * Creates and returns a predefined style configuration for displaying descriptive text in rows on the Class
     * Selection page. This style is used to format text such as class descriptions, ensuring readability and proper
     * layout.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 13. - Text wrapping
     *         enabled. - Text color set to "#d0d9e2".
     */
    private static HyUIStyle rowDescriptionStyle() {
        return new HyUIStyle()
            .setFontSize(13)
            .setWrap(true)
            .setTextColor("#d0d9e2");
    }

    /**
     * Creates and returns a predefined style configuration for displaying status information on the Class Selection
     * page. This style is used for rows to indicate status-related information.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 12. - Bold rendering
     *         enabled. - Uppercase text rendering enabled. - Text color set to "#9fe3ff".
     */
    private static HyUIStyle rowStatusStyle() {
        return new HyUIStyle()
            .setFontSize(12)
            .setRenderBold(true)
            .setRenderUppercase(true)
            .setTextColor("#9fe3ff");
    }

    /**
     * Creates and returns a predefined style configuration for displaying the name of a previewed class on the Class
     * Selection page.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 18. - Bold rendering
     *         enabled. - Text color set to "#f1f6ff".
     */
    private static HyUIStyle previewNameStyle() {
        return new HyUIStyle()
            .setFontSize(18)
            .setRenderBold(true)
            .setTextColor("#f1f6ff");
    }

    /**
     * Creates and returns a predefined style configuration for displaying the description text of a previewed class on
     * the Class Selection page.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 14. - Text wrapping
     *         enabled. - Text color set to "#d6e1ea".
     */
    private static HyUIStyle previewDescriptionStyle() {
        return new HyUIStyle()
            .setFontSize(14)
            .setWrap(true)
            .setTextColor("#d6e1ea");
    }

    /**
     * Creates and returns a predefined style configuration for displaying status messages on the Class Selection page.
     *
     * @return an {@link HyUIStyle} object with the following properties: - Font size set to 14. - Text wrapping
     *         enabled. - Text color set to "#f6e39a".
     */
    private static HyUIStyle statusStyle() {
        return new HyUIStyle()
            .setFontSize(14)
            .setWrap(true)
            .setTextColor("#f6e39a");
    }

    /**
     * Handles the previewing of a class based on the specified row index. This method validates the provided row index,
     * determines if it corresponds to a valid class in the sorted list of available classes, and updates the user
     * interface to reflect the previewed class. If the index is invalid, an appropriate status message is displayed.
     *
     * @param rowIndex The zero-based index of the class in the list to be previewed. Must be within the bounds of the
     *                 sorted list of available classes.
     * @param ctx      The {@link UIContext} used to update the user interface. It provides mechanisms to apply changes
     *                 and update the visual state of the page.
     */
    private void handlePreviewByIndex(
        int rowIndex,
        @Nonnull UIContext ctx
    ) {
        var classes = UIUtil.getSortedClasses();
        int index = rowIndex - 1;

        if (index < 0 || index >= classes.size()) {
            statusMessage = "Invalid class selection.";
            applyState(ctx, getPageState());
            return;
        }

        handlePreview(classes.get(index).id(), ctx);
    }

    /**
     * Handles the previewing of a class selection based on the provided class ID. This method verifies the validity of
     * the class ID, checks if the class is registered in the system, and updates the state of the page accordingly. If
     * the class ID is invalid or unregistered, an appropriate status message is set. Otherwise, the class is marked as
     * being previewed.
     *
     * @param classId The ID of the class to be previewed. Must be a non-blank, valid identifier of a registered class.
     * @param ctx     The {@link UIContext} used to update the user interface. Provides methods to apply the updated
     *                state of the page and manage UI interactions.
     */
    private void handlePreview(
        @Nonnull String classId,
        @Nonnull UIContext ctx
    ) {
        if (classId.isBlank()) {
            statusMessage = "Invalid class selection.";
            applyState(ctx, getPageState());
            return;
        }

        if (
            ClassesCore.getClassRegistryIfPresent()
                .flatMap(registry -> registry.get(classId))
                .isEmpty()
        ) {
            statusMessage = "That class is no longer registered.";
            applyState(ctx, getPageState());
            return;
        }

        previewClassId = classId;
        statusMessage = null;
        applyState(ctx, getPageState());
    }

    /**
     * Handles the confirmation of a selected class for the player. This method validates if the player can select a
     * class, checks for any existing class selection, and finalizes the selection process. If the selection is
     * successful, a confirmation message is sent to the player, and the UI page is closed.
     *
     * @param ref   A {@link Ref} to the {@link EntityStore}, used to access and modify the player's relevant components
     *              and data.
     * @param store The {@link Store} of {@link EntityStore}, providing access to entity storage and retrieval
     *              operations for managing player-related data.
     * @param ctx   The {@link UIContext} used to manipulate the user interface. Provides methods to update or close the
     *              current page and handle UI state changes.
     */
    private void handleConfirm(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull UIContext ctx
    ) {
        var playerId = playerRef.getUuid();

        if (previewClassId == null || previewClassId.isBlank()) {
            statusMessage = "Choose a class first.";
            applyState(ctx, getPageState());
            return;
        }

        if (
            ClassesCore.getClassService()
                .getPlayerState(playerId)
                .map(PlayerClassState::classId)
                .flatMap(ClassesCore.getClassRegistry()::get)
                .isPresent()
        ) {
            statusMessage = "You already have a class selected.";
            applyState(ctx, getPageState());
            return;
        }

        try {
            ClassesCore.getClassService().selectClass(playerId, previewClassId);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusMessage = ex.getMessage();
            applyState(ctx, getPageState());
            return;
        }

        var player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var chosenName = ClassesCore.getClassServiceIfPresent()
                .flatMap(s -> s.getSelectedClassDefinition(playerId))
                .map(ClassDefinition::displayName)
                .orElse(previewClassId);

            player.sendMessage(BaseLangMessages.SELECTED_CLASS.param("className", chosenName));
        }

        ctx.getPage().ifPresent(HyUIPage::close);
    }

    /**
     * Applies the given state to the user interface by updating various components of the Class Selection page, such as
     * labels, buttons, and visibility of rows. The method ensures that the UI accurately reflects the current and
     * previewed class information provided in the state.
     *
     * @param ctx   The {@link UIContext} used to manipulate the UI components. It provides access to UI elements by
     *              their identifiers.
     * @param state The {@link PageState} object representing the current state of the Class Selection page, containing
     *              details about available classes, the currently selected class, the previewed class, and other
     *              UI-related data such as labels and button states.
     */
    private void applyState(@Nonnull UIContext ctx, @Nonnull PageState state) {
        ctx.getById("current-class", LabelBuilder.class)
            .ifPresent(label -> label.withText("Current Class: " + state.currentClassName()));

        ctx.getById("class-count", LabelBuilder.class)
            .ifPresent(label -> label.withText("Available Classes: " + state.classes().size()));

        ctx.getById("preview-name", LabelBuilder.class)
            .ifPresent(label -> label.withText(state.previewName()));

        ctx.getById("preview-badge", LabelBuilder.class)
            .ifPresent(label -> label.withText(state.badgeText()));

        ctx.getById("preview-description", LabelBuilder.class)
            .ifPresent(label -> label.withText(state.previewDescription()));

        ctx.getById("status-label", LabelBuilder.class)
            .ifPresent(label -> label.withText(state.statusText()));

        ctx.getById("confirm-btn", ButtonBuilder.class)
            .ifPresent(button -> {
                button.withText(state.currentClassId() != null ? "Locked In" : "Confirm");
                button.withDisabled(state.confirmDisabled());
            });

        for (var i = 1; i <= UIUtil.MAX_ROWS; i++) {
            var classIndex = i - 1;
            var visible = classIndex < state.classes().size();

            var rowId = "row-" + i;
            var nameId = "class-name-" + i;
            var descId = "class-description-" + i;
            var statusId = "row-status-" + i;
            var buttonId = "preview-" + i;

            ctx.getById(rowId, GroupBuilder.class)
                .ifPresent(row -> row.withVisible(visible));

            if (!visible) {
                ctx.getById(nameId, LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById(descId, LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById(statusId, LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById(buttonId, ButtonBuilder.class).ifPresent(button -> {
                    button.withText("View");
                    button.withDisabled(true);
                });
                continue;
            }

            var def = state.classes().get(classIndex);
            var isPreview = def.id().equals(state.previewClassId());
            var isCurrent = def.id().equals(state.currentClassId());

            ctx.getById(nameId, LabelBuilder.class)
                .ifPresent(label -> label.withText(UIUtil.safe(def.displayName())));

            ctx.getById(descId, LabelBuilder.class)
                .ifPresent(label -> label.withText(UIUtil.safe(def.description())));

            ctx.getById(statusId, LabelBuilder.class)
                .ifPresent(
                    label -> label.withText(
                        isCurrent ? "SELECTED" : (isPreview ? "PREVIEWING" : "")
                    )
                );

            ctx.getById(buttonId, ButtonBuilder.class)
                .ifPresent(button -> {
                    button.withText(isPreview ? "Viewing" : "View");
                    button.withDisabled(isPreview);
                });
        }

        ctx.updatePage(true);
    }

    /**
     * Computes and returns the current state of the class selection page, encapsulating the available classes, the
     * currently selected class, the class being previewed, and the corresponding status and user feedback information.
     *
     * @return a {@link PageState} object containing the following: - The list of available {@link ClassDefinition}
     *         objects. - The ID and display name of the currently selected class. - The ID, display name, description,
     *         and badge text of the previewed class. - A status message for the user interface. - A flag indicating
     *         whether the confirm button is disabled.
     */
    private PageState getPageState() {
        var playerId = playerRef.getUuid();
        var classes = UIUtil.getSortedClasses();
        var currentClassId = ClassesCore.getClassServiceIfPresent()
            .flatMap(s -> s.getPlayerState(playerId))
            .map(PlayerClassState::classId)
            .orElse(null);

        if (previewClassId == null) {
            previewClassId = currentClassId != null
                ? currentClassId
                : (classes.isEmpty() ? null : classes.getFirst().id());
        }

        Optional<ClassDefinition> previewClass = previewClassId == null
            ? Optional.empty()
            : ClassesCore.getClassRegistryIfPresent().flatMap(registry -> registry.get(previewClassId));

        var currentClassName = currentClassId == null
            ? "None"
            : ClassesCore.getClassRegistryIfPresent()
                .flatMap(registry -> registry.get(currentClassId))
                .map(ClassDefinition::displayName)
                .orElse(currentClassId);

        String previewName;
        String previewDescription;
        String badgeText;
        String statusText;

        if (classes.isEmpty()) {
            previewName = "No classes available";
            previewDescription = "No classes are currently registered.";
            badgeText = "";
            statusText = statusMessage != null && !statusMessage.isBlank()
                ? statusMessage
                : "No classes found.";
        } else {
            previewName = previewClass.map(ClassDefinition::displayName).orElse("Unknown Class");
            previewDescription = previewClass.map(ClassDefinition::description).orElse("No description available.");
            badgeText = previewClassId != null && previewClassId.equals(currentClassId) ? "SELECTED" : "PREVIEW";

            if (statusMessage != null && !statusMessage.isBlank()) {
                statusText = statusMessage;
            } else if (currentClassId != null) {
                statusText = "You have already chosen a class.";
            } else if (previewClassId != null) {
                statusText = "Press Confirm to lock in this class.";
            } else {
                statusText = "Select a class to continue.";
            }
        }

        var confirmDisabled = classes.isEmpty() || currentClassId != null || previewClassId == null;

        return new PageState(
            classes,
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
}
