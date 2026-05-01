package com.smart.phone.compat.voicechat;

import com.smart.phone.SmartPhone;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

@ForgeVoicechatPlugin
public class SmartPhoneVoicechatPlugin implements VoicechatPlugin {
    private static VoicechatServerApi voicechatServerApi;

    @Override
    public String getPluginId() {
        return SmartPhone.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        voicechatServerApi = event.getVoicechat();
        CallManager.getInstance().setVoicechatApi(voicechatServerApi);
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        CallManager.getInstance().cleanupAll("smartPhone.ui.app.phoneCall.ended.serverStopped", true);
        voicechatServerApi = null;
        CallManager.getInstance().setVoicechatApi(null);
    }
}
