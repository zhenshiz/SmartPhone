package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.SettingUI;
import com.smart.phone.ui.data.PhoneInfo;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@KJSBindings(clientOnly = true)
public class SmartPhoneClientUtil {

    @Info("打开手机")
    public static void openPhone(PhoneInfo phoneInfo) {
        PhoneUI phoneUI = new PhoneUI(phoneInfo);
        ModularUI modularUI = new ModularUI(UI.of(phoneUI));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    @Info("更新玩家手机信息")
    public static void setPhoneInfoByPlayer(PhoneInfo phoneInfo) {
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, phoneInfo);
    }

    @Info("打开配置文件")
    public static void openSetting(PhoneInfo phoneInfo) {
        SettingUI settingUI = new SettingUI(phoneInfo);
        ModularUI modularUI = new ModularUI(UI.of(settingUI));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }
}
