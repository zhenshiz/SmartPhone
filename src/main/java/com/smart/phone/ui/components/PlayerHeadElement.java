package com.smart.phone.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public class PlayerHeadElement extends UIElement {
    public PlayerHeadElement(float size) {
        super();
        layout(layout -> {
            layout.setWidth(size);
            layout.setHeight(size);
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        RenderSystem.depthMask(false);
        guiContext.graphics.drawManaged(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;

            if (player != null) {
                ResourceLocation skin = player.getSkin().texture();
                var x = (int) getPositionX();
                var y = (int) getPositionY();
                var size = (int) getSizeWidth();

                guiContext.graphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
            }
        });
        RenderSystem.depthMask(true);
    }
}
