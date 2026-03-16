package com.azuredoom.classescore.util;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.lang.BaseLangMessages;

public class NotificationsUtil {

    private NotificationsUtil() {}

    public static void sendItemClassRestrictionNotification(PlayerRef playerRef, ItemStack itemStack, String classId) {
        var itemTranslatedName = I18nModule.get()
            .getMessage(
                playerRef.getLanguage(),
                itemStack.getItem().getTranslationKey()
            );
        var itemName = itemTranslatedName != null ? itemTranslatedName : itemStack.getItemId();
        var definition = ClassesCore.getClassRegistry().get(classId);
        if (definition.isEmpty()) {
            return;
        }
        NotificationUtil.sendNotification(
            playerRef.getPacketHandler(),
            BaseLangMessages.ITEM_RESTRICTED.param("itemName", itemName)
                .param("displayName", definition.get().displayName()),
            NotificationStyle.Danger
        );
    }
}
