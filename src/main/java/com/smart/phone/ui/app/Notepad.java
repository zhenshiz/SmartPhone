package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.NotepadUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@LDLRegister(name = Notepad.NOTEPAD_ID, registry = IApp.ID)
public class Notepad extends IApp {
    public static final String NOTEPAD_ID = SmartPhone.MOD_ID + ":notepad";

    @Override
    public ResourceLocation getPhoneId() {
        return ResourceLocation.parse(NOTEPAD_ID);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.notepad");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/notepad.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new NotepadUI(homeScreen);
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
