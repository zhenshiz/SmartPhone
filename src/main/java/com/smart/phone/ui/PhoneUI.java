package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.smart.phone.Config;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.ui.view.LockScreen;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

public class PhoneUI extends UIElement {
    public final UIElement screenContainer;
    public final HomeScreen homeScreen;
    public final LockScreen lockScreen;

    private static final IGuiTexture BACKGROUND = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/phone.png"));
    private static final IGuiTexture SIGNAL = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/signal.png"));
    private static final IGuiTexture BATTERY = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/battery.png"));
    private static final IGuiTexture TOP_BAR_BACKGROUND = new ColorRectTexture(ColorPattern.BLACK.color);
    public PhoneInfo phoneInfo;
    private String lastHeaderTime = "";
    private Component lastHeaderTitle = Component.empty();
    private boolean lastTopBarVisible;

    public PhoneUI(PhoneInfo phoneInfo) {
        this.phoneInfo = phoneInfo;
        this.homeScreen = new HomeScreen(this);
        this.lockScreen = new LockScreen(this);

        this.layout(layout -> {
            layout.marginLeft(Config.PHONE_MARGIN_LEFT.get().floatValue());
            layout.marginTop(Config.PHONE_MARGIN_TOP.get().floatValue());
            layout.widthPercent(80);
            layout.heightPercent(80);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> {
            style.backgroundTexture(BACKGROUND);
        });

        addEventListener(UIEvents.TICK, event -> phoneInfo.getIPhoneTimeSource().tick());

        UIElement topContainer = new UIElement().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.top(0);
            layout.left(0);
            layout.widthPercent(100);
            layout.height(8);
            layout.paddingHorizontal(2);
        }).style(style -> style.zIndex(20)).addEventListener(UIEvents.TICK, event -> updateTopBarBackground(event.target));

        Label timeLabel = new Label();
        timeLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(8);
        });
        timeLabel.textStyle(textStyle -> {
            textStyle.fontSize(5);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.LEFT);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        timeLabel.addEventListener(UIEvents.TICK, event -> updateHeaderTime((Label) event.target));
        UIElement left = new UIElement().layout(layout -> layout.flex(1)).addChildren(timeLabel);

        Label titleLabel = new Label();
        titleLabel.setText(Component.empty());
        titleLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(8);
        });
        titleLabel.textStyle(textStyle -> {
            textStyle.fontSize(5);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        titleLabel.addEventListener(UIEvents.TICK, event -> updateHeaderTitle((Label) event.target));
        UIElement center = new UIElement().layout(layout -> layout.flex(1).justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER)).addChildren(titleLabel);

        UIElement right = new UIElement().layout(layout -> layout.flex(1).alignItems(AlignItems.FLEX_END)).addChildren(new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        }).addChildren(new UIElement().layout(layout -> {
            layout.width(6);
            layout.height(4);
        }).style(style -> style.backgroundTexture(SIGNAL)), new UIElement().layout(layout -> {
            layout.width(8);
            layout.height(8);
        }).style(style -> style.backgroundTexture(BATTERY))));

        topContainer.addChildren(left, center, right);

        screenContainer = new UIElement().layout(layout -> {
            layout.widthPercent(23.4f);
            layout.heightPercent(78.6f);
        }).style(style -> {
            style.holder.setOverflowVisible(false);
        }).addChildren(homeScreen, lockScreen, topContainer);

        this.addChildren(screenContainer);
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        return PhoneUiScale.defaultAutoSize(screenSize);
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        transform(transform -> transform.pivot(0.5f, 0.5f).scale(PhoneUiScale.autoScaleFactor()));
    }

    private void updateTopBarBackground(UIElement topContainer) {
        boolean visible = homeScreen.appUI != null;
        if (visible == lastTopBarVisible) return;
        lastTopBarVisible = visible;
        topContainer.getStyle().backgroundTexture(visible ? TOP_BAR_BACKGROUND : IGuiTexture.EMPTY);
    }

    private void updateHeaderTime(Label label) {
        String time = "%s:%s".formatted(
                phoneInfo.getIPhoneTimeSource().getHour(),
                phoneInfo.getIPhoneTimeSource().getMinute()
        );
        if (time.equals(lastHeaderTime)) return;
        lastHeaderTime = time;
        label.setText(Component.literal(time));
    }

    private void updateHeaderTitle(Label label) {
        Component title = homeScreen.iApp == null ? Component.empty() : homeScreen.iApp.getDisplayName();
        if (title.equals(lastHeaderTitle)) return;
        lastHeaderTitle = title;
        label.setText(title);
    }
}
