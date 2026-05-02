package com.smart.phone.event;

import com.smart.phone.SmartPhone;
import com.smart.phone.client.camera.PhoneCameraClient;
import com.smart.phone.client.message.PhoneMessageClientState;
import com.smart.phone.ui.data.OfficialMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = SmartPhone.MOD_ID, value = Dist.CLIENT)
public class PhoneClientEvent {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PhoneCameraClient.tick();
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Post event) {
        PhoneCameraClient.captureOnRenderFrame();
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (PhoneCameraClient.renderCameraOverlay(event.getGuiGraphics())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (PhoneCameraClient.shouldHideFirstPersonHand()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!PhoneMessageClientState.hasVisibleNotification()) return;
        OfficialMessage message = PhoneMessageClientState.getNotification();
        if (message == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = 128;
        int height = 36;
        int x = minecraft.getWindow().getGuiScaledWidth() - width - 8;
        int y = 8;
        graphics.fill(x, y, x + width, y + height, 0xCC1F1D24);
        graphics.fill(x, y, x + 3, y + height, 0xFF5EE06E);
        graphics.drawString(minecraft.font, Component.translatable("smartPhone.notification.officialMessage"), x + 8, y + 5, 0xFFFFFFFF, false);
        graphics.drawString(minecraft.font, Component.literal(clip(message.getTitle(), 20)), x + 8, y + 18, 0xFFE6E0EA, false);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        PhoneCameraClient.handleMouseButton(event);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        PhoneCameraClient.handleMouseScroll(event);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        PhoneCameraClient.handleKey(event);
    }

    @SubscribeEvent
    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        PhoneCameraClient.handleInteractionKeyMapping(event);
    }

    private static String clip(String text, int maxLength) {
        String normalized = text == null ? "" : text.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
