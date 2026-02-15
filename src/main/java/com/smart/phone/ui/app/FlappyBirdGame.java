package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.FlappyBirdUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

@LDLRegister(name = FlappyBirdGame.FLAPPY_BIRD, registry = IApp.ID)
public class FlappyBirdGame extends IApp {
    public static final String FLAPPY_BIRD = SmartPhone.MOD_ID + ":flappy_bird";

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.flappyBird");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/flappy_bird.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new FlappyBirdUI(homeScreen);
    }
}
