package com.smart.phone.network.s2c;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;

public class S2CPayload {
    private static final String MOD_ID = SmartPhone.MOD_ID + ":";
    public static final String OPEN_PHONE = MOD_ID + "open_phone";
    public static final String OPEN_SETTING = MOD_ID + "open_setting";

    @RPCPacket(OPEN_PHONE)
    public static void openPhone(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneClientUtil.openPhone(phoneInfo);
    }

    @RPCPacket(OPEN_SETTING)
    public static void openSetting(RPCSender sender, PhoneInfo phoneInfo) {
        SmartPhoneClientUtil.openSetting(phoneInfo);
    }
}
