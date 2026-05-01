package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.OfficialMessagesUI;
import com.smart.phone.ui.data.IPhoneInfoData;
import com.smart.phone.ui.data.OfficialMessagesData;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = OfficialMessages.OFFICIAL_MESSAGES_ID, registry = IApp.ID)
public class OfficialMessages extends IApp {
    public static final String OFFICIAL_MESSAGES_ID = SmartPhone.MOD_ID + ":official_messages";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.officialMessages");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/notepad.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new OfficialMessagesUI(homeScreen);
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

    @Override
    public Class<? extends IPhoneInfoData> getDataType() {
        return OfficialMessagesData.class;
    }
}
