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
}
