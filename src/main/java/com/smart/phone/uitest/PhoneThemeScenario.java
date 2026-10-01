package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.app.*;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.world.item.ItemStack;

@LDLRegisterClient(name = "phone_theme", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneThemeScenario implements UIScenario {
    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("phone", "theme");
    }

    @Override public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY);
        for (IApp app : new IApp[]{new Setting(), new Notepad(), new AppStore(), new OfficialMessages(),
                new ChatRoomApp(), new PhotoAlbumApp(), new PhoneCall(), new Game2048(), new SnakeGame(),
                new Minesweeper(), new FlappyBirdGame(), new PianoTilesGame()}) {
            s.step("open themed " + app.name(), ctx -> SmartPhoneClientUtil.openPhoneApp(new PhoneInfo(), app))
                    .ticks(3)
                    .check("themed app is rendered: " + app.name(), ctx -> {
                        var phone = (PhoneUI)ctx.requireUI().ui.rootElement;
                        return phone.homeScreen.appUI != null && phone.homeScreen.appUI.hasClass("phone_app")
                                && phone.homeScreen.iApp.name().equals(app.name()) && ctx.requireUI().ui.stylesheets.size() == 2;
                    }).screenshot("ore_" + app.name().replace(':', '_'));
        }
        s.step("open camera viewfinder", ctx -> com.smart.phone.client.camera.PhoneCameraClient.openPreview(new PhoneInfo()))
                .ticks(3).screenshot("ore_camera_viewfinder")
                .step("leave camera through normal phone screen", ctx -> SmartPhoneClientUtil.openPhoneApp(new PhoneInfo(), new Setting()))
                .ticks(3).teardown("close theme gallery", ctx -> ctx.mc().setScreen(null));
    }
}
