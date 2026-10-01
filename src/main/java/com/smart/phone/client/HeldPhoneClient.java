package com.smart.phone.client;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.Config;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.client.camera.PhoneCameraClient;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.client.Minecraft;

public final class HeldPhoneClient {
    private static final int REQUEST_RETRY_TICKS = 5;
    private static boolean shownForCurrentHold;
    private static int ticksUntilRequest;

    private HeldPhoneClient() {
    }

    public static boolean isPhoneInMainHand() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getMainHandItem().is(SmartPhoneRegistries.PHONE.get());
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !isPhoneInMainHand() || !Config.HELD_PHONE_MODE.get()) {
            shownForCurrentHold = false;
            ticksUntilRequest = 0;
            if (minecraft.screen instanceof HeldPhoneScreen) minecraft.setScreen(null);
            return;
        }
        if (minecraft.screen instanceof HeldPhoneScreen screen) {
            var owner = com.smart.phone.PhoneItem.ownerOf(minecraft.player.getMainHandItem(), minecraft.player.getUUID());
            if (screen.isSynced() && !owner.equals(screen.getPhoneUI().getOwnerUuid())) {
                minecraft.setScreen(null);
                shownForCurrentHold = false;
                return;
            }
            shownForCurrentHold = true;
            if (!screen.isSynced() && --ticksUntilRequest <= 0) {
                RPCPacketDistributor.rpcToServer(C2SPayload.REQUEST_HELD_PHONE);
                ticksUntilRequest = REQUEST_RETRY_TICKS;
            }
            return;
        }
        if (shownForCurrentHold || minecraft.screen != null || PhoneCameraClient.shouldHideFirstPersonHand()) return;
        shownForCurrentHold = true;
        SmartPhoneClientUtil.showHeldPhoneImmediately();
        RPCPacketDistributor.rpcToServer(C2SPayload.REQUEST_HELD_PHONE);
        ticksUntilRequest = REQUEST_RETRY_TICKS;
    }
}
