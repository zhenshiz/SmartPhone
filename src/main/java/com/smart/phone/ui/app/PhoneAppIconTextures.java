package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

public class PhoneAppIconTextures {
    public static IGuiTexture camera() {
        return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(x + width);
            int bottom = Math.round(y + height);
            int bodyLeft = Math.round(x + width * 0.18f);
            int bodyTop = Math.round(y + height * 0.30f);
            int bodyRight = Math.round(x + width * 0.84f);
            int bodyBottom = Math.round(y + height * 0.78f);
            graphics.fill(left, top, right, bottom, 0xFF23262E);
            graphics.fill(Math.round(x + width * 0.26f), Math.round(y + height * 0.20f), Math.round(x + width * 0.48f), bodyTop, 0xFFEDE8F2);
            graphics.fill(bodyLeft, bodyTop, bodyRight, bodyBottom, 0xFFEDE8F2);
            graphics.fill(Math.round(x + width * 0.38f), Math.round(y + height * 0.40f), Math.round(x + width * 0.64f), Math.round(y + height * 0.66f), 0xFF3D5D7A);
            graphics.fill(Math.round(x + width * 0.46f), Math.round(y + height * 0.48f), Math.round(x + width * 0.56f), Math.round(y + height * 0.58f), 0xFF10131A);
        };
    }

    public static IGuiTexture album() {
        return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
            int left = Math.round(x);
            int top = Math.round(y);
            int right = Math.round(x + width);
            int bottom = Math.round(y + height);
            graphics.fill(left, top, right, bottom, 0xFF2B2633);
            graphics.fill(Math.round(x + width * 0.18f), Math.round(y + height * 0.20f), Math.round(x + width * 0.80f), Math.round(y + height * 0.72f), 0xFFEDE8F2);
            graphics.fill(Math.round(x + width * 0.24f), Math.round(y + height * 0.28f), Math.round(x + width * 0.74f), Math.round(y + height * 0.64f), 0xFF3D5D7A);
            graphics.fill(Math.round(x + width * 0.28f), Math.round(y + height * 0.50f), Math.round(x + width * 0.50f), Math.round(y + height * 0.64f), 0xFF78A66A);
            graphics.fill(Math.round(x + width * 0.46f), Math.round(y + height * 0.42f), Math.round(x + width * 0.74f), Math.round(y + height * 0.64f), 0xFFE0A74F);
            graphics.fill(Math.round(x + width * 0.58f), Math.round(y + height * 0.32f), Math.round(x + width * 0.66f), Math.round(y + height * 0.40f), 0xFFFFF5B8);
        };
    }
}
