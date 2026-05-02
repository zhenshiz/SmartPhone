package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.CameraUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = CameraApp.CAMERA_ID, registry = IApp.ID)
public class CameraApp extends IApp {
    public static final String CAMERA_ID = SmartPhone.MOD_ID + ":camera";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.camera");
    }

    @Override
    public IGuiTexture getIcon() {
        return PhoneAppIconTextures.camera();
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new CameraUI(homeScreen);
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
