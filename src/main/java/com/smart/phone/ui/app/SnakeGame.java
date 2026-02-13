package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.ui.SnakeGameUI;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@LDLRegister(name = SnakeGame.SNAKE_GAME_ID, registry = IApp.ID)
public class SnakeGame extends IApp {
    public static final String SNAKE_GAME_ID = SmartPhone.MOD_ID + ":snake_game";

    @Override
    public ResourceLocation getPhoneId() {
        return ResourceLocation.parse(SNAKE_GAME_ID);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("smartPhone.ui.app.snakeGame");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("smartPhone.ui.app.snakeGame.description");
    }

    @Override
    public IGuiTexture getIcon() {
        return SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/snake_game.png"));
    }

    @Override
    public UIElement createAppUI(HomeScreen homeScreen) {
        return new SnakeGameUI(homeScreen);
    }
}
