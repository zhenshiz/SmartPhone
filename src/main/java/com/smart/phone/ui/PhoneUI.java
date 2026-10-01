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

import java.util.UUID;

public class PhoneUI extends UIElement {
    // phone.png has transparent side margins; these canvas dimensions give the visible shell a 1:1.88 ratio.
    public static final float CANVAS_WIDTH = 470f;
    public static final float CANVAS_HEIGHT = 275f;
    public static final float SHELL_WIDTH = CANVAS_WIDTH * 808f / 2816f;
    public static final float SHELL_HEIGHT = CANVAS_HEIGHT * 1424f / 1536f;
    public static final float HEIGHT_SCALE = 1.1f;
    public final UIElement screenContainer;
    public final HomeScreen homeScreen;
    public final LockScreen lockScreen;

    private static final IGuiTexture BACKGROUND = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/phone.png"));
    private static final IGuiTexture SIGNAL = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/signal.png"));
    private static final IGuiTexture BATTERY = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/battery.png"));
    private static final IGuiTexture TOP_BAR_BACKGROUND = new ColorRectTexture(ColorPattern.BLACK.color);
    public PhoneInfo phoneInfo;
    private UUID accessToken;
    private boolean passcodeEnabled;
    private boolean accessLocked;
    private boolean lastBlocked;
    private com.smart.phone.ui.view.PasscodeView passcodeView;

    public UUID getAccessToken() { return accessToken; }
    public boolean isPasscodeEnabled() { return passcodeEnabled; }
    public boolean isAccessLocked() { return !isPreview() && (phoneInfo.isBlocked() || accessLocked); }

    public void configureAccess(UUID token, boolean enabled, boolean locked) {
        accessToken = token;
        passcodeEnabled = enabled;
        accessLocked = locked || phoneInfo.isBlocked();
        lastBlocked = phoneInfo.isBlocked();
        homeScreen.setActive(!isAccessLocked());
        homeScreen.setVisible(!isAccessLocked());
        if (isAccessLocked()) {
            com.smart.phone.client.chat.PhoneChatClientState.clear();
            com.smart.phone.client.message.PhoneMessageClientState.clear();
            homeScreen.closeApp();
            lockScreen.resetLocked();
        }
    }

    public void showPasscode(com.smart.phone.ui.view.PasscodeView.Mode mode) {
        if (isPreview() || phoneInfo.isBlocked() || accessToken == null || passcodeView != null) return;
        passcodeView = new com.smart.phone.ui.view.PasscodeView(this, mode);
        screenContainer.addChild(passcodeView);
        passcodeView.focus();
    }

    public void closePasscode() {
        if (passcodeView == null) return;
        screenContainer.removeChild(passcodeView);
        passcodeView = null;
        if (accessLocked) lockScreen.resetLocked();
    }

    public void securityResult(String action, boolean success, boolean enabled, PhoneInfo info, String error) {
        if (!success) {
            if (passcodeView != null) passcodeView.failed(error);
            return;
        }
        phoneInfo = info;
        passcodeEnabled = enabled;
        if (action.equals("lock")) {
            closePasscode();
            configureAccess(accessToken, enabled, enabled);
            lockScreen.resetLocked();
            homeScreen.reloadAppView();
            return;
        }
        accessLocked = false;
        homeScreen.setActive(true);
        homeScreen.setVisible(true);
        homeScreen.reloadAppView();
        closePasscode();
        if (action.equals("unlock")) lockScreen.unlockAfterVerification();
        else if (homeScreen.iApp != null) homeScreen.openApp(homeScreen.iApp);
    }

    private UUID ownerUuid;
    private Runnable previewChanged;
    private java.util.function.BiConsumer<String, UUID> chatSelection;
    private java.util.function.Consumer<UUID> officialMessageSelection;

    public void setOfficialMessageSelection(java.util.function.Consumer<UUID> listener) {
        officialMessageSelection = listener;
    }

    public void selectPreviewOfficialMessage(UUID messageId) {
        if (officialMessageSelection != null) officialMessageSelection.accept(messageId);
    }

    public void setChatSelection(java.util.function.BiConsumer<String, UUID> listener) { chatSelection = listener; }

    public void selectPreviewChat(String roomId, UUID messageId) {
        if (chatSelection != null) chatSelection.accept(roomId, messageId);
    }
    private String ownerName;

    public boolean isPreview() { return previewChanged != null; }

    public void configurePreview(UUID owner, String name, Runnable onChanged) {
        ownerUuid = owner;
        ownerName = name;
        previewChanged = onChanged;
        lastBlocked = phoneInfo.isBlocked();
        setId("phone_editor_preview");
        lockScreen.setId("phone_editor_lock");
        layout(l -> l.marginAll(0).width(CANVAS_WIDTH).height(CANVAS_HEIGHT).flexShrink(0));
    }

    public String getOwnerName() {
        if (ownerName != null) return ownerName;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                var stack = mc.player.getItemInHand(hand);
                if (java.util.Objects.equals(ownerUuid, stack.get(com.smart.phone.SmartPhoneRegistries.PHONE_OWNER.get()))) {
                    String name = stack.get(com.smart.phone.SmartPhoneRegistries.PHONE_OWNER_NAME.get());
                    if (name != null) return name;
                }
            }
            if (ownerUuid == null || ownerUuid.equals(mc.player.getUUID())) return mc.player.getGameProfile().getName();
        }
        return Component.translatable("smartPhone.item.phone.unknownOwner").getString();
    }

    public void setOwnerName(String name) { ownerName = name.isBlank() ? null : name; }

    public void savePhoneData() {
        if (isPreview()) previewChanged.run();
        else com.smart.phone.util.SmartPhoneClientUtil.setPhoneInfoByPlayer(phoneInfo);
    }

    private String lastHeaderTime = "";
    private Component lastHeaderTitle = Component.empty();
    private boolean lastTopBarVisible;
    private final boolean heldMode;

    public PhoneUI(PhoneInfo phoneInfo) {
        this(phoneInfo, false);
    }

    public PhoneUI(PhoneInfo phoneInfo, boolean heldMode) {
        this.phoneInfo = phoneInfo;
        this.heldMode = heldMode;
        if (heldMode) setId("held_phone");
        this.homeScreen = new HomeScreen(this);
        this.lockScreen = new LockScreen(this);
        if (heldMode) this.lockScreen.setId("held_phone_lock");

        this.layout(layout -> {
            if (!heldMode) {
                layout.marginLeft(Config.PHONE_MARGIN_LEFT.get().floatValue());
                layout.marginTop(Config.PHONE_MARGIN_TOP.get().floatValue());
            }
            layout.width(CANVAS_WIDTH);
            layout.height(CANVAS_HEIGHT);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> {
            style.backgroundTexture(BACKGROUND);
        });

        addEventListener(UIEvents.TICK, event -> {
            this.phoneInfo.getIPhoneTimeSource().tick();
            if (lastBlocked != this.phoneInfo.isBlocked()) {
                closePasscode();
                configureAccess(accessToken, passcodeEnabled, passcodeEnabled);
                lockScreen.resetLocked();
            }
        });

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
        timeLabel.setId("phone_header_time");
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
        }).setId("phone_status_icons").addEventListener(UIEvents.TICK,
                event -> event.target.setVisible(!this.phoneInfo.isHideStatusIcons())).addChildren(new UIElement().layout(layout -> {
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

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        Size size = PhoneUiScale.defaultAutoSize(screenSize);
        int height = Math.round(size.height * HEIGHT_SCALE);
        return Size.of(Math.round(height * CANVAS_WIDTH / CANVAS_HEIGHT), height);
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        if (!isPreview()) transform(transform -> transform.pivot(0.5f, 0.5f).scale(PhoneUiScale.autoScaleFactor()));
    }

    public void updateHeldPose(int screenWidth, int screenHeight, float progress) {
        if (!heldMode) return;
        float eased = 1f - (float) Math.pow(1f - progress, 3);
        float x = screenWidth * (0.27f + Config.PHONE_MARGIN_LEFT.get().floatValue() / 100f);
        float y = screenHeight * (0.04f + Config.PHONE_MARGIN_TOP.get().floatValue() / 100f);
        transform(transform -> transform.translate(x + (1f - eased) * screenWidth * 0.12f,
                y + (1f - eased) * screenHeight * 0.9f)
                .rotation((1f - eased) * 11f));
    }

    private void updateTopBarBackground(UIElement topContainer) {
        boolean visible = homeScreen.appUI != null;
        if (visible == lastTopBarVisible) return;
        lastTopBarVisible = visible;
        topContainer.getStyle().backgroundTexture(visible ? TOP_BAR_BACKGROUND : IGuiTexture.EMPTY);
    }

    private void updateHeaderTime(Label label) {
        label.setVisible(!phoneInfo.isHideDate());
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
