package com.smart.phone.network.c2s;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.smart.phone.SmartPhone;
import com.smart.phone.compat.voicechat.CallManager;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneServerUtil;

import java.util.UUID;

public class C2SPayload {
    private static final String MOD_ID = SmartPhone.MOD_ID + ":";
    public static final String SAVE_PHONE_INFO = MOD_ID + "save_phone_info";
    public static final String CALL_DIAL = MOD_ID + "call_dial";
    public static final String CALL_ANSWER = MOD_ID + "call_answer";
    public static final String CALL_REJECT = MOD_ID + "call_reject";
    public static final String CALL_HANGUP = MOD_ID + "call_hangup";
    public static final String CALL_REQUEST_STATUS = MOD_ID + "call_request_status";
    public static final String OFFICIAL_MESSAGE_MARK_READ = MOD_ID + "official_message_mark_read";
    public static final String OFFICIAL_MESSAGE_DELETE = MOD_ID + "official_message_delete";
    public static final String CHAT_ROOM_REQUEST_LIST = MOD_ID + "chat_room_request_list";
    public static final String CHAT_ROOM_OPEN = MOD_ID + "chat_room_open";
    public static final String CHAT_ROOM_SEND = MOD_ID + "chat_room_send";
    public static final String FRIEND_LIST_REQUEST = MOD_ID + "friend_list_request";
    public static final String FRIEND_REQUEST = MOD_ID + "friend_request";
    public static final String FRIEND_ACCEPT = MOD_ID + "friend_accept";
    public static final String FRIEND_REMOVE = MOD_ID + "friend_remove";
    public static final String DIRECT_CHAT_OPEN = MOD_ID + "direct_chat_open";
    public static final String CHAT_ROOM_SEND_IMAGE = MOD_ID + "chat_room_send_image";

    @RPCPacket(SAVE_PHONE_INFO)
    public static void savePhoneInfo(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneServerUtil.setPhoneInfoByPlayer(sender.asPlayer(), phoneInfo);
    }

    @RPCPacket(CALL_DIAL)
    public static void callDial(RPCSender sender, UUID targetUuid) {
        CallManager.getInstance().dial(sender.asPlayer(), targetUuid);
    }

    @RPCPacket(CALL_ANSWER)
    public static void callAnswer(RPCSender sender, UUID sessionId) {
        CallManager.getInstance().answer(sender.asPlayer(), sessionId);
    }

    @RPCPacket(CALL_REJECT)
    public static void callReject(RPCSender sender, UUID sessionId) {
        CallManager.getInstance().reject(sender.asPlayer(), sessionId);
    }

    @RPCPacket(CALL_HANGUP)
    public static void callHangup(RPCSender sender) {
        CallManager.getInstance().hangup(sender.asPlayer());
    }

    @RPCPacket(CALL_REQUEST_STATUS)
    public static void callRequestStatus(RPCSender sender) {
        CallManager.getInstance().sendStatus(sender.asPlayer());
    }

    @RPCPacket(OFFICIAL_MESSAGE_MARK_READ)
    public static void officialMessageMarkRead(RPCSender sender, UUID messageId) {
        SmartPhoneServerUtil.markOfficialMessageRead(sender.asPlayer(), messageId);
    }

    @RPCPacket(OFFICIAL_MESSAGE_DELETE)
    public static void officialMessageDelete(RPCSender sender, UUID messageId) {
        SmartPhoneServerUtil.deleteOfficialMessage(sender.asPlayer(), messageId);
    }

    @RPCPacket(CHAT_ROOM_REQUEST_LIST)
    public static void chatRoomRequestList(RPCSender sender) {
        SmartPhoneServerUtil.requestChatRooms(sender.asPlayer());
    }

    @RPCPacket(CHAT_ROOM_OPEN)
    public static void chatRoomOpen(RPCSender sender, String roomId) {
        SmartPhoneServerUtil.openChatRoom(sender.asPlayer(), roomId);
    }

    @RPCPacket(CHAT_ROOM_SEND)
    public static void chatRoomSend(RPCSender sender, String roomId, String body) {
        SmartPhoneServerUtil.sendChatRoomMessage(sender.asPlayer(), roomId, body);
    }

    @RPCPacket(FRIEND_LIST_REQUEST)
    public static void friendListRequest(RPCSender sender) {
        SmartPhoneServerUtil.requestFriendList(sender.asPlayer());
    }

    @RPCPacket(FRIEND_REQUEST)
    public static void friendRequest(RPCSender sender, UUID targetUuid) {
        SmartPhoneServerUtil.requestFriend(sender.asPlayer(), targetUuid);
    }

    @RPCPacket(FRIEND_ACCEPT)
    public static void friendAccept(RPCSender sender, UUID targetUuid) {
        SmartPhoneServerUtil.acceptFriend(sender.asPlayer(), targetUuid);
    }

    @RPCPacket(FRIEND_REMOVE)
    public static void friendRemove(RPCSender sender, UUID targetUuid) {
        SmartPhoneServerUtil.removeFriend(sender.asPlayer(), targetUuid);
    }

    @RPCPacket(DIRECT_CHAT_OPEN)
    public static void directChatOpen(RPCSender sender, UUID targetUuid) {
        SmartPhoneServerUtil.openDirectChatRoom(sender.asPlayer(), targetUuid);
    }

    @RPCPacket(CHAT_ROOM_SEND_IMAGE)
    public static void chatRoomSendImage(RPCSender sender, String roomId, String imageDataBase64) {
        byte[] imageData = java.util.Base64.getDecoder().decode(imageDataBase64);
        SmartPhoneServerUtil.sendChatRoomImage(sender.asPlayer(), roomId, imageData);
    }
}
