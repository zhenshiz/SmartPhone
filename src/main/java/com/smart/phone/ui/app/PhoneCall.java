package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.PhoneCallUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = PhoneCall.PHONE_CALL_ID, registry = IApp.ID)
public class PhoneCall extends IApp {
    public static final String PHONE_CALL_ID = SmartPhone.MOD_ID + ":phone_call";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.phoneCall");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/item/phone.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new PhoneCallUI(homeScreen);
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
