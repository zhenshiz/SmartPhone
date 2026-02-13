package com.smart.phone.network.c2s;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneServerUtil;

public class C2SPayload {
    private static final String MOD_ID = SmartPhone.MOD_ID + ":";
    public static final String SAVE_PHONE_INFO = MOD_ID + "save_phone_info";

    @RPCPacket(SAVE_PHONE_INFO)
    public static void savePhoneInfo(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneServerUtil.setPhoneInfoByPlayer(sender.asPlayer(), phoneInfo);
    }
}
