package com.smart.phone.client.camera;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.PhoneUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** 横握手机的纯 HUD 取景器，不创建 Screen，也不接管玩家移动。 */
@EventBusSubscriber(modid = SmartPhone.MOD_ID, value = Dist.CLIENT)
public final class PhoneCameraHud implements ModularHudLayer {
    public static final ResourceLocation ID = SmartPhone.id("camera");
    public static final PhoneCameraHud INSTANCE = new PhoneCameraHud();
    private static final SpriteTexture FRAME = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/phone_camera_frame.png"));
    private ModularUI ui;

    private PhoneCameraHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, INSTANCE);
    }

    @Override
    public ModularUI getModularUI() {
        if (!PhoneCameraClient.isOverlayVisible()) return null;
        if (ui == null) {
            UIElement root = new UIElement() {
                @Override public void drawBackgroundAdditional(GUIContext context) {
                    draw(context.graphics);
                }
            };
            root.setId("phone_camera_hud");
            root.layout(l -> l.widthPercent(100).heightPercent(100));
            ui = new ModularUI(UI.of(root));
        }
        return ui;
    }

    /** 保留竖屏手机的机身比例，旋转后在屏幕中央等比放大。 */
    public static Layout layoutFor(int width, int height) {
        float ratio = PhoneUI.SHELL_HEIGHT / PhoneUI.SHELL_WIDTH;
        int bodyWidth = Math.round(Math.min(width * .88f, height * .78f * ratio));
        int bodyHeight = Math.round(bodyWidth / ratio);
        Rect body = new Rect((width - bodyWidth) / 2, (height - bodyHeight) / 2, bodyWidth, bodyHeight);
        // Coordinates of the transparent screen opening in the rotated bezel texture.
        Rect glass = new Rect(body.x + Math.round(bodyWidth * 123f / 1424f),
                body.y + Math.round(bodyHeight * 75f / 808f),
                Math.round(bodyWidth * 1206f / 1424f), Math.round(bodyHeight * 658f / 808f));
        int bar = Math.max(12, Math.round(bodyHeight * .065f));
        Rect viewport = new Rect(glass.x, glass.y + bar, glass.width, Math.max(1, glass.height - bar * 2));
        return new Layout(width, height, body, glass, viewport, bar);
    }

    private static void draw(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Layout layout = layoutFor(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        Rect body = layout.body;
        Rect glass = layout.glass;
        Rect viewport = layout.viewport;
        PhoneCameraViewfinder.draw(graphics, layout, PhoneCameraClient.zoom());
        // Keep the enlarged scene on the shutter frame; omit only the phone and its decorations.
        if (PhoneCameraClient.isCaptureFrame()) return;
        FRAME.draw(graphics, 0, 0, body.x, body.y, body.width, body.height, 0);
        int bar = layout.barHeight;
        graphics.fill(glass.x, glass.y, glass.right(), glass.y + bar, 0xD9111416);
        graphics.fill(glass.x, glass.bottom() - bar, glass.right(), glass.bottom(), 0xD9111416);
        text(graphics, Component.translatable("smartPhone.ui.app.camera.heading", "%.1f×".formatted(PhoneCameraClient.zoom())),
                glass.x + 4, glass.y, glass.width - 8, bar, false);
        text(graphics, PhoneCameraClient.statusText(), glass.x + 4, glass.bottom() - bar, glass.width - 8, bar, true);
        int focusW = Math.round(viewport.width * .24f);
        int focusH = Math.round(viewport.height * .28f);
        int cx = viewport.x + viewport.width / 2;
        int cy = viewport.y + viewport.height / 2;
        int corner = Math.max(5, focusH / 3);
        corner(graphics, cx - focusW / 2, cy - focusH / 2, corner, 1, 1);
        corner(graphics, cx + focusW / 2, cy - focusH / 2, corner, -1, 1);
        corner(graphics, cx - focusW / 2, cy + focusH / 2, corner, 1, -1);
        corner(graphics, cx + focusW / 2, cy + focusH / 2, corner, -1, -1);
        graphics.fill(cx - 2, cy, cx + 3, cy + 1, 0xAAFFFFFF);
        graphics.fill(cx, cy - 2, cx + 1, cy + 3, 0xAAFFFFFF);
        drawHands(graphics, layout);
    }

    private static void text(GuiGraphics graphics, Component text, int x, int y, int width, int height, boolean centered) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1f, Math.min((height - 4f) / font.lineHeight, width / (float) Math.max(1, font.width(text))));
        graphics.pose().pushPose();
        graphics.pose().translate(centered ? x + (width - font.width(text) * scale) / 2 : x,
                y + (height - font.lineHeight * scale) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }

    private static void corner(GuiGraphics graphics, int x, int y, int length, int dx, int dy) {
        graphics.fill(Math.min(x, x + dx * length), y, Math.max(x, x + dx * length) + 1, y + 1, 0xCCFFFFFF);
        graphics.fill(x, Math.min(y, y + dy * length), x + 1, Math.max(y, y + dy * length) + 1, 0xCCFFFFFF);
    }

    private static void drawHands(GuiGraphics graphics, Layout layout) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isInvisible()) return;
        var renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        graphics.flush();
        Lighting.setupForEntityInInventory();
        try {
            for (boolean right : new boolean[]{false, true}) {
                var pose = graphics.pose();
                pose.pushPose();
                try {
                    float scale = layout.body.height * .72f;
                    pose.translate(layout.body.x + layout.body.width * (right ? .92f : .08f),
                            layout.body.bottom() - layout.body.height * .075f, 180);
                    pose.scale(scale, scale, -scale);
                    pose.mulPose(Axis.ZP.rotationDegrees(right ? 155f : -155f));
                    pose.mulPose(Axis.YP.rotationDegrees(right ? -18f : 18f));
                    // Center the fingertips, letting each forearm extend down and out of the frame.
                    pose.translate(right ? .375f : -.375f, -.75f, 0);
                    int light = mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, 0);
                    if (right) renderer.renderRightHand(pose, graphics.bufferSource(), light, mc.player);
                    else renderer.renderLeftHand(pose, graphics.bufferSource(), light, mc.player);
                    graphics.flush();
                } finally { pose.popPose(); }
            }
        } finally { Lighting.setupFor3DItems(); }
    }

    public record Layout(int screenWidth, int screenHeight, Rect body, Rect glass, Rect viewport, int barHeight) {}
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
    }
}
