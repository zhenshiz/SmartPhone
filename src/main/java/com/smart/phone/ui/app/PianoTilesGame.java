package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.PianoTilesUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = PianoTilesGame.PIANO_TILES, registry = IApp.ID)
public class PianoTilesGame extends IApp {
    public static final String PIANO_TILES = SmartPhone.MOD_ID + ":piano_tiles";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.pianoTiles");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/piano_tiles.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new PianoTilesUI(homeScreen);
    }
}
