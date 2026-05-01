package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.SmartPhone;
import com.smart.phone.network.s2c.S2CPayload;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.OfficialMessagesData;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

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
}
