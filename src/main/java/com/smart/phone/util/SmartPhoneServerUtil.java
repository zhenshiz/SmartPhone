package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.SmartPhone;
import com.smart.phone.network.s2c.S2CPayload;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.OfficialMessagesData;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSavedData;
import com.smart.phone.ui.data.chat.ChatRoomSnapshot;
import com.smart.phone.ui.data.social.PhoneSocialSavedData;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.UUID;

@KJSBindings
public class SmartPhoneServerUtil {
    private static final int MAX_CHAT_MESSAGE_LENGTH = 160;
    // 聊天图片最大 100KB
    private static final int MAX_IMAGE_DATA_SIZE = 100 * 1024;

    @Info("打开手机")
    public static void openPhone(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_PHONE, SmartPhone.getPhoneSavedData().getPhoneInfo(player));
    }

    @Info("重置本地玩家信息")
    public static void reload(ServerPlayer player) {
        SmartPhone.getPhoneSavedData().resetPhoneInfo(player);
    }

    @Info("更新玩家手机信息")
    public static void setPhoneInfoByPlayer(ServerPlayer player, PhoneInfo phoneInfo) {
        PhoneSavedData savedData = SmartPhone.getPhoneSavedData();
        OfficialMessagesData serverMessages = savedData.getPhoneInfo(player).getOrCreateExtensionData(OfficialMessagesData.class);
        phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).setMessages(serverMessages.getMessages());
        savedData.setPhoneInfo(player, phoneInfo);
    }

    @Info("打开配置文件")
    public static void openSetting(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SETTING, SmartPhone.getPhoneSavedData().getPhoneInfo(player));
    }

    @Info("发送官方消息")
    public static void sendOfficialMessage(ServerPlayer player, String title, String body) {
        sendOfficialMessage(player, "", title, body);
    }

    @Info("发送带发送者名称的官方消息")
    public static void sendOfficialMessage(ServerPlayer player, String sender, String title, String body) {
        if (player == null) return;
        OfficialMessage message = new OfficialMessage(sender, title, body);
        PhoneInfo phoneInfo = SmartPhone.getPhoneSavedData().getPhoneInfo(player);
        phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(message);
        SmartPhone.getPhoneSavedData().setPhoneInfo(player, phoneInfo);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OFFICIAL_MESSAGE_RECEIVED, message);
    }

    public static void sendOfficialMessage(Collection<ServerPlayer> players, String title, String body) {
        players.forEach(player -> sendOfficialMessage(player, title, body));
    }

    public static void markOfficialMessageRead(ServerPlayer player, UUID messageId) {
        if (player == null || messageId == null) return;
        PhoneInfo phoneInfo = SmartPhone.getPhoneSavedData().getPhoneInfo(player);
        phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).markRead(messageId);
        SmartPhone.getPhoneSavedData().setPhoneInfo(player, phoneInfo);
    }

    public static void deleteOfficialMessage(ServerPlayer player, UUID messageId) {
        if (player == null || messageId == null) return;
        PhoneInfo phoneInfo = SmartPhone.getPhoneSavedData().getPhoneInfo(player);
        if (phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).deleteMessage(messageId)) {
            SmartPhone.getPhoneSavedData().setPhoneInfo(player, phoneInfo);
        }
    }

    public static void requestChatRooms(ServerPlayer player) {
        ChatRoomSavedData chatRoomSavedData = getChatRoomSavedData(player);
        if (player == null || chatRoomSavedData == null) return;
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CHAT_ROOM_LIST_UPDATE, chatRoomSavedData.createListSnapshot());
    }

    public static void openChatRoom(ServerPlayer player, String roomId) {
        ChatRoomSavedData chatRoomSavedData = getChatRoomSavedData(player);
        if (player == null || chatRoomSavedData == null) return;
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CHAT_ROOM_SNAPSHOT, chatRoomSavedData.createRoomSnapshot(roomId));
    }

    public static void sendChatRoomMessage(ServerPlayer player, String roomId, String body) {
        ChatRoomSavedData chatRoomSavedData = getChatRoomSavedData(player);
        if (player == null || chatRoomSavedData == null) return;
        String normalizedBody = normalizeChatBody(body);
        if (normalizedBody.isBlank()) return;
        if (!canUseChatRoom(player, roomId)) return;
        ChatRoomMessage message = chatRoomSavedData.addPlayerMessage(roomId, player.getUUID(), player.getGameProfile().getName(), normalizedBody);
        sendChatMessageToRecipients(player, message);
    }

    public static void sendChatRoomImage(ServerPlayer player, String roomId, byte[] imageData) {
        ChatRoomSavedData chatRoomSavedData = getChatRoomSavedData(player);
        if (player == null || chatRoomSavedData == null) return;
        if (imageData == null || imageData.length == 0 || imageData.length > MAX_IMAGE_DATA_SIZE) return;
        if (!canUseChatRoom(player, roomId)) return;
        ChatRoomMessage message = chatRoomSavedData.addPlayerImageMessage(roomId, player.getUUID(), player.getGameProfile().getName(), imageData);
        sendChatMessageToRecipients(player, message);
    }

    public static void requestFriendList(ServerPlayer player) {
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        if (player == null || socialSavedData == null) return;
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.FRIEND_LIST_UPDATE, socialSavedData.createSnapshot(player));
    }

    public static void requestFriend(ServerPlayer player, UUID targetUuid) {
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        ServerPlayer target = findOnlinePlayer(player, targetUuid);
        if (player == null || socialSavedData == null || target == null) return;
        if (socialSavedData.requestFriend(player, target)) {
            sendFriendUpdates(player, target, "smartPhone.ui.app.chatRoom.friend.requestSent", "smartPhone.ui.app.chatRoom.friend.requestReceived");
        }
    }

    public static void acceptFriend(ServerPlayer player, UUID targetUuid) {
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        if (player == null || socialSavedData == null || targetUuid == null) return;
        String targetName = socialSavedData.findRecord(player.getUUID(), targetUuid)
                .map(record -> record.getTargetName() == null ? "" : record.getTargetName())
                .orElse("");
        if (socialSavedData.acceptFriend(player, targetUuid)) {
            requestFriendList(player);
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.FRIEND_TOAST, "smartPhone.ui.app.chatRoom.friend.accepted", targetName);
            ServerPlayer target = findOnlinePlayer(player, targetUuid);
            if (target != null) {
                requestFriendList(target);
                RPCPacketDistributor.rpcToPlayer(target, S2CPayload.FRIEND_TOAST, "smartPhone.ui.app.chatRoom.friend.acceptedBy", player.getGameProfile().getName());
            }
        }
    }

    public static void removeFriend(ServerPlayer player, UUID targetUuid) {
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        if (player == null || socialSavedData == null || targetUuid == null) return;
        if (socialSavedData.removeFriend(player, targetUuid)) {
            requestFriendList(player);
            ServerPlayer target = findOnlinePlayer(player, targetUuid);
            if (target != null) requestFriendList(target);
        }
    }

    public static void openDirectChatRoom(ServerPlayer player, UUID targetUuid) {
        ChatRoomSavedData chatRoomSavedData = getChatRoomSavedData(player);
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        ServerPlayer target = findOnlinePlayer(player, targetUuid);
        if (player == null || chatRoomSavedData == null || socialSavedData == null || target == null) return;
        if (!socialSavedData.areFriends(player.getUUID(), targetUuid)) return;
        String roomId = ChatRoomSavedData.directRoomId(player.getUUID(), targetUuid);
        ChatRoomSnapshot snapshot = chatRoomSavedData.createRoomSnapshot(roomId);
        snapshot.setDisplayName(target.getGameProfile().getName());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CHAT_ROOM_SNAPSHOT, snapshot);
    }

    private static ChatRoomSavedData getChatRoomSavedData(ServerPlayer player) {
        ChatRoomSavedData data = SmartPhone.getChatRoomSavedData();
        if (data != null) return data;
        if (player == null || player.getServer() == null) return null;
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return null;
        data = overworld.getDataStorage().computeIfAbsent(ChatRoomSavedData.factory(), ChatRoomSavedData.DATA_NAME);
        data.ensureDefaultRooms();
        SmartPhone.setChatRoomSavedData(data);
        return data;
    }

    private static PhoneSocialSavedData getPhoneSocialSavedData(ServerPlayer player) {
        PhoneSocialSavedData data = SmartPhone.getPhoneSocialSavedData();
        if (data != null) return data;
        if (player == null || player.getServer() == null) return null;
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return null;
        data = overworld.getDataStorage().computeIfAbsent(PhoneSocialSavedData.factory(), PhoneSocialSavedData.DATA_NAME);
        SmartPhone.setPhoneSocialSavedData(data);
        return data;
    }

    private static boolean canUseChatRoom(ServerPlayer player, String roomId) {
        if (player == null || !ChatRoomSavedData.isDirectRoom(roomId)) return true;
        if (!ChatRoomSavedData.isDirectParticipant(roomId, player.getUUID())) return false;
        UUID targetUuid = directTarget(roomId, player.getUUID());
        PhoneSocialSavedData socialSavedData = getPhoneSocialSavedData(player);
        return socialSavedData != null && socialSavedData.areFriends(player.getUUID(), targetUuid);
    }

    private static void sendChatMessageToRecipients(ServerPlayer sender, ChatRoomMessage message) {
        if (sender == null || sender.getServer() == null) return;
        if (ChatRoomSavedData.isDirectRoom(message.getRoomId())) {
            sender.getServer().getPlayerList().getPlayers().stream()
                    .filter(target -> ChatRoomSavedData.isDirectParticipant(message.getRoomId(), target.getUUID()))
                    .forEach(target -> RPCPacketDistributor.rpcToPlayer(target, S2CPayload.CHAT_ROOM_MESSAGE, message));
            return;
        }
        sender.getServer().getPlayerList().getPlayers().forEach(target ->
                RPCPacketDistributor.rpcToPlayer(target, S2CPayload.CHAT_ROOM_MESSAGE, message)
        );
    }

    private static void sendFriendUpdates(ServerPlayer player, ServerPlayer target, String playerToastKey, String targetToastKey) {
        requestFriendList(player);
        requestFriendList(target);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.FRIEND_TOAST, playerToastKey, target.getGameProfile().getName());
        RPCPacketDistributor.rpcToPlayer(target, S2CPayload.FRIEND_TOAST, targetToastKey, player.getGameProfile().getName());
    }

    private static ServerPlayer findOnlinePlayer(ServerPlayer requester, UUID targetUuid) {
        if (requester == null || requester.getServer() == null || targetUuid == null) return null;
        return requester.getServer().getPlayerList().getPlayer(targetUuid);
    }

    private static UUID directTarget(String roomId, UUID self) {
        if (roomId == null || self == null || !ChatRoomSavedData.isDirectRoom(roomId)) return null;
        String[] parts = roomId.substring(ChatRoomSavedData.DIRECT_ROOM_PREFIX.length()).split(":");
        if (parts.length != 2) return null;
        try {
            UUID first = UUID.fromString(parts[0]);
            UUID second = UUID.fromString(parts[1]);
            return self.equals(first) ? second : first;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizeChatBody(String body) {
        if (body == null) return "";
        String normalized = body.replace('\n', ' ').trim();
        if (normalized.length() <= MAX_CHAT_MESSAGE_LENGTH) return normalized;
        return normalized.substring(0, MAX_CHAT_MESSAGE_LENGTH);
    }
}
