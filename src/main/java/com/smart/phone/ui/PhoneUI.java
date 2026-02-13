package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.Config;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.ui.view.LockScreen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

public class PhoneUI extends UIElement {
    public final UIElement screenContainer;
    public final HomeScreen homeScreen;
    public final LockScreen lockScreen;

    private static final IGuiTexture BACKGROUND = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/phone.png"));
    private static final IGuiTexture SIGNAL = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/signal.png"));
    private static final IGuiTexture BATTERY = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/battery.png"));
    public PhoneInfo phoneInfo;

    public PhoneUI(PhoneInfo phoneInfo) {
        this.phoneInfo = phoneInfo;
        this.homeScreen = new HomeScreen(this);
        this.lockScreen = new LockScreen(this);

        this.layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, Config.PHONE_MARGIN_LEFT.get().floatValue());
            layout.setMargin(YogaEdge.TOP, Config.PHONE_MARGIN_TOP.get().floatValue());
            layout.setWidthPercent(80);
            layout.setHeightPercent(80);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        }).style(style -> {
            style.backgroundTexture(BACKGROUND);
        });

        addEventListener(UIEvents.TICK, event -> phoneInfo.getIPhoneTimeSource().tick());

        UIElement topContainer = new UIElement().layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
            layout.top(0);
            layout.left(0);
            layout.setWidthPercent(100);
            layout.setHeight(8);
            layout.setPadding(YogaEdge.HORIZONTAL, 2);
        }).style(style -> style.zIndex(20)).addEventListener(UIEvents.TICK, event -> {
            event.target.getStyle().backgroundTexture(homeScreen.appUI == null ? IGuiTexture.EMPTY : new ColorRectTexture(ColorPattern.BLACK.color));
        });

        UIElement left = new UIElement().layout(layout -> layout.setFlex(1)).addChildren(new Label().textStyle(textStyle -> {
            textStyle.fontSize(5);
            textStyle.adaptiveHeight(true);
        }).addEventListener(UIEvents.TICK, event -> {
            Label target = (Label) event.target;
            target.setText("%s:%s".formatted(
                    phoneInfo.getIPhoneTimeSource().getHour(),
                    phoneInfo.getIPhoneTimeSource().getMinute()
            ));
        }));
        UIElement center = new UIElement().layout(layout -> layout.setFlex(1).setJustifyContent(YogaJustify.CENTER).setAlignItems(YogaAlign.CENTER)).addChildren(new Label().textStyle(textStyle -> textStyle.fontSize(5).adaptiveWidth(true).adaptiveHeight(true)).addEventListener(UIEvents.TICK, event -> {
            ((Label) event.target).setText(homeScreen.iApp == null ? Component.empty() : homeScreen.iApp.getDisplayName());
        }));
        UIElement right = new UIElement().layout(layout -> layout.setFlex(1).setAlignItems(YogaAlign.FLEX_END)).addChildren(new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setWidth(6);
            layout.setHeight(4);
        }).style(style -> style.backgroundTexture(SIGNAL)), new UIElement().layout(layout -> {
            layout.setWidth(8);
            layout.setHeight(8);
        }).style(style -> style.backgroundTexture(BATTERY))));
        topContainer.addChildren(left, center, right);

        screenContainer = new UIElement().layout(layout -> {
            layout.setWidthPercent(23.4f);
            layout.setHeightPercent(78.6f);
        }).style(style -> {
            style.holder.setOverflow(YogaOverflow.HIDDEN);
        }).addChildren(homeScreen, lockScreen, topContainer);

        this.addChildren(screenContainer);
    }
}
