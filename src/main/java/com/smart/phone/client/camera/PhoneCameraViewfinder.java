package com.smart.phone.client.camera;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

/** Magnifies the live scene only inside the phone, without changing the player's world FOV. */
final class PhoneCameraViewfinder {
    private static TextureTarget scene;

    private PhoneCameraViewfinder() {}

    static void draw(GuiGraphics graphics, PhoneCameraHud.Layout layout, float zoom) {
        if (zoom <= 1f) return;
        graphics.flush();
        var main = Minecraft.getInstance().getMainRenderTarget();
        var viewport = layout.viewport();
        int x = Math.round(viewport.x() * main.width / (float) layout.screenWidth());
        int y = Math.round(viewport.y() * main.height / (float) layout.screenHeight());
        int width = Math.round(viewport.width() * main.width / (float) layout.screenWidth());
        int height = Math.round(viewport.height() * main.height / (float) layout.screenHeight());
        int bottom = main.height - y - height; // Framebuffer coordinates start at the bottom left.

        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try {
            // The intermediate image prevents reading from the framebuffer we're writing to.
            // Copy before drawing the frame, hints or hands so they can never feed back into it.
            if (scissor) RenderSystem.disableScissor();
            ensureSize(width, height);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GlStateManager._glBlitFrameBuffer(x, bottom, x + width, bottom + height,
                    0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            int sourceWidth = Math.max(1, Math.round(width / zoom));
            int sourceHeight = Math.max(1, Math.round(height / zoom));
            int sourceX = (width - sourceWidth) / 2;
            int sourceY = (height - sourceHeight) / 2;
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scene.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GlStateManager._glBlitFrameBuffer(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight,
                    x, bottom, x + width, bottom + height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            if (scissor) GlStateManager._enableScissorTest();
        }
    }

    private static void ensureSize(int width, int height) {
        if (scene != null && scene.width == width && scene.height == height) return;
        // RenderTarget allocation changes the viewport, texture binding and depth-test state.
        // Preserve them so a window resize cannot affect the following HUD layers.
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try (var stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            try {
                if (scene == null) scene = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                else scene.resize(width, height, Minecraft.ON_OSX);
            } finally {
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GlStateManager._bindTexture(texture);
                if (!depth) RenderSystem.disableDepthTest();
            }
        }
    }

    static void release() {
        if (scene == null) return;
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        scene.destroyBuffers();
        scene = null;
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
    }
}
