package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.client.call.PhoneCallClientState;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.SettingUI;
import com.smart.phone.ui.app.PhoneCall;
import com.smart.phone.ui.data.PhoneInfo;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

@KJSBindings(clientOnly = true)
public class SmartPhoneClientUtil {

    @Info("打开手机")
    public static void openPhone(PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        PhoneUI phoneUI = new PhoneUI(phoneInfo);
        ModularUI modularUI = new ModularUI(UI.of(phoneUI, PhoneUI::getAutoGuiScaledSize));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    @Info("更新玩家手机信息")
    public static void setPhoneInfoByPlayer(PhoneInfo phoneInfo) {
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, phoneInfo);
    }

    @Info("打开配置文件")
    public static void openSetting(PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        SettingUI settingUI = new SettingUI(phoneInfo);
        ModularUI modularUI = new ModularUI(UI.of(settingUI, SettingUI::getAutoGuiScaledSize));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    public static void openPhoneCall(PhoneInfo phoneInfo, UUID sessionId, UUID callerUuid, String callerName) {
        phoneInfo.ensureDefaultContent();
        PhoneCallClientState.incoming(sessionId, callerUuid, callerName);
        PhoneUI phoneUI = new PhoneUI(phoneInfo);
        phoneUI.screenContainer.removeChild(phoneUI.lockScreen);
        phoneUI.homeScreen.openApp(new PhoneCall());
        ModularUI modularUI = new ModularUI(UI.of(phoneUI, PhoneUI::getAutoGuiScaledSize));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    public static void callRinging(UUID sessionId, UUID calleeUuid, String calleeName) {
        PhoneCallClientState.ringing(sessionId, calleeUuid, calleeName);
    }

    public static void callConnected(UUID sessionId, UUID remoteUuid, String remoteName) {
        PhoneCallClientState.connected(sessionId, remoteUuid, remoteName);
    }

    public static void callEnded(UUID sessionId, String reasonKey) {
        PhoneCallClientState.ended(sessionId, reasonKey);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(reasonKey), true);
        }
    }

    public static void callError(String messageKey) {
        PhoneCallClientState.error(messageKey);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    public static void callStatusUpdate(String encodedStatuses) {
        PhoneCallClientState.updatePlayerStatuses(encodedStatuses);
    }

    public static void dialCall(UUID targetUuid) {
        RPCPacketDistributor.rpcToServer(C2SPayload.CALL_DIAL, targetUuid);
    }

    public static void answerCall(UUID sessionId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.CALL_ANSWER, sessionId);
    }

    public static void rejectCall(UUID sessionId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.CALL_REJECT, sessionId);
    }

    public static void hangupCall() {
        RPCPacketDistributor.rpcToServer(C2SPayload.CALL_HANGUP);
    }

    public static void requestCallStatus() {
        RPCPacketDistributor.rpcToServer(C2SPayload.CALL_REQUEST_STATUS);
    }
}
