package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.MinesweeperUI;
import com.smart.phone.ui.app.ui.PianoTilesUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = Minesweeper.MINESWEEPER_ID, registry = IApp.ID)
public class Minesweeper extends IApp {
    public static final String MINESWEEPER_ID = SmartPhone.MOD_ID + ":minesweeper";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.minesweeper");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/minesweeper.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new MinesweeperUI(homeScreen);
    }
}
