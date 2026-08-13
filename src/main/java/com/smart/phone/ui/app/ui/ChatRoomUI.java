package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.client.chat.PhoneChatClientState;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSavedData;
import com.smart.phone.ui.data.chat.ChatRoomSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomSummary;
import com.smart.phone.ui.data.social.FriendEntry;
import com.smart.phone.ui.data.social.FriendStatus;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChatRoomUI extends AppUI {
    private static final int LIST_MODE_ROOMS = 0;
    private static final int LIST_MODE_FRIENDS = 1;
    private static final ZoneId MESSAGE_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd").withZone(MESSAGE_ZONE);
    private static final DateTimeFormatter CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(MESSAGE_ZONE);
    private static final int ROW_BACKGROUND = 0x22000000;
    private static final int BUBBLE_SELF = 0x55307752;
    private static final int BUBBLE_OTHER = 0x442F2A38;
    private static final int TEXT_SECONDARY = 0xFFAAA3B6;

    private final UIElement inputBar;
    private final TextArea inputArea;
    private int lastVersion = -1;
    private int listMode = LIST_MODE_ROOMS;
    private String selectedRoomId;
    private String draftText = "";
    private boolean showingPhotoPicker;

    public ChatRoomUI(HomeScreen homeScreen) {
        super(homeScreen);
        appScrollView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingHorizontal(0);
            layout.paddingVertical(2);
            layout.gapAll(1);
        });

        inputArea = new TextArea();
        inputArea.textAreaStyle(style -> {
            style.fontSize(4.5f);
            style.placeholder(Component.translatable("smartPhone.ui.app.chatRoom.input"));
        }).layout(layout -> {
            layout.flex(1);
            layout.height(16);
            layout.paddingAll(2);
        });
        inputArea.setLinesResponder(lines -> draftText = String.join("\n", lines));

        inputBar = new UIElement().layout(layout -> {
            layout.widthPercent(98);
            layout.height(20);
            layout.marginBottom(5);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });
        inputBar.addChildren(inputArea, createAttachButton(), createSendButton());
        inputBar.setVisible(false);
        addChild(inputBar);

        addEventListener(UIEvents.TICK, event -> {
            if (lastVersion == PhoneChatClientState.getVersion()) return;
            refreshFromState();
        });

        showRoomList();
        SmartPhoneClientUtil.requestChatRooms();
        SmartPhoneClientUtil.requestFriendList();
    }

    public void refreshFromState() {
        lastVersion = PhoneChatClientState.getVersion();
        if (showingPhotoPicker) return;
        if (selectedRoomId == null) {
            showRoomList();
        } else {
            showRoomMessages();
        }
    }

    private void showRoomList() {
        selectedRoomId = null;
        inputBar.setVisible(false);
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(createTabBar());
        if (listMode == LIST_MODE_FRIENDS) {
            showFriendRows();
            return;
        }
        List<ChatRoomSummary> rooms = PhoneChatClientState.getRooms();
        if (rooms.isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.loading"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            return;
        }
        rooms.forEach(room -> appScrollView.viewContainer.addChildren(createRoomRow(room)));
    }

    private UIElement createRoomRow(ChatRoomSummary room) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(28);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(3);
            layout.paddingVertical(2);
            layout.gapAll(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ROW_BACKGROUND)));
        row.setId("chat_room_" + safeSelectorId(room.getRoomId()));

        UIElement icon = new UIElement().layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(0xFF3D5D7A)));
        icon.addChildren(createInlineLabel(Component.literal("#"), 7, ColorPattern.WHITE.color, 14, Horizontal.CENTER));

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
            layout.height(9);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });
        titleLine.addChildren(
                createFlexLabel(roomTitle(room), 5.5f, ColorPattern.WHITE.color, 9, Horizontal.LEFT),
                createFixedLabel(Component.literal(room.getLatestAtMillis() <= 0 ? "" : compactTime(room.getLatestAtMillis())), 4, TEXT_SECONDARY, 22, 9, Horizontal.RIGHT)
        );
        textColumn.addChildren(
                titleLine,
                createInlineLabel(Component.literal(clipByVisualWidth(roomPreview(room), 36)), 4.2f, TEXT_SECONDARY, 8, Horizontal.LEFT)
        );

        row.addChildren(icon, textColumn);
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            selectedRoomId = room.getRoomId();
            inputBar.setVisible(true);
            appScrollView.clearAllScrollViewChildren();
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.loading"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            SmartPhoneClientUtil.openChatRoom(selectedRoomId);
        });
        return row;
    }

    private UIElement createTabBar() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(12);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(2);
            layout.gapAll(2);
        });
        row.addChildren(
                createTabButton("smartPhone.ui.app.chatRoom.tab.rooms", listMode == LIST_MODE_ROOMS, () -> {
                    listMode = LIST_MODE_ROOMS;
                    SmartPhoneClientUtil.requestChatRooms();
                    showRoomList();
                }),
                createTabButton("smartPhone.ui.app.chatRoom.tab.friends", listMode == LIST_MODE_FRIENDS, () -> {
                    listMode = LIST_MODE_FRIENDS;
                    SmartPhoneClientUtil.requestFriendList();
                    showRoomList();
                })
        );
        return row;
    }

    private Button createTabButton(String key, boolean active, Runnable onClick) {
        Button button = createFlexibleButton(key, active ? 0x553D5D7A : 0x22000000);
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            onClick.run();
        });
        return button;
    }

    private void showFriendRows() {
        List<FriendEntry> friends = PhoneChatClientState.getFriends();
        if (friends.isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.friend.empty"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            return;
        }
        friends.forEach(friend -> appScrollView.viewContainer.addChildren(createFriendRow(friend)));
    }

    private UIElement createFriendRow(FriendEntry friend) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(28);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(3);
            layout.paddingVertical(2);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ROW_BACKGROUND)));

        UIElement icon = new UIElement().layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(friend.isOnline() ? 0xFF2F7D58 : 0xFF3D3946)));
        icon.addChildren(createInlineLabel(Component.literal(friend.getTargetName().isBlank() ? "?" : friend.getTargetName().substring(0, 1)), 6, ColorPattern.WHITE.color, 14, Horizontal.CENTER));

        UIElement textColumn = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });
        textColumn.addChildren(
                createInlineLabel(Component.literal(clipByVisualWidth(friend.getTargetName(), 22)), 5.5f, ColorPattern.WHITE.color, 9, Horizontal.LEFT),
                createInlineLabel(Component.translatable(statusKey(friend)), 3.8f, TEXT_SECONDARY, 7, Horizontal.LEFT)
        );

        row.addChildren(icon, textColumn, createFriendActionButton(friend));
        return row;
    }

    private Button createFriendActionButton(FriendEntry friend) {
        String status = friend.getStatus();
        if (FriendStatus.ACCEPTED.equals(status)) {
            Button button = createCompactButton(friend.isOnline() ? "smartPhone.ui.app.chatRoom.friend.chat" : "smartPhone.ui.app.chatRoom.friend.offline", 24, 0x44307752);
            if (friend.isOnline()) {
                button.addEventListener(UIEvents.CLICK, event -> {
                    if (event.button != 0 || minecraft.player == null) return;
                    selectedRoomId = ChatRoomSavedData.directRoomId(minecraft.player.getUUID(), friend.getTargetUuid());
                    inputBar.setVisible(true);
                    appScrollView.clearAllScrollViewChildren();
                    appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.loading"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
                    SmartPhoneClientUtil.openDirectChat(friend.getTargetUuid());
                });
            }
            return button;
        }
        if (FriendStatus.PENDING_RECEIVED.equals(status)) {
            Button button = createCompactButton("smartPhone.ui.app.chatRoom.friend.accept", 24, 0x443D5D7A);
            button.addEventListener(UIEvents.CLICK, event -> {
                if (event.button != 0) return;
                SmartPhoneClientUtil.acceptFriend(friend.getTargetUuid());
            });
            return button;
        }
        if (FriendStatus.PENDING_SENT.equals(status)) {
            return createCompactButton("smartPhone.ui.app.chatRoom.friend.waiting", 24, 0x22000000);
        }
        Button button = createCompactButton("smartPhone.ui.app.chatRoom.friend.add", 24, 0x443D5D7A);
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            SmartPhoneClientUtil.requestFriend(friend.getTargetUuid());
        });
        return button;
    }

    private void showRoomMessages() {
        inputBar.setVisible(true);
        appScrollView.clearAllScrollViewChildren();
        ChatRoomSnapshot snapshot = PhoneChatClientState.getRoom(selectedRoomId).orElse(null);
        appScrollView.viewContainer.addChildren(createBackRow(snapshot));
        if (snapshot == null) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.loading"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            return;
        }
        if (snapshot.getMessages().isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.noMessages"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            return;
        }
        snapshot.getMessages().forEach(message -> appScrollView.viewContainer.addChildren(createMessageBubble(message)));
    }

    private UIElement createBackRow(ChatRoomSnapshot snapshot) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(15);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(3);
            layout.gapAll(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(0x22000000)));
        row.addChildren(
                createFixedLabel(Component.translatable("smartPhone.ui.app.chatRoom.backRooms"), 5, ColorPattern.WHITE.color, 38, 10, Horizontal.LEFT),
                createFlexLabel(snapshot == null ? Component.empty() : roomTitle(snapshot), 5, TEXT_SECONDARY, 10, Horizontal.RIGHT)
        );
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            showRoomList();
        });
        return row;
    }

    private UIElement createMessageBubble(ChatRoomMessage message) {
        boolean ownMessage = isOwnMessage(message);
        boolean hasImage = message.getImageData() != null && message.getImageData().length > 0;
        boolean isImageOnly = hasImage && "[image]".equals(message.getBody());

        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(ownMessage ? AlignItems.FLEX_END : AlignItems.FLEX_START);
        });

        UIElement bubble = new UIElement().layout(layout -> {
            layout.widthPercent(80);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(3);
            layout.gapAll(1);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ownMessage ? BUBBLE_SELF : BUBBLE_OTHER)));

        bubble.addChildren(
                createInlineLabel(Component.literal("%s · %s".formatted(displaySender(message), compactTime(message.getCreatedAtMillis()))), 4, TEXT_SECONDARY, 7, Horizontal.LEFT)
        );

        // 内联图片
        if (hasImage) {
            UIElement imageElement = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.height(30);
            }).style(style -> style.backgroundTexture(messageImageTexture(message)));
            bubble.addChildren(imageElement);
        }

        // 文字内容（纯图片消息不重复显示 [image]）
        if (!isImageOnly) {
            bubble.addChildren(createBodyLabel(message.getBody()));
        }

        row.addChildren(bubble);
        return row;
    }

    private IGuiTexture messageImageTexture(ChatRoomMessage message) {
        Optional<ResourceLocation> texture = PhonePhotoAlbum.textureForMessageData(message.getMessageId(), message.getImageData());
        return texture.<IGuiTexture>map(SpriteTexture::of).orElseGet(() -> new ColorRectTexture(0xFF312D3A));
    }

    private Button createAttachButton() {
        Button button = createCompactButton("smartPhone.ui.app.chatRoom.attach", 18, 0x33000000);
        button.setId("chat_room_attach");
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            showPhotoPicker();
        });
        return button;
    }

    private void showPhotoPicker() {
        showingPhotoPicker = true;
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(createPhotoPickerBackRow());

        List<PhonePhoto> photos = PhonePhotoAlbum.listPhotos();
        if (photos.isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.chatRoom.noPhotos"), 6, ColorPattern.T_WHITE.color, 14, Horizontal.CENTER));
            return;
        }
        appScrollView.viewContainer.addChildren(createPhotoPickerGrid(photos));
    }

    private UIElement createPhotoPickerBackRow() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(15);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(0x22000000)));
        row.addChildren(createFixedLabel(Component.translatable("smartPhone.ui.app.chatRoom.backChat"), 5, ColorPattern.WHITE.color, 60, 10, Horizontal.CENTER));
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            showingPhotoPicker = false;
            showRoomMessages();
        });
        return row;
    }

    private UIElement createPhotoPickerGrid(List<PhonePhoto> photos) {
        UIElement grid = new UIElement().layout(layout -> {
            layout.widthPercent(98);
            layout.flexDirection(FlexDirection.ROW);
            layout.flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingAll(2);
            layout.gapAll(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ROW_BACKGROUND)));
        photos.forEach(photo -> grid.addChildren(createPickerTile(photo)));
        return grid;
    }

    private UIElement createPickerTile(PhonePhoto photo) {
        UIElement tile = new UIElement().layout(layout -> {
            layout.width(32);
            layout.height(34);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });
        tile.setId("chat_room_photo_" + safeSelectorId(photo.fileName()));
        UIElement thumbnail = new UIElement().layout(layout -> {
            layout.width(28);
            layout.height(22);
        }).style(style -> style.backgroundTexture(photoTexture(photo)));
        tile.addChildren(thumbnail);
        tile.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            sendPhotoMessage(photo);
        });
        return tile;
    }

    private String safeSelectorId(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private IGuiTexture photoTexture(PhonePhoto photo) {
        Optional<ResourceLocation> texture = PhonePhotoAlbum.textureFor(photo);
        return texture.<IGuiTexture>map(SpriteTexture::of).orElseGet(() -> new ColorRectTexture(0xFF312D3A));
    }

    private void sendPhotoMessage(PhonePhoto photo) {
        if (selectedRoomId == null) return;
        Optional<byte[]> thumbnailBytes = PhonePhotoAlbum.createThumbnailBytes(photo);
        if (thumbnailBytes.isEmpty()) {
            Toast.show(this, Component.translatable("smartPhone.ui.app.chatRoom.imageFailed"), 1.2f);
            showingPhotoPicker = false;
            showRoomMessages();
            return;
        }
        SmartPhoneClientUtil.sendChatRoomImage(selectedRoomId, thumbnailBytes.get());
        showingPhotoPicker = false;
        showRoomMessages();
    }

    private Button createSendButton() {
        Button button = createCompactButton("smartPhone.ui.app.chatRoom.send", 26, 0x33000000);
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) return;
            sendDraft();
        });
        return button;
    }

    private Button createCompactButton(String key, float width, int backgroundColor) {
        Button button = new Button();
        button.layout(layout -> {
            layout.width(width);
            layout.height(12);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(backgroundColor)));
        button.text.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.marginHorizontal(0);
        });
        button.textStyle(textStyle -> {
            textStyle.fontSize(3.8f);
            textStyle.textColor(ColorPattern.WHITE.color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        button.text.setOverflowVisible(false);
        button.setText(key);
        return button;
    }

    private Button createFlexibleButton(String key, int backgroundColor) {
        Button button = new Button();
        button.layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.height(12);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(backgroundColor)));
        button.text.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.marginHorizontal(0);
        });
        button.textStyle(textStyle -> {
            textStyle.fontSize(4.2f);
            textStyle.textColor(ColorPattern.WHITE.color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        button.text.setOverflowVisible(false);
        button.setText(key);
        return button;
    }

    private void sendDraft() {
        if (selectedRoomId == null) return;
        String body = normalize(draftText);
        if (body.isBlank()) {
            Toast.show(this, Component.translatable("smartPhone.ui.app.chatRoom.emptyMessage"), 1.2f);
            return;
        }
        SmartPhoneClientUtil.sendChatRoomMessage(selectedRoomId, body);
        draftText = "";
        inputArea.setLines(List.of(""));
    }

    private Label createBodyLabel(String body) {
        Label label = createInlineLabel(Component.literal(body == null ? "" : body), 5, ColorPattern.WHITE.color, 8, Horizontal.LEFT);
        label.textStyle(textStyle -> {
            textStyle.fontSize(5);
            textStyle.textColor(ColorPattern.WHITE.color);
            textStyle.textWrap(TextWrap.WRAP);
            textStyle.adaptiveHeight(true);
            textStyle.textAlignHorizontal(Horizontal.LEFT);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        return label;
    }

    private Label createLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = createInlineLabel(component, fontSize, color, height, horizontal);
        label.layout(layout -> {
            layout.widthPercent(94);
            layout.height(height);
        });
        return label;
    }

    private Label createFlexLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = createInlineLabel(component, fontSize, color, height, horizontal);
        label.layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.height(height);
        });
        return label;
    }

    private Label createFixedLabel(Component component, float fontSize, int color, float width, float height, Horizontal horizontal) {
        Label label = createInlineLabel(component, fontSize, color, height, horizontal);
        label.layout(layout -> {
            layout.width(width);
            layout.height(height);
        });
        return label;
    }

    private Label createInlineLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
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
        return label;
    }

    private String roomPreview(ChatRoomSummary room) {
        String latestPreview = normalize(room.getLatestPreview());
        if (latestPreview.isBlank()) return Component.translatable("smartPhone.ui.app.chatRoom.noMessages").getString();
        // 图片消息占位文字显示为 [照片]
        if ("[image]".equals(latestPreview)) return Component.translatable("smartPhone.ui.app.chatRoom.imagePreview").getString();
        return latestPreview;
    }

    private Component roomTitle(ChatRoomSummary room) {
        return room.getDisplayName() == null || room.getDisplayName().isBlank()
                ? Component.translatable(room.getDisplayNameKey())
                : Component.literal(room.getDisplayName());
    }

    private Component roomTitle(ChatRoomSnapshot room) {
        return room.getDisplayName() == null || room.getDisplayName().isBlank()
                ? Component.translatable(room.getDisplayNameKey())
                : Component.literal(room.getDisplayName());
    }

    private String statusKey(FriendEntry friend) {
        String onlineKey = friend.isOnline() ? "online" : "offline";
        return "smartPhone.ui.app.chatRoom.friend.status.%s.%s".formatted(friend.getStatus(), onlineKey);
    }

    private String displaySender(ChatRoomMessage message) {
        String senderName = message.getSenderName();
        return senderName == null || senderName.isBlank() ? "?" : senderName;
    }

    private boolean isOwnMessage(ChatRoomMessage message) {
        if (minecraft.player == null || message.getSenderUuid() == null) return false;
        UUID self = minecraft.player.getUUID();
        return self.equals(message.getSenderUuid());
    }

    private String compactTime(long millis) {
        if (millis <= 0) return "";
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDate messageDate = instant.atZone(MESSAGE_ZONE).toLocalDate();
        return messageDate.equals(LocalDate.now(MESSAGE_ZONE)) ? CLOCK_FORMATTER.format(instant) : DATE_FORMATTER.format(instant);
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
