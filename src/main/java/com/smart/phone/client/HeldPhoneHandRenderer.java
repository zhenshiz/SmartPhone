package com.smart.phone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.smart.phone.Config;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class HeldPhoneHandRenderer {
    private HeldPhoneHandRenderer() {
    }

    public static boolean render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Config.HELD_PHONE_MODE.get() || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(SmartPhoneRegistries.PHONE.get())
                || !(minecraft.screen instanceof HeldPhoneScreen screen) || minecraft.player == null) return false;

        if (minecraft.player.isInvisible()) return true;
        float progress = screen.raiseProgress();
        float eased = 1f - (float) Math.pow(1f - progress, 3);
        float left = Config.PHONE_MARGIN_LEFT.get().floatValue() / 100f;
        float top = Config.PHONE_MARGIN_TOP.get().floatValue() / 100f;
        float screenAspect = (float) minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getGuiScaledWidth();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // 手掌停在手机右下角内侧，前臂从屏幕右下方伸入。
        pose.translate(0.255f + left * 2f + (1f - eased) * 0.24f,
                -0.04f - ((PhoneUI.HEIGHT_SCALE - 1f) * 0.8f + top * 2f) * screenAspect
                        - (1f - eased) * 1.2f, -0.06f);
        pose.mulPose(Axis.ZP.rotationDegrees((1f - eased) * -11f));
        pose.translate(0.64000005f, -0.6f, -0.71999997f);
        pose.mulPose(Axis.YP.rotationDegrees(45f));
        pose.translate(-1f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(120f));
        pose.mulPose(Axis.XP.rotationDegrees(200f));
        pose.mulPose(Axis.YP.rotationDegrees(-135f));
        pose.translate(5.6f, 0f, 0f);
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), minecraft.player);
        pose.popPose();
        return true;
    }
}
