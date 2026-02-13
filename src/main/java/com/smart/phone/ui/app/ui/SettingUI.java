package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.components.PlayerHeadElement;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;

public class SettingUI extends AppUI {

    public SettingUI(HomeScreen homeScreen) {
        super(homeScreen);

        if (minecraft.player == null) return;

        PhoneInfo phoneInfo = homeScreen.getPhoneUI().phoneInfo;
        ConfiguratorGroup group = (ConfiguratorGroup) new ConfiguratorGroup().layout(layout -> layout.setWidthPercent(100));
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);

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
                new Label().setText(minecraft.player.getDisplayName()).textStyle(textStyle -> textStyle.adaptiveWidth(true).adaptiveHeight(true).fontSize(6)).layout(layout -> layout.setMargin(YogaEdge.ALL, 2)),
                group);
    }
}
