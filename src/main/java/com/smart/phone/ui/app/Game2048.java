package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.Game2048UI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = Game2048.GAME_2048, registry = IApp.ID)
public class Game2048 extends IApp {
    public static final String GAME_2048 = SmartPhone.MOD_ID + ":game_2048";


    @Override
    public Component getDisplayName() {
        return Component.literal("2048");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("smartPhone.ui.app.game2048.description");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/2048.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new Game2048UI(homeScreen);
    }
}
