package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.PhotoAlbumUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = PhotoAlbumApp.PHOTO_ALBUM_ID, registry = IApp.ID)
public class PhotoAlbumApp extends IApp {
    public static final String PHOTO_ALBUM_ID = SmartPhone.MOD_ID + ":photo_album";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.photoAlbum");
    }

    @Override
    public IGuiTexture getIcon() {
        return PhoneAppIconTextures.album();
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new PhotoAlbumUI(homeScreen);
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
