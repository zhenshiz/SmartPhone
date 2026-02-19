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
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaJustify;

public class SettingUI extends UIElement {

    public SettingUI(PhoneInfo phoneInfo) {
        this.layout(layout -> {
            layout.setWidthPercent(80);
            layout.setHeightPercent(80);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        ScrollerView scrollerView = new ScrollerView();
        scrollerView.viewContainer.layout(layout -> layout.setAlignItems(YogaAlign.CENTER).setJustifyContent(YogaJustify.FLEX_START));
        scrollerView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        scrollerView.scrollerStyle(style -> {
            style.verticalScrollDisplay(ScrollDisplay.NEVER);
            style.horizontalScrollDisplay(ScrollDisplay.NEVER);
        }).layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });

        Label title = (Label) new Label().setText("smartPhone.ui.app.setting").textStyle(textStyle -> textStyle.adaptiveWidth(true).adaptiveHeight(true)).layout(layout -> layout.setMargin(YogaEdge.BOTTOM, 2));

        ConfiguratorGroup group = (ConfiguratorGroup) new ConfiguratorGroup().layout(layout -> {
            layout.setWidthPercent(100);
        });
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);
        phoneInfo.buildConfigurator(group);
        group.addEventListener(Configurator.CHANGE_EVENT, event -> {
            SmartPhoneClientUtil.setPhoneInfoByPlayer(phoneInfo);
        });

        scrollerView.addScrollViewChildren(title, group);
        this.addChildren(scrollerView);
    }
}
