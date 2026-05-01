package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.client.call.PhoneCallClientState;
import com.smart.phone.compat.voicechat.CallState;
import com.smart.phone.compat.voicechat.CallStatusKeys;
import com.smart.phone.ui.components.PlayerHeadElement;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PhoneCallUI extends AppUI {
    private static final int LAYOUT_UNSET = 0;
    private static final int LAYOUT_PLAYER_LIST = 1;
    private static final int LAYOUT_CALL_STATE = 2;
    private static final int PLAYER_LIST_CHECK_INTERVAL = 20;
    private static final int STATUS_REQUEST_INTERVAL = 40;

    private int lastStateVersion = -1;
    private int lastStatusVersion = -1;
    private int refreshTicks;
    private int statusRequestTicks = STATUS_REQUEST_INTERVAL;
    private int layoutMode = LAYOUT_UNSET;
    private String lastPlayerListKey = "";

    public PhoneCallUI(HomeScreen homeScreen) {
        super(homeScreen);
        requestStatus();
        reload();
        addEventListener(UIEvents.TICK, event -> {
            refreshTicks++;
            statusRequestTicks++;
            if (!PhoneCallClientState.hasActiveSession() && statusRequestTicks >= STATUS_REQUEST_INTERVAL) {
                statusRequestTicks = 0;
                requestStatus();
            }
            if (lastStateVersion != PhoneCallClientState.getVersion()) {
                refreshTicks = 0;
                reload();
                return;
            }
            if (PhoneCallClientState.hasActiveSession()) return;

            boolean statusChanged = lastStatusVersion != PhoneCallClientState.getStatusVersion();
            if (statusChanged || refreshTicks >= PLAYER_LIST_CHECK_INTERVAL) {
                refreshTicks = 0;
                String playerListKey = createPlayerListKey();
                if (statusChanged || !playerListKey.equals(lastPlayerListKey)) {
                    reload();
                }
            }
        });
    }

    private void reload() {
        lastStateVersion = PhoneCallClientState.getVersion();
        lastStatusVersion = PhoneCallClientState.getStatusVersion();
        appScrollView.clearAllScrollViewChildren();
        if (PhoneCallClientState.hasActiveSession()) {
            applyCallStateLayout();
            lastPlayerListKey = "";
            renderCallState();
        } else {
            applyPlayerListLayout();
            lastPlayerListKey = createPlayerListKey();
            renderPlayerRows();
        }
    }

    private void applyPlayerListLayout() {
        if (layoutMode == LAYOUT_PLAYER_LIST) return;
        layoutMode = LAYOUT_PLAYER_LIST;
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.flexWrap(FlexWrap.NO_WRAP);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(2);
            layout.gapAll(4);
        });
    }

    private void applyCallStateLayout() {
        if (layoutMode == LAYOUT_CALL_STATE) return;
        layoutMode = LAYOUT_CALL_STATE;
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.flexWrap(FlexWrap.NO_WRAP);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(8);
            layout.gapAll(5);
        });
    }

    private void renderPlayerRows() {
        List<PlayerInfo> players = getVisiblePlayers();
        players.forEach(playerInfo -> appScrollView.viewContainer.addChildren(createPlayerRow(playerInfo)));
        if (appScrollView.viewContainer.getChildren().isEmpty()) {
            appScrollView.viewContainer.addChildren(createCenteredLabel(Component.translatable("smartPhone.ui.app.phoneCall.empty"), 6, 12));
        }
    }

    private UIElement createPlayerRow(PlayerInfo playerInfo) {
        UUID playerUuid = playerInfo.getProfile().getId();
        String statusKey = PhoneCallClientState.getPlayerStatus(playerUuid);
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(92);
            layout.height(34);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(3);
            layout.gapAll(4);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(0x33000000)));

        UIElement info = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });
        info.addChildren(
                createTextLabel(Component.translatable("smartPhone.ui.app.phoneCall.playerId", playerInfo.getProfile().getName()), 6, ColorPattern.WHITE.color, 9),
                createTextLabel(Component.translatable(statusKey), 5, statusColor(statusKey), 8)
        );

        UIElement statusBar = new UIElement().layout(layout -> {
            layout.width(2);
            layout.height(22);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(statusColor(statusKey))));

        row.addChildren(new PlayerHeadElement(playerUuid, 20), info, statusBar);
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            SmartPhoneClientUtil.dialCall(playerUuid);
            Toast.show(this, Component.translatable("smartPhone.ui.app.phoneCall.status.calling"), 1.5f);
        });
        return row;
    }

    private void renderCallState() {
        appScrollView.viewContainer.addChildren(new PlayerHeadElement(PhoneCallClientState.getRemoteUuid(), 32));
        appScrollView.viewContainer.addChildren(createCenteredLabel(remoteTitle(), 7, 10));
        appScrollView.viewContainer.addChildren(createCenteredLabel(PhoneCallClientState.message(), 7, 10));
        if (PhoneCallClientState.hasIncomingCall()) {
            appScrollView.viewContainer.addChildren(createIncomingButtons());
        } else if (PhoneCallClientState.getState() == CallState.CALLING || PhoneCallClientState.getState() == CallState.CONNECTED) {
            appScrollView.viewContainer.addChildren(createHangupButton());
        }
    }

    private Component remoteTitle() {
        String name = PhoneCallClientState.getRemoteName();
        return name == null || name.isBlank() ? Component.empty() : Component.literal(name);
    }

    private UIElement createIncomingButtons() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(16);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(3);
        });
        Button answer = smallButton("smartPhone.ui.app.phoneCall.answer");
        answer.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0 && PhoneCallClientState.getSessionId() != null) {
                SmartPhoneClientUtil.answerCall(PhoneCallClientState.getSessionId());
            }
        });
        Button reject = smallButton("smartPhone.ui.app.phoneCall.reject");
        reject.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0 && PhoneCallClientState.getSessionId() != null) {
                SmartPhoneClientUtil.rejectCall(PhoneCallClientState.getSessionId());
            }
        });
        row.addChildren(answer, reject);
        return row;
    }

    private Button createHangupButton() {
        Button hangup = smallButton("smartPhone.ui.app.phoneCall.hangup");
        hangup.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) {
                SmartPhoneClientUtil.hangupCall();
            }
        });
        return hangup;
    }

    private Button smallButton(String translationKey) {
        Button button = new Button();
        button.layout(layout -> {
            layout.width(36);
            layout.height(14);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        button.text.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.marginHorizontal(0);
        });
        button.textStyle(textStyle -> {
            textStyle.fontSize(4.5f);
            textStyle.textColor(ColorPattern.WHITE.color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        button.text.setOverflowVisible(false);
        button.setText(translationKey);
        return button;
    }

    private Label createTextLabel(Component component, float fontSize, int color, float height) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        label.textStyle(textStyle -> {
            textStyle.fontSize(fontSize);
            textStyle.textColor(color);
            textStyle.textWrap(TextWrap.HOVER_ROLL);
            textStyle.textAlignHorizontal(Horizontal.LEFT);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        label.setOverflowVisible(false);
        return label;
    }

    private Label createCenteredLabel(Component component, float fontSize, float height) {
        Label label = createTextLabel(component, fontSize, ColorPattern.WHITE.color, height);
        label.textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER));
        return label;
    }

    private int statusColor(String statusKey) {
        return switch (statusKey) {
            case CallStatusKeys.AVAILABLE -> ColorPattern.GREEN.color;
            case CallStatusKeys.BUSY -> ColorPattern.YELLOW.color;
            case CallStatusKeys.VOICE_UNAVAILABLE -> ColorPattern.RED.color;
            default -> ColorPattern.T_WHITE.color;
        };
    }

    private String createPlayerListKey() {
        return getVisiblePlayers().stream()
                .map(playerInfo -> {
                    UUID playerUuid = playerInfo.getProfile().getId();
                    return playerUuid + ":" + playerInfo.getProfile().getName() + ":" + PhoneCallClientState.getPlayerStatus(playerUuid);
                })
                .collect(Collectors.joining("|"));
    }

    private List<PlayerInfo> getVisiblePlayers() {
        if (minecraft.player == null || minecraft.getConnection() == null) return List.of();
        UUID self = minecraft.player.getUUID();
        return minecraft.getConnection().getOnlinePlayers().stream()
                .filter(playerInfo -> !self.equals(playerInfo.getProfile().getId()))
                .sorted(Comparator.comparing(playerInfo -> playerInfo.getProfile().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void requestStatus() {
        if (minecraft.player != null && minecraft.getConnection() != null) {
            SmartPhoneClientUtil.requestCallStatus();
        }
    }
}
