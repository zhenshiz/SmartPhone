package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.components.PlayerHeadElement;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.view.HomeScreen;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.resources.ResourceLocation;

public class SettingUI extends AppUI {

    public SettingUI(HomeScreen homeScreen) {
        super(homeScreen);

        if (minecraft.player == null) return;

        PhoneInfo phoneInfo = homeScreen.getPhoneUI().phoneInfo;
        ConfiguratorGroup group = (ConfiguratorGroup) new ConfiguratorGroup().layout(layout -> layout.widthPercent(100));
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(TaffyDisplay.NONE);

        StringConfigurator phoneWallpaper = new StringConfigurator("smartPhone.data.phoneInfo.phoneWallpaper", () -> phoneInfo.getPhoneWallpaper().toString(), res -> {
            ResourceLocation texture = ResourceLocation.parse(res);
            if (SmartPhone.isPresentResource(texture)) {
                phoneInfo.setPhoneWallpaper(texture);
                homeScreen.savePhoneData();
            }
        }, phoneInfo.getPhoneWallpaper().toString(), true).setResourceLocation(true);
        phoneWallpaper.textField.textFieldStyle(textField -> textField.fontSize(6));
        group.addConfigurators(phoneWallpaper);

        group.selfAndAllChildren().forEach(element -> {
            if (element instanceof TextElement textElement) textElement.textStyle(textStyle -> textStyle.fontSize(6));
        });

        appScrollView.viewContainer.addChildren(new PlayerHeadElement(16),
                new Label().setText(minecraft.player.getDisplayName()).textStyle(textStyle -> textStyle.adaptiveWidth(true).adaptiveHeight(true).fontSize(6)).layout(layout -> layout.marginAll(2)),
                group);
        var phone = homeScreen.getPhoneUI();
        if (!phone.isPreview() && phone.getAccessToken() != null) {
            var change = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button().setText(phone.isPasscodeEnabled()
                    ? "smartPhone.security.change" : "smartPhone.security.enable");
            change.setId("phone_passcode_setting");
            change.layout(l -> l.widthPercent(94).height(15).marginTop(6));
            change.textStyle(t -> t.fontSize(5));
            change.setOnClick(e -> phone.showPasscode(phone.isPasscodeEnabled()
                    ? com.smart.phone.ui.view.PasscodeView.Mode.CHANGE : com.smart.phone.ui.view.PasscodeView.Mode.SET));
            appScrollView.viewContainer.addChild(change);
            if (phone.isPasscodeEnabled()) {
                var disable = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button().setText("smartPhone.security.disable");
                disable.setId("phone_passcode_disable");
                disable.layout(l -> l.widthPercent(94).height(15).marginTop(3));
                disable.textStyle(t -> t.fontSize(5));
                disable.setOnClick(e -> phone.showPasscode(com.smart.phone.ui.view.PasscodeView.Mode.DISABLE));
                appScrollView.viewContainer.addChild(disable);
            }
        }
    }
}
