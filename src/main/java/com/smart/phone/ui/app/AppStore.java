package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.AppStoreUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = AppStore.APP_STORE_ID, registry = IApp.ID)
public class AppStore extends IApp {
    public static final String APP_STORE_ID = SmartPhone.MOD_ID + ":app_store";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.appStore");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/app_store.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new AppStoreUI(homeScreen);
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
