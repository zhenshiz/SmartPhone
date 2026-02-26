package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyDisplay;

public class SettingUI extends UIElement {

    public SettingUI(PhoneInfo phoneInfo) {
        this.layout(layout -> {
            layout.widthPercent(80);
            layout.heightPercent(80);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        ScrollerView scrollerView = new ScrollerView();
        scrollerView.viewContainer.layout(layout -> layout.alignItems(AlignItems.CENTER).justifyContent(AlignContent.FLEX_START));
        scrollerView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        scrollerView.scrollerStyle(style -> {
            style.verticalScrollDisplay(ScrollDisplay.NEVER);
            style.horizontalScrollDisplay(ScrollDisplay.NEVER);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        Label title = (Label) new Label().setText("smartPhone.ui.app.setting").textStyle(textStyle -> textStyle.adaptiveWidth(true).adaptiveHeight(true)).layout(layout -> layout.marginBottom(2));

        ConfiguratorGroup group = (ConfiguratorGroup) new ConfiguratorGroup().layout(layout -> {
            layout.widthPercent(100);
        });
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(TaffyDisplay.NONE);
        phoneInfo.buildConfigurator(group);
        group.addEventListener(Configurator.CHANGE_EVENT, event -> {
            SmartPhoneClientUtil.setPhoneInfoByPlayer(phoneInfo);
        });

        scrollerView.addScrollViewChildren(title, group);
        this.addChildren(scrollerView);
    }
}
