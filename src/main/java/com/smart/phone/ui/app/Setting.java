package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.SettingUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = Setting.SETTING_ID, registry = IApp.ID)
public class Setting extends IApp {
    public static final String SETTING_ID = SmartPhone.MOD_ID + ":setting";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.setting");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/setting.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new SettingUI(homeScreen);
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
