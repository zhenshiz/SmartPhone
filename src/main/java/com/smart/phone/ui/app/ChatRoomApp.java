package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.ChatRoomUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = ChatRoomApp.CHAT_ROOM_ID, registry = IApp.ID)
public class ChatRoomApp extends IApp {
    public static final String CHAT_ROOM_ID = SmartPhone.MOD_ID + ":chat_room";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.chatRoom");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/notepad.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new ChatRoomUI(homeScreen);
    }

    @Override
    public boolean isDefaultInstalled() {
        return true;
    }

    @Override
    public boolean isAppStoreInstall() {
        return false;
    }

    @Override
    public boolean isUninstall() {
        return false;
    }
}
