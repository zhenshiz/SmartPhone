package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.SmartPhone;
import com.smart.phone.network.s2c.S2CPayload;
import com.smart.phone.ui.data.PhoneInfo;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;

@KJSBindings
public class SmartPhoneServerUtil {

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
        SmartPhone.getPhoneSavedData().setPhoneInfo(player, phoneInfo);
    }

    @Info("打开配置文件")
    public static void openSetting(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_SETTING, SmartPhone.getPhoneSavedData().getPhoneInfo(player));
    }
}
