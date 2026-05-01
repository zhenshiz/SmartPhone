package com.smart.phone.event;

import com.smart.phone.SmartPhone;
import com.smart.phone.client.message.PhoneMessageClientState;
import com.smart.phone.ui.data.OfficialMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = SmartPhone.MOD_ID, value = Dist.CLIENT)
public class PhoneClientEvent {
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

    private static String clip(String text, int maxLength) {
        String normalized = text == null ? "" : text.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
