package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.Config;
import com.smart.phone.PhoneItem;
import com.smart.phone.client.HeldPhoneClient;
import com.smart.phone.client.chat.PhoneChatClientState;
import com.smart.phone.client.message.PhoneMessageClientState;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.network.c2s.ChatRoomImagePayload;
import com.smart.phone.client.call.PhoneCallClientState;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.HeldPhoneScreen;
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

    private static UUID activeAccessToken;

    public static void setPhoneOwnerName(UUID owner, String name) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phone && owner.equals(phone.getOwnerUuid())) {
            phone.setOwnerName(name);
        }
    }

    public static void configurePhoneAccess(UUID token, boolean enabled, boolean locked) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phone) {
            activeAccessToken = token;
            phone.configureAccess(token, enabled, locked);
        }
    }

    public static void receivePhoneSecurity(UUID token, String action, boolean success, boolean enabled, PhoneInfo info, String error) {
        if (com.smart.phone.client.camera.PhoneCameraClient.receiveSecurity(token, action, success, enabled, info, error)) return;
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phone
                && token.equals(phone.getAccessToken())) phone.securityResult(action, success, enabled, info, error);
    }

    public static void tickPhoneAccess() {
        if (activeAccessToken == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { activeAccessToken = null; return; }
        if (com.smart.phone.client.camera.PhoneCameraClient.isCameraActive()) return;
        if (mc.screen instanceof ModularUIScreen screen && screen.getModularUI().ui.rootElement instanceof PhoneUI phone
                && activeAccessToken.equals(phone.getAccessToken())) return;
        RPCPacketDistributor.rpcToServer(C2SPayload.CLOSE_PHONE, activeAccessToken);
        activeAccessToken = null;
    }

    @Info("打开手机")
    public static void openPhone(PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        displayPhone(phoneInfo, false, null, Minecraft.getInstance().player.getUUID());
    }

    public static void openPhone(UUID ownerUuid, PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        displayPhone(phoneInfo, false, null, ownerUuid);
    }

    public static void openHeldPhone(UUID ownerUuid, PhoneInfo phoneInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Config.HELD_PHONE_MODE.get() || !HeldPhoneClient.isPhoneInMainHand()
                || !(minecraft.screen instanceof HeldPhoneScreen heldScreen)) return;
        phoneInfo.ensureDefaultContent();
        PhoneUI phoneUI = heldScreen.getPhoneUI();
        phoneUI.phoneInfo = phoneInfo;
        phoneUI.setOwnerUuid(ownerUuid);
        phoneUI.homeScreen.reloadAppView();
        heldScreen.markSynced();
    }

    public static void closeRejectedHeldPhone() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HeldPhoneScreen) minecraft.setScreen(null);
    }

    public static void showHeldPhoneImmediately() {
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.ensureDefaultContent();
        Minecraft minecraft = Minecraft.getInstance();
        displayPhone(phoneInfo, false, null, PhoneItem.ownerOf(
                minecraft.player.getMainHandItem(), minecraft.player.getUUID()));
        if (Minecraft.getInstance().screen instanceof HeldPhoneScreen heldScreen) {
            heldScreen.awaitSync();
        }
    }

    public static void openUnlockedPhone(PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        displayPhone(phoneInfo, true, null, Minecraft.getInstance().player.getUUID());
    }

    public static void openPhoneApp(PhoneInfo phoneInfo, IApp app) {
        phoneInfo.ensureDefaultContent();
        displayPhone(phoneInfo, true, app, Minecraft.getInstance().player.getUUID());
    }

    @Info("更新玩家手机信息")
    public static void setPhoneInfoByPlayer(PhoneInfo phoneInfo) {
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, currentPhoneOwner(), phoneInfo);
    }

    public static void sendPresetChat(String roomId, String body, byte[] imageData) {
        RPCPacketDistributor.rpcToServer(C2SPayload.SEND_PRESET_CHAT, currentPhoneOwner(), body,
                new com.smart.phone.network.c2s.ChatRoomImagePayload(roomId, imageData));
    }

    public static void refreshPhoneInfo(UUID owner, PhoneInfo info) {
        if (com.smart.phone.client.camera.PhoneCameraClient.refreshPhoneInfo(owner, info)) return;
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phone
                && owner.equals(phone.getOwnerUuid())) {
            phone.phoneInfo = info;
            if (phone.homeScreen.appUI instanceof ChatRoomUI chat) chat.refreshFromState();
            else if (phone.homeScreen.iApp != null) phone.homeScreen.openApp(phone.homeScreen.iApp);
            phone.homeScreen.reloadAppView();
        }
    }

    public static void openPhoneCall(PhoneInfo phoneInfo, UUID sessionId, UUID callerUuid, String callerName) {
        phoneInfo.ensureDefaultContent();
        PhoneCallClientState.incoming(sessionId, callerUuid, callerName);
        if (phoneInfo.isBlocked()) return;
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phone && phone.isAccessLocked()) return;
        displayPhone(phoneInfo, true, new PhoneCall(), Minecraft.getInstance().player.getUUID());
    }

    private static void displayPhone(PhoneInfo phoneInfo, boolean unlocked, IApp app, UUID ownerUuid) {
        boolean heldMode = Config.HELD_PHONE_MODE.get() && HeldPhoneClient.isPhoneInMainHand();
        PhoneUI phoneUI = new PhoneUI(phoneInfo, heldMode);
        phoneUI.setOwnerUuid(ownerUuid);
        if (unlocked && !phoneInfo.isBlocked()) phoneUI.screenContainer.removeChild(phoneUI.lockScreen);
        if (app != null) phoneUI.homeScreen.openApp(app);
        ModularUI modularUI = new ModularUI(com.smart.phone.ui.PhoneTheme.create(phoneUI, PhoneUI::getAutoGuiScaledSize));
        Minecraft.getInstance().setScreen(heldMode
                ? new HeldPhoneScreen(modularUI, phoneUI)
                : new ModularUIScreen(modularUI, Component.empty()));
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
        RPCPacketDistributor.rpcToServer(C2SPayload.OFFICIAL_MESSAGE_MARK_READ, currentPhoneOwner(), messageId);
    }

    public static void deleteOfficialMessage(UUID messageId) {
        RPCPacketDistributor.rpcToServer(C2SPayload.OFFICIAL_MESSAGE_DELETE, currentPhoneOwner(), messageId);
    }

    private static UUID currentPhoneOwner() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneUI phoneUI
                && phoneUI.getOwnerUuid() != null) return phoneUI.getOwnerUuid();
        return minecraft.player.getUUID();
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
        RPCPacketDistributor.rpcToServer(C2SPayload.CHAT_ROOM_SEND_IMAGE, new ChatRoomImagePayload(roomId, imageData));
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
