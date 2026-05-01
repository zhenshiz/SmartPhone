package com.smart.phone.network.s2c;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;

public class S2CPayload {
    private static final String MOD_ID = SmartPhone.MOD_ID + ":";
    public static final String OPEN_PHONE = MOD_ID + "open_phone";
    public static final String OPEN_SETTING = MOD_ID + "open_setting";
    public static final String CALL_INCOMING = MOD_ID + "call_incoming";
    public static final String CALL_RINGING = MOD_ID + "call_ringing";
    public static final String CALL_CONNECTED = MOD_ID + "call_connected";
    public static final String CALL_ENDED = MOD_ID + "call_ended";
    public static final String CALL_ERROR = MOD_ID + "call_error";
    public static final String CALL_STATUS_UPDATE = MOD_ID + "call_status_update";
    public static final String OFFICIAL_MESSAGE_RECEIVED = MOD_ID + "official_message_received";

    @RPCPacket(OPEN_PHONE)
    public static void openPhone(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneClientUtil.openPhone(phoneInfo);
    }

    @RPCPacket(OPEN_SETTING)
    public static void openSetting(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneClientUtil.openSetting(phoneInfo);
    }

    @RPCPacket(CALL_INCOMING)
    public static void callIncoming(RPCSender sender, PhoneInfo phoneInfo, java.util.UUID sessionId, java.util.UUID callerUuid, String callerName) {
        SmartPhoneClientUtil.openPhoneCall(phoneInfo, sessionId, callerUuid, callerName);
    }

    @RPCPacket(CALL_RINGING)
    public static void callRinging(RPCSender sender, java.util.UUID sessionId, java.util.UUID calleeUuid, String calleeName) {
        SmartPhoneClientUtil.callRinging(sessionId, calleeUuid, calleeName);
    }

    @RPCPacket(CALL_CONNECTED)
    public static void callConnected(RPCSender sender, java.util.UUID sessionId, java.util.UUID remoteUuid, String remoteName) {
        SmartPhoneClientUtil.callConnected(sessionId, remoteUuid, remoteName);
    }

    @RPCPacket(CALL_ENDED)
    public static void callEnded(RPCSender sender, java.util.UUID sessionId, String reasonKey) {
        SmartPhoneClientUtil.callEnded(sessionId, reasonKey);
    }

    @RPCPacket(CALL_ERROR)
    public static void callError(RPCSender sender, String messageKey) {
        SmartPhoneClientUtil.callError(messageKey);
    }

    @RPCPacket(CALL_STATUS_UPDATE)
    public static void callStatusUpdate(RPCSender sender, String encodedStatuses) {
        SmartPhoneClientUtil.callStatusUpdate(encodedStatuses);
    }

    @RPCPacket(OFFICIAL_MESSAGE_RECEIVED)
    public static void officialMessageReceived(RPCSender sender, OfficialMessage message) {
        SmartPhoneClientUtil.receiveOfficialMessage(message);
    }
}
