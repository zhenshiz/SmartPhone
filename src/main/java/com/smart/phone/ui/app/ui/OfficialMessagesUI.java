package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.ui.data.OfficialMessage;
import com.smart.phone.ui.data.OfficialMessagesData;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class OfficialMessagesUI extends AppUI {
    private static final ZoneId MESSAGE_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DETAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(MESSAGE_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd").withZone(MESSAGE_ZONE);
    private static final DateTimeFormatter CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(MESSAGE_ZONE);
    private static final int ROW_BACKGROUND_UNREAD = 0x30000000;
    private static final int ROW_BACKGROUND_READ = 0x12000000;
    private static final int AVATAR_UNREAD = 0xFF2F7D58;
    private static final int AVATAR_READ = 0xFF3D3946;
    private static final int TEXT_PRIMARY_UNREAD = ColorPattern.WHITE.color;
    private static final int TEXT_PRIMARY_READ = 0xFFD6D2DF;
    private static final int TEXT_SECONDARY = 0xFFAAA3B6;
    private static final int TEXT_MUTED = 0xFF898395;
    private static final int SEPARATOR = 0x22FFFFFF;
    private final OfficialMessagesData data;
    private OfficialMessage selectedMessage;

    public OfficialMessagesUI(HomeScreen homeScreen) {
        super(homeScreen);
        data = homeScreen.getPhoneUI().phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class);
        applyListLayout();
        showList();
    }

    private void applyListLayout() {
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingVertical(3);
            layout.paddingHorizontal(0);
            layout.gapAll(0);
        });
    }

    private void applyDetailLayout() {
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingAll(5);
            layout.gapAll(3);
        });
    }

    public void refreshFromData() {
        if (selectedMessage == null) {
            showList();
        }
    }

    private void showList() {
        selectedMessage = null;
        applyListLayout();
        appScrollView.clearAllScrollViewChildren();
        if (data.getMessages().isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.officialMessages.empty"), 6, ColorPattern.T_WHITE.color, 16, Horizontal.CENTER));
            return;
        }
        data.getMessages().forEach(message -> appScrollView.viewContainer.addChildren(createMessageRow(message)));
    }

    private UIElement createMessageRow(OfficialMessage message) {
        UIElement item = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(34);
            layout.flexDirection(FlexDirection.COLUMN);
        });

        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(33);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(4);
            layout.paddingVertical(2);
            layout.gapAll(4);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(message.isRead() ? ROW_BACKGROUND_READ : ROW_BACKGROUND_UNREAD)));

        UIElement textColumn = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });

        UIElement titleLine = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(10);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });
        titleLine.addChildren(
                createFlexLabel(Component.literal(clipByVisualWidth(titleText(message), 16)), 6, message.isRead() ? TEXT_PRIMARY_READ : TEXT_PRIMARY_UNREAD, 10, Horizontal.LEFT),
                createFixedLabel(Component.literal(compactTime(message.getCreatedAtMillis())), 4, TEXT_MUTED, 24, 10, Horizontal.RIGHT)
        );

        textColumn.addChildren(
                titleLine,
                createFullWidthLabel(Component.literal(clipByVisualWidth(previewText(message), 28)), 4.5f, TEXT_SECONDARY, 8, Horizontal.LEFT)
        );

        UIElement unreadDot = new UIElement().layout(layout -> {
            layout.width(3);
            layout.height(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(message.isRead() ? 0x00000000 : ColorPattern.GREEN.color)));

        row.addChildren(createOfficialIcon(message), textColumn, unreadDot);
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) {
                openMessage(message);
            }
        });

        UIElement separator = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(1);
            layout.flexDirection(FlexDirection.ROW);
        });
        UIElement separatorIndent = new UIElement().layout(layout -> {
            layout.width(26);
            layout.height(1);
        });
        UIElement separatorLine = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.height(1);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(SEPARATOR)));

        separator.addChildren(separatorIndent, separatorLine);
        item.addChildren(row, separator);
        return item;
    }

    private UIElement createOfficialIcon(OfficialMessage message) {
        UIElement icon = new UIElement().layout(layout -> {
            layout.width(18);
            layout.height(18);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(message.isRead() ? AVATAR_READ : AVATAR_UNREAD)));
        icon.addChildren(createFullWidthLabel(Component.literal("!"), 8, ColorPattern.WHITE.color, 18, Horizontal.CENTER));
        return icon;
    }

    private void openMessage(OfficialMessage message) {
        selectedMessage = message;
        if (!message.isRead()) {
            message.setRead(true);
            SmartPhoneClientUtil.markOfficialMessageRead(message.getMessageId());
        }
        applyDetailLayout();
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(
                createBackRow(),
                createLabel(title(message), 7, ColorPattern.WHITE.color, 12, Horizontal.LEFT),
                createLabel(meta(message), 5, ColorPattern.T_WHITE.color, 8, Horizontal.LEFT),
                createBodyLabel(message.getBody())
        );
    }

    private UIElement createBackRow() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(94);
            layout.height(14);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(0x33000000)));
        row.addChildren(createLabel(Component.translatable("smartPhone.ui.app.officialMessages.back"), 5, ColorPattern.WHITE.color, 10, Horizontal.CENTER));
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) {
                showList();
            }
        });
        return row;
    }

    private Label createBodyLabel(String body) {
        Label label = createLabel(Component.literal(body == null ? "" : body), 6, ColorPattern.WHITE.color, 12, Horizontal.LEFT);
        label.textStyle(textStyle -> {
            textStyle.textWrap(TextWrap.WRAP);
            textStyle.adaptiveHeight(true);
        });
        return label;
    }

    private Label createLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.widthPercent(94);
            layout.height(height);
        });
        label.textStyle(textStyle -> {
            textStyle.fontSize(fontSize);
            textStyle.textColor(color);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(horizontal);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        label.setOverflowVisible(false);
        return label;
    }

    private Label createFlexLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.height(height);
        });
        applyFixedTextStyle(label, fontSize, color, horizontal);
        return label;
    }

    private Label createFixedLabel(Component component, float fontSize, int color, float width, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.width(width);
            layout.height(height);
        });
        applyFixedTextStyle(label, fontSize, color, horizontal);
        return label;
    }

    private Label createFullWidthLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        applyFixedTextStyle(label, fontSize, color, horizontal);
        return label;
    }

    private void applyFixedTextStyle(Label label, float fontSize, int color, Horizontal horizontal) {
        label.textStyle(textStyle -> {
            textStyle.fontSize(fontSize);
            textStyle.textColor(color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(horizontal);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        label.setOverflowVisible(false);
    }

    private Component title(OfficialMessage message) {
        return Component.literal(titleText(message));
    }

    private Component meta(OfficialMessage message) {
        return Component.literal("%s · %s".formatted(displaySender(message), DETAIL_TIME_FORMATTER.format(Instant.ofEpochMilli(message.getCreatedAtMillis()))));
    }

    private String titleText(OfficialMessage message) {
        String title = message.getTitle();
        return title == null || title.isBlank() ? Component.translatable("smartPhone.ui.app.officialMessages.untitled").getString() : title;
    }

    private String previewText(OfficialMessage message) {
        String body = normalize(message.getBody());
        return body.isBlank() ? displaySender(message) : body;
    }

    private String displaySender(OfficialMessage message) {
        String sender = message.getSender();
        return sender == null || sender.isBlank() ? Component.translatable("smartPhone.ui.app.officialMessages.defaultSender").getString() : sender;
    }

    private String compactTime(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDate messageDate = instant.atZone(MESSAGE_ZONE).toLocalDate();
        return messageDate.equals(LocalDate.now(MESSAGE_ZONE)) ? CLOCK_FORMATTER.format(instant) : DATE_FORMATTER.format(instant);
    }

    private String clip(String text, int maxLength) {
        String normalized = normalize(text);
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String clipByVisualWidth(String text, int maxWidth) {
        String normalized = normalize(text);
        int width = 0;
        int index = 0;
        for (; index < normalized.length(); index++) {
            int codePoint = normalized.codePointAt(index);
            int nextWidth = width + visualWidth(codePoint);
            if (nextWidth > maxWidth) break;
            width = nextWidth;
            if (Character.isSupplementaryCodePoint(codePoint)) {
                index++;
            }
        }
        if (index >= normalized.length()) return normalized;
        int suffixWidth = 3;
        while (index > 0 && width + suffixWidth > maxWidth) {
            index--;
            int codePoint = normalized.codePointAt(index);
            width -= visualWidth(codePoint);
        }
        return normalized.substring(0, Math.max(0, index)) + "...";
    }

    private int visualWidth(int codePoint) {
        return codePoint <= 0x007F ? 1 : 2;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\n', ' ').trim();
    }
}
