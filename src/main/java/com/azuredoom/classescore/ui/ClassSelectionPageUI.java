package com.azuredoom.classescore.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
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

public final class ClassSelectionPageUI extends InteractiveCustomUIPage<ClassSelectionPageUI.Data> {

    private static final int MAX_ROWS = 8;

    private static final String UI_DOCUMENT = "Pages/ClassesCore/ClassSelectionPageUI.ui";

    private final PlayerRef playerRef;

    @Nullable
    private String previewClassId;

    @Nullable
    private String statusMessage;

    public ClassSelectionPageUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder uiCommandBuilder,
        @Nonnull UIEventBuilder uiEventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        uiCommandBuilder.append(UI_DOCUMENT);

        bindEvents(uiEventBuilder);
        writeState(uiCommandBuilder, getPageState());
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull Data data
    ) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null || data.action.isBlank()) {
            refreshPage();
            return;
        }

        if ("close".equals(data.action)) {
            close();
            return;
        }

        if ("confirm".equals(data.action)) {
            handleConfirm(ref, store);
            return;
        }

        if (data.action.startsWith("preview:")) {
            var rawIndex = data.action.substring("preview:".length());
            try {
                int rowIndex = Integer.parseInt(rawIndex);
                handlePreviewByIndex(rowIndex);
            } catch (NumberFormatException ignored) {
                statusMessage = "Invalid class selection.";
                refreshPage();
            }
            return;
        }

        refreshPage();
    }

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

        for (var i = 1; i <= MAX_ROWS; i++) {
            uiEventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#preview" + i,
                    new EventData().append("Action", "preview:" + i)
            );
        }
    }

    private void handlePreviewByIndex(int rowIndex) {
        var classes = getSortedClasses();
        var index = rowIndex - 1;

        if (index < 0 || index >= classes.size()) {
            statusMessage = "Invalid class selection.";
            refreshPage();
            return;
        }

        handlePreview(classes.get(index).id());
    }

    private void handlePreview(@Nonnull String classId) {
        if (classId.isBlank()) {
            statusMessage = "Invalid class selection.";
            refreshPage();
            return;
        }

        if (!ClassesCoreAPI.hasClass(classId)) {
            statusMessage = "That class is no longer registered.";
            refreshPage();
            return;
        }

        previewClassId = classId;
        statusMessage = null;
        refreshPage();
    }

    private void handleConfirm(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        var playerId = playerRef.getUuid();

        if (previewClassId == null || previewClassId.isBlank()) {
            statusMessage = "Choose a class first.";
            refreshPage();
            return;
        }

        if (ClassesCoreAPI.playerHasClass(playerId)) {
            statusMessage = "You already have a class selected.";
            refreshPage();
            return;
        }

        boolean success;
        try {
            success = ClassesCoreAPI.selectClass(playerId, previewClassId);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusMessage = ex.getMessage();
            refreshPage();
            return;
        }

        if (!success) {
            statusMessage = "Could not select class.";
            refreshPage();
            return;
        }

        var player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var chosenName = ClassesCoreAPI.getSelectedClass(playerId)
                .map(ClassDefinition::displayName)
                .orElse(previewClassId);

            player.sendMessage(Message.raw("Selected class: " + chosenName));
        }

        close();
    }

    private void refreshPage() {
        var builder = new UICommandBuilder();
        writeState(builder, getPageState());
        this.sendUpdate(builder);
    }

    private void writeState(@Nonnull UICommandBuilder ui, @Nonnull PageState state) {
        setText(ui, "#currentclass", "Current Class: " + state.currentClassName());
        setText(ui, "#classcount", "Available Classes: " + state.classes().size());

        setText(ui, "#previewname", state.previewName());
        setText(ui, "#previewbadge", state.badgeText());
        setText(ui, "#previewdescription", state.previewDescription());
        setText(ui, "#statuslabel", state.statusText());

        setText(ui, "#confirmbtn", state.currentClassId() != null ? "Locked In" : "Confirm");
        ui.set("#confirmbtn.Disabled", state.confirmDisabled());

        for (var i = 1; i <= MAX_ROWS; i++) {
            var classIndex = i - 1;
            var visible = classIndex < state.classes().size();

            var rowId = "#row" + i;
            var nameId = "#classname" + i;
            var descId = "#classdescription" + i;
            var statusId = "#rowstatus" + i;
            var buttonId = "#preview" + i;

            ui.set(rowId + ".Visible", visible);

            if (!visible) {
                setText(ui, nameId, "");
                setText(ui, descId, "");
                setText(ui, statusId, "");
                setText(ui, buttonId, "View");
                ui.set(buttonId + ".Disabled", true);
                continue;
            }

            var def = state.classes().get(classIndex);
            var isPreview = def.id().equals(state.previewClassId());
            var isCurrent = def.id().equals(state.currentClassId());

            setText(ui, nameId, safe(def.displayName()));
            setText(ui, descId, safe(def.description()));
            setText(ui, statusId, isCurrent ? "SELECTED" : (isPreview ? "PREVIEWING" : ""));
            setText(ui, buttonId, isPreview ? "Viewing" : "View");
            ui.set(buttonId + ".Disabled", isPreview);
        }
    }

    private static void setText(@Nonnull UICommandBuilder ui, @Nonnull String selector, @Nonnull String text) {
        ui.set(selector + ".TextSpans", Message.raw(text));
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

        var currentClassName = currentClassId == null
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
