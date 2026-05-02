package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.client.chat.PhoneChatClientState;
import com.smart.phone.client.message.PhoneMessageClientState;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.client.call.PhoneCallClientState;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.SettingUI;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.app.PhoneCall;
import com.smart.phone.ui.app.ui.ChatRoomUI;
import com.smart.phone.ui.app.ui.OfficialMessagesUI;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.OfficialMessagesData;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.chat.ChatRoomListSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSnapshot;
import com.smart.phone.ui.data.social.FriendListSnapshot;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

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

    public static void openUnlockedPhone(PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        PhoneUI phoneUI = new PhoneUI(phoneInfo);
        phoneUI.screenContainer.removeChild(phoneUI.lockScreen);
        ModularUI modularUI = new ModularUI(UI.of(phoneUI, PhoneUI::getAutoGuiScaledSize));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    public static void openPhoneApp(PhoneInfo phoneInfo, IApp app) {
        phoneInfo.ensureDefaultContent();
        PhoneUI phoneUI = new PhoneUI(phoneInfo);
        phoneUI.screenContainer.removeChild(phoneUI.lockScreen);
        phoneUI.homeScreen.openApp(app);
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

    public static void receiveOfficialMessage(OfficialMessage message) {
        if (message == null) return;
        PhoneMessageClientState.receive(message);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.1f, 0.45f));
        mergeOfficialMessage(message);
    }

    private static void mergeOfficialMessage(OfficialMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ModularUIScreen screen)) return;
        if (!(screen.getModularUI().ui.rootElement instanceof PhoneUI phoneUI)) return;
        phoneUI.phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(message);
        if (phoneUI.homeScreen.appUI instanceof OfficialMessagesUI officialMessagesUI) {
            officialMessagesUI.refreshFromData();
        }
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

    public static void markOfficialMessageRead(UUID messageId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.OFFICIAL_MESSAGE_MARK_READ, messageId);
    }

    public static void deleteOfficialMessage(UUID messageId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.OFFICIAL_MESSAGE_DELETE, messageId);
    }

    public static void requestChatRooms() {
        RPCPacketDistributor.rpcToServer(C2SPayload.CHAT_ROOM_REQUEST_LIST);
    }

    public static void openChatRoom(String roomId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.CHAT_ROOM_OPEN, roomId);
    }

    public static void sendChatRoomMessage(String roomId, String body) {
        RPCPacketDistributor.rpcToServer(C2SPayload.CHAT_ROOM_SEND, roomId, body);
    }

    public static void sendChatRoomImage(String roomId, byte[] imageData) {
        String base64 = java.util.Base64.getEncoder().encodeToString(imageData);
        RPCPacketDistributor.rpcToServer(C2SPayload.CHAT_ROOM_SEND_IMAGE, roomId, base64);
    }

    public static void requestFriendList() {
        RPCPacketDistributor.rpcToServer(C2SPayload.FRIEND_LIST_REQUEST);
    }

    public static void requestFriend(UUID targetUuid) {
        RPCPacketDistributor.rpcToServer(C2SPayload.FRIEND_REQUEST, targetUuid);
    }

    public static void acceptFriend(UUID targetUuid) {
        RPCPacketDistributor.rpcToServer(C2SPayload.FRIEND_ACCEPT, targetUuid);
    }

    public static void removeFriend(UUID targetUuid) {
        RPCPacketDistributor.rpcToServer(C2SPayload.FRIEND_REMOVE, targetUuid);
    }

    public static void openDirectChat(UUID targetUuid) {
        RPCPacketDistributor.rpcToServer(C2SPayload.DIRECT_CHAT_OPEN, targetUuid);
    }

    public static void receiveChatRoomList(ChatRoomListSnapshot snapshot) {
        PhoneChatClientState.receiveRoomList(snapshot);
        refreshChatRoomUI();
    }

    public static void receiveChatRoomSnapshot(ChatRoomSnapshot snapshot) {
        PhoneChatClientState.receiveRoom(snapshot);
        refreshChatRoomUI();
    }

    public static void receiveChatRoomMessage(ChatRoomMessage message) {
        PhoneChatClientState.receiveMessage(message);
        refreshChatRoomUI();
    }

    public static void receiveFriendList(FriendListSnapshot snapshot) {
        PhoneChatClientState.receiveFriendList(snapshot);
        refreshChatRoomUI();
    }

    public static void receiveFriendToast(String translationKey, String targetName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey, targetName == null ? "" : targetName), true);
        }
        requestFriendList();
    }

    private static void refreshChatRoomUI() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ModularUIScreen screen)) return;
        if (!(screen.getModularUI().ui.rootElement instanceof PhoneUI phoneUI)) return;
        if (phoneUI.homeScreen.appUI instanceof ChatRoomUI chatRoomUI) {
            chatRoomUI.refreshFromState();
        }
    }
}
