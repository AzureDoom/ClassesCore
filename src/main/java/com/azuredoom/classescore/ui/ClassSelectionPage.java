package com.azuredoom.classescore.ui;

import au.ellie.hyui.builders.*;
import au.ellie.hyui.events.UIContext;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.azuredoom.classescore.api.ClassesCoreAPI;
import com.azuredoom.classescore.data.ClassDefinition;

public final class ClassSelectionPage {

    private static final int MAX_ROWS = 8;

    private final PlayerRef playerRef;

    @Nullable
    private String previewClassId;

    @Nullable
    private String statusMessage;

    public ClassSelectionPage(@Nonnull PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public void open(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        openInternal(ref, store);
    }

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

        for (int i = 1; i <= MAX_ROWS; i++) {
            final int rowIndex = i;
            page.addEventListener(
                "preview-" + rowIndex,
                CustomUIEventBindingType.Activating,
                (ignored, ctx) -> handlePreviewByIndex(rowIndex, ctx)
            );
        }

        page.open(store);
    }

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

        for (int i = 1; i <= MAX_ROWS; i++) {
            list.addChild(buildRow(i, state));
            if (i < MAX_ROWS) {
                list.addChild(spacerH(8));
            }
        }

        panel.addChild(list);
        return panel;
    }

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

        int classIndex = index - 1;
        boolean visible = classIndex < state.classes().size();

        String name = "";
        String description = "";
        String status = "";
        boolean disableButton = true;
        String buttonText = "View";

        if (visible) {
            var def = state.classes().get(classIndex);
            boolean isPreview = def.id().equals(state.previewClassId());
            boolean isCurrent = def.id().equals(state.currentClassId());

            name = safe(def.displayName());
            description = safe(def.description());
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

    private GroupBuilder spacerW(int width) {
        return GroupBuilder.group().withAnchor(size(width, 1));
    }

    private GroupBuilder spacerH(int height) {
        return GroupBuilder.group().withAnchor(size(1, height));
    }

    private static HyUIAnchor size(int width, int height) {
        return new HyUIAnchor()
            .setWidth(width)
            .setHeight(height);
    }

    private static HyUIAnchor fill() {
        return new HyUIAnchor()
            .setFull(0);
    }

    private static HyUIStyle titleStyle() {
        return new HyUIStyle()
            .setFontSize(16)
            .setRenderBold(true)
            .setTextColor("#dbe7f5");
    }

    private static HyUIStyle subtitleStyle() {
        return new HyUIStyle()
            .setFontSize(13)
            .setTextColor("#8fa8b7")
            .setRenderBold(true);
    }

    private static HyUIStyle rowNameStyle() {
        return new HyUIStyle()
            .setFontSize(16)
            .setRenderBold(true)
            .setTextColor("#f1f6ff");
    }

    private static HyUIStyle rowDescriptionStyle() {
        return new HyUIStyle()
            .setFontSize(13)
            .setWrap(true)
            .setTextColor("#d0d9e2");
    }

    private static HyUIStyle rowStatusStyle() {
        return new HyUIStyle()
            .setFontSize(12)
            .setRenderBold(true)
            .setRenderUppercase(true)
            .setTextColor("#9fe3ff");
    }

    private static HyUIStyle previewNameStyle() {
        return new HyUIStyle()
            .setFontSize(18)
            .setRenderBold(true)
            .setTextColor("#f1f6ff");
    }

    private static HyUIStyle previewDescriptionStyle() {
        return new HyUIStyle()
            .setFontSize(14)
            .setWrap(true)
            .setTextColor("#d6e1ea");
    }

    private static HyUIStyle statusStyle() {
        return new HyUIStyle()
            .setFontSize(14)
            .setWrap(true)
            .setTextColor("#f6e39a");
    }

    private void handlePreviewByIndex(
        int rowIndex,
        @Nonnull UIContext ctx
    ) {
        var classes = getSortedClasses();
        int index = rowIndex - 1;

        if (index < 0 || index >= classes.size()) {
            statusMessage = "Invalid class selection.";
            applyState(ctx, getPageState());
            return;
        }

        handlePreview(classes.get(index).id(), ctx);
    }

    private void handlePreview(
        @Nonnull String classId,
        @Nonnull UIContext ctx
    ) {
        if (classId.isBlank()) {
            statusMessage = "Invalid class selection.";
            applyState(ctx, getPageState());
            return;
        }

        if (!ClassesCoreAPI.hasClass(classId)) {
            statusMessage = "That class is no longer registered.";
            applyState(ctx, getPageState());
            return;
        }

        previewClassId = classId;
        statusMessage = null;
        applyState(ctx, getPageState());
    }

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

        if (ClassesCoreAPI.playerHasClass(playerId)) {
            statusMessage = "You already have a class selected.";
            applyState(ctx, getPageState());
            return;
        }

        boolean success;
        try {
            success = ClassesCoreAPI.selectClass(playerId, previewClassId);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusMessage = ex.getMessage();
            applyState(ctx, getPageState());
            return;
        }

        if (!success) {
            statusMessage = "Could not select class.";
            applyState(ctx, getPageState());
            return;
        }

        var player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var chosenName = ClassesCoreAPI.getSelectedClass(playerId)
                .map(ClassDefinition::displayName)
                .orElse(previewClassId);

            player.sendMessage(Message.raw("Selected class: " + chosenName));
        }

        ctx.getPage().ifPresent(HyUIPage::close);
    }

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

        for (var i = 1; i <= MAX_ROWS; i++) {
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
                .ifPresent(label -> label.withText(safe(def.displayName())));

            ctx.getById(descId, LabelBuilder.class)
                .ifPresent(label -> label.withText(safe(def.description())));

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

    private PageState getPageState() {
        var playerId = playerRef.getUuid();
        var classes = getSortedClasses();
        var currentClassId = ClassesCoreAPI.getSelectedClassId(playerId).orElse(null);

        if (previewClassId == null) {
            previewClassId = currentClassId != null
                ? currentClassId
                : (classes.isEmpty() ? null : classes.getFirst().id());
        }

        Optional<ClassDefinition> previewClass = previewClassId == null
            ? Optional.empty()
            : ClassesCoreAPI.getClassDefinition(previewClassId);

        String currentClassName = currentClassId == null
            ? "None"
            : ClassesCoreAPI.getClassDefinition(currentClassId)
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

        boolean confirmDisabled = classes.isEmpty() || currentClassId != null || previewClassId == null;

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

    private List<ClassDefinition> getSortedClasses() {
        return ClassesCoreAPI.getClasses()
            .stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(def -> safe(def.displayName()).toLowerCase()))
            .toList();
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private record PageState(
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
}
