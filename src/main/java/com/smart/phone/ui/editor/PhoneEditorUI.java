package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.Config;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.app.ui.ChatRoomUI;
import com.smart.phone.ui.app.ui.OfficialMessagesUI;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Edits a detached draft of one player's SavedData, with a live, local phone preview. */
public final class PhoneEditorUI extends UIElement {
    private static final String KEY = "smartPhone.editor.";
    private final UUID owner;
    private final UUID session;
    private final String ownerName;
    private final PhoneInfo draft;
    private final ViewContainer parameters = new ViewContainer();
    private final Label status = new Label();
    private final Selector<Integer> sizeSelector = new Selector<>();
    private int interfaceSize = ClientConfig.EDITOR_GUI_SCALE.get();
    private boolean resizePending;
    private Button save;
    private PhoneUI preview;
    private UIElement previewSpace;
    private ScrollerView messageFields;
    private View messagesView;
    private OfficialMessage selectedOfficialMessage;
    private Button removeOfficialMessage;
    private ScrollerView chatFields;
    private View chatView;
    private ChatRoom selectedChat;
    private ChatRoomMessage selectedChatMessage;
    private Button removeChat, addChatMessage, removeChatMessage;
    private int revision;
    private int savedRevision;
    private int submittedRevision;
    private boolean saving;
    private String passcode = "";
    private boolean passcodeEnabled;
    private boolean passcodeEdited;
    private boolean submittedPasscodeUpdate;
    private String submittedPasscode;
    private Button clearPasscode;

    private PhoneEditorUI(UUID owner, String ownerName, UUID session, PhoneInfo draft, boolean passcodeEnabled) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.session = session;
        this.draft = draft;
        this.passcodeEnabled = passcodeEnabled;
        setId("phone_editor");
        addClass("panel_bg");
        layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(4).gapAll(3));
        UIElement toolbar = new UIElement().layout(l -> l.widthPercent(100).height(22)
                .flexShrink(0).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(5));
        Label target = new Label();
        target.setText(Component.translatable(KEY + "target", ownerName));
        target.layout(l -> l.flex(1).minWidth(0));
        target.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.HOVER_ROLL));
        save = button("save", () -> {
            if (saving) return;
            if (!validPasscode()) {
                status.setText(KEY + "passcodeInvalid");
                return;
            }
            saving = true;
            submittedRevision = revision;
            submittedPasscode = passcode;
            submittedPasscodeUpdate = passcodeEdited;
            save.setActive(false);
            status.setText(KEY + "saving");
            RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_EDITOR, owner, session, draft,
                    submittedPasscodeUpdate, submittedPasscode);
        });
        Label sizeLabel = new Label();
        sizeLabel.setText(KEY + "interfaceSize");
        sizeLabel.textStyle(s -> s.adaptiveWidth(true));
        sizeLabel.layout(l -> l.flexShrink(0));
        sizeSelector.setId("phone_editor_size");
        sizeSelector.dialog.setId("phone_editor_size_menu");
        sizeSelector.layout(l -> l.width(48).height(18).flexShrink(0));
        sizeSelector.style(s -> s.tooltips(KEY + "interfaceSize.tooltip", KEY + "interfaceSize.tooltip.scope"));
        sizeSelector.setCandidateUIProvider(value -> new Label()
                .textStyle(s -> s.textWrap(TextWrap.HOVER_ROLL))
                .setText(value == null || value == 0 ? Component.translatable("options.guiScale.auto")
                        : Component.literal(value.toString()))
                .setId("phone_editor_size_option_" + value).setOverflowVisible(false));
        sizeSelector.setOnValueChanged(value -> {
            if (value == null || value == interfaceSize) return;
            interfaceSize = value;
            ClientConfig.EDITOR_GUI_SCALE.set(value);
            ClientConfig.EDITOR_GUI_SCALE.save();
            // 等下拉菜单完成点击后再重排，不替换界面或草稿。
            resizePending = true;
        });
        toolbar.addChildren(target, sizeLabel, sizeSelector, save, button("close", this::requestClose));
        addEventListener(UIEvents.TICK, event -> {
            if (!resizePending || getModularUI() == null) return;
            resizePending = false;
            var screen = getModularUI().getScreen();
            if (screen != null) screen.resize(Minecraft.getInstance(), screen.width, screen.height);
        });
        status.setId("phone_editor_status");
        status.setText(KEY + "ready");
        status.layout(l -> l.widthPercent(100).height(14).flexShrink(0));
        status.textStyle(s -> s.fontSize(8).textWrap(TextWrap.HIDE));

        ViewContainer previewContainer = new ViewContainer();
        View previewView = view("preview");
        previewSpace = new UIElement().layout(l -> l.flex(1).widthPercent(100).minHeight(0)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        previewSpace.setOverflowVisible(false);
        rebuildPreview();
        previewView.addChildren(previewSpace, button("resetPreview", this::rebuildPreview));
        previewContainer.addView(previewView);
        SplitView.Horizontal split = new SplitView.Horizontal().setPercentage(42);
        split.setMinPercentage(28);
        split.setMaxPercentage(60);
        split.left(previewContainer).right(parameters);
        split.layout(l -> l.flex(1).minHeight(0));
        addChildren(toolbar, split, status);
        buildBasics();
        buildApps();
        buildChats();
        buildMessages();
        buildNotes();
    }

    public static void open(UUID owner, String name, UUID session, PhoneInfo draft, boolean passcodeEnabled) {
        PhoneEditorUI editor = new PhoneEditorUI(owner, name, session, draft, passcodeEnabled);
        UI themed = com.smart.phone.ui.PhoneTheme.create(editor, null);
        Minecraft.getInstance().setScreen(new PhoneEditorScreen(new ModularUI(themed), editor));
    }

    public static void receiveResult(UUID session, boolean success) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.getModularUI().ui.rootElement instanceof PhoneEditorUI editor
                && session.equals(editor.session)) {
            editor.saving = false;
            editor.save.setActive(true);
            if (success) {
                editor.savedRevision = editor.submittedRevision;
                if (editor.submittedPasscodeUpdate) {
                    editor.passcodeEnabled = !editor.submittedPasscode.isEmpty();
                    editor.clearPasscode.setDisplay(editor.passcodeEnabled);
                    if (editor.passcode.equals(editor.submittedPasscode)) editor.passcodeEdited = false;
                }
            }
            editor.status.setText(KEY + (!success ? "saveFailed" : editor.isDirty() ? "dirty" : "saved"));
        }
    }

    @Override
    public void initScreen(int width, int height) {
        var mc = Minecraft.getInstance();
        int maximum = mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
        sizeSelector.setCandidates(java.util.stream.IntStream.rangeClosed(0, maximum).boxed().toList());
        sizeSelector.setSelected(Math.min(interfaceSize, maximum), false);
        super.initScreen(width, height);
    }

    public boolean isDirty() { return revision != savedRevision; }
    public UUID getOwner() { return owner; }
    public PhoneUI getPreview() { return preview; }

    void requestClose() {
        if (saving) return;
        if (!isDirty()) Minecraft.getInstance().setScreen(null);
        else Dialog.showCheckBox(KEY + "discardTitle", KEY + "discard", confirmed -> {
            if (confirmed) Minecraft.getInstance().setScreen(null);
        }).show(this);
    }

    private void markDirty() {
        revision++;
        status.setText(KEY + "dirty");
    }

    private void previewChanged() {
        markDirty();
        if (messageFields != null) rebuildMessageFields();
    }

    private void changed() {
        markDirty();
        if (preview.homeScreen.appUI instanceof ChatRoomUI chat) chat.refreshFromState();
        else if (preview.homeScreen.appUI instanceof OfficialMessagesUI messages) messages.refreshFromData();
        else if (preview.homeScreen.iApp != null) preview.homeScreen.openApp(preview.homeScreen.iApp);
        preview.homeScreen.reloadAppView();
    }

    private void rebuildPreview() {
        previewSpace.clearAllChildren();
        preview = new PhoneUI(draft);
        preview.configurePreview(owner, ownerName, this::previewChanged);
        preview.setChatSelection(this::selectChat);
        preview.setOfficialMessageSelection(this::selectOfficialMessage);
        preview.addEventListener(UIEvents.TICK, event -> {
            float scale = Math.max(0.1f, Math.min((previewSpace.getSizeWidth() - 12) / PhoneUI.SHELL_WIDTH,
                    (previewSpace.getSizeHeight() - 12) / PhoneUI.SHELL_HEIGHT));
            preview.transform(t -> t.pivot(0.5f, 0.5f).scale(scale));
        });
        previewSpace.addChild(preview);
    }

    private View view(String name) {
        View view = new View(KEY + name);
        view.setFloatable(false);
        view.layout(l -> l.minHeight(0));
        return view;
    }

    private ScrollerView parameterView(String name) {
        View view = view(name);
        ScrollerView scroll = new ScrollerView();
        scroll.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        scroll.viewContainer.layout(l -> l.paddingAll(5).gapAll(5));
        view.addChild(scroll);
        parameters.addView(view);
        parameters.tabView.getTabContents().inverse().get(view).setId("phone_editor_tab_" + name);
        return scroll;
    }

    private ConfiguratorGroup fieldsGroup() {
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(false);
        group.layout(l -> l.widthPercent(100));
        return group;
    }

    private void buildBasics() {
        ScrollerView scroll = parameterView("basics");
        ConfiguratorGroup fields = fieldsGroup();
        draft.buildConfigurator(fields);
        fields.addEventListener(Configurator.CHANGE_EVENT, event -> changed());
        UIElement security = new UIElement().layout(l -> l.widthPercent(100).height(20)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(3));
        Label label = new Label();
        label.setText(KEY + "passcode");
        label.layout(l -> l.width(50).height(18).flexShrink(0));
        TextField password = new TextField() {
            @Override
            public String name() { return "text-field"; }

            @Override
            public void insertText(String text) {
                int selected = Math.abs(getSelectionEnd() - getSelectionStart());
                if (text.matches("[0-9]*") && getRawText().length() - selected + text.length() <= 6) {
                    super.insertText(text);
                }
            }
        };
        password.setId("phone_editor_passcode");
        password.layout(l -> l.flex(1).minWidth(0).height(18));
        password.textFieldStyle(s -> s.placeholder(Component.translatable(KEY + "passcodePlaceholder")));
        password.setTextResponder(value -> {
            passcode = value;
            passcodeEdited = true;
            markDirty();
        });
        clearPasscode = button("clearPasscode", () -> {
            password.setText("");
            passcode = "";
            passcodeEdited = true;
            markDirty();
        });
        clearPasscode.setDisplay(passcodeEnabled);
        security.addChildren(label, password, clearPasscode);
        scroll.addScrollViewChildren(hint("basicsHelp"), security, fields);
    }

    private boolean validPasscode() {
        return passcode.isEmpty() || com.smart.phone.security.PhonePasscode.valid(passcode);
    }

    private void buildApps() {
        ScrollerView scroll = parameterView("apps");
        scroll.addScrollViewChildren(hint("appsHelp"));
        ConfiguratorGroup fields = fieldsGroup();
        SmartPhoneRegistries.APPS.forEach(holder -> {
            var app = holder.value().get();
            if (!Config.isAppEnabled(app.name())) return;
            BooleanConfigurator toggle = new BooleanConfigurator(app.getDisplayName().getString(),
                    () -> draft.getInstalledApps().stream().anyMatch(installed -> installed.name().equals(app.name())),
                    installed -> {
                        draft.getInstalledApps().removeIf(existing -> existing.name().equals(app.name()));
                        if (installed) draft.getInstalledApps().add(app);
                        changed();
                    }, app.isDefaultInstalled(), true);
            if (app.isDefaultInstalled()) toggle.setActive(false);
            fields.addConfigurator(toggle);
        });
        scroll.addScrollViewChildren(fields);
    }

    private void buildChats() {
        chatView = view("chats");
        chatFields = new ScrollerView();
        chatFields.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        chatFields.viewContainer.layout(l -> l.paddingAll(5).gapAll(5));
        UIElement actions = new UIElement().layout(l -> l.widthPercent(100).height(24).paddingAll(3)
                .flexShrink(0).flexDirection(FlexDirection.ROW).justifyContent(AlignContent.FLEX_END).gapAll(3));
        Button addChat = iconButton("addChat", "+", () -> {
            ChatRoom room = new ChatRoom("preset:" + UUID.randomUUID(), Component.translatable(KEY + "newChat").getString());
            draft.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
            selectedChat = room;
            selectedChatMessage = null;
            changed();
            showChatPreview(null);
            rebuildChatFields();
        });
        removeChat = iconButton("removeChat", "−", () -> {
            if (selectedChat == null) return;
            draft.getOrCreateExtensionData(PresetChatsData.class).getRooms().remove(selectedChat);
            selectedChat = null;
            selectedChatMessage = null;
            changed();
            showChatPreview(null);
            rebuildChatFields();
        });
        Button addFriend = button("addPresetFriend", () -> {
            String name = Component.translatable(KEY + "newFriend").getString();
            ChatRoom room = new ChatRoom("preset:" + UUID.randomUUID(), name);
            room.setPresetFriend(true);
            room.setPresetFriendId(name);
            draft.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
            selectedChat = room;
            selectedChatMessage = null;
            changed();
            showChatPreview(null);
            if (preview.homeScreen.appUI instanceof ChatRoomUI chat) chat.showPresetFriends();
            rebuildChatFields();
        });
        addFriend.layout(l -> l.width(46));
        addChatMessage = iconButton("addChatMessage", "+", () -> {
            if (selectedChat == null) return;
            selectedChatMessage = new ChatRoomMessage(selectedChat.getRoomId(), new UUID(0, 0),
                    selectedChat.isPresetFriend() ? selectedChat.getPresetFriendId() : Component.translatable(KEY + "newSender").getString(), "");
            selectedChat.addMessage(selectedChatMessage);
            changed();
            showChatPreview(selectedChat.getRoomId());
            rebuildChatFields();
        });
        removeChatMessage = iconButton("removeMessage", "−", () -> {
            if (selectedChat == null || selectedChatMessage == null) return;
            selectedChat.getMessages().remove(selectedChatMessage);
            selectedChatMessage = null;
            changed();
            rebuildChatFields();
        });
        actions.addChildren(actionLabel("conversationActions"), addChat, addFriend, removeChat,
                actionLabel("messageActions"), addChatMessage, removeChatMessage);
        chatView.addChildren(chatFields, actions);
        parameters.addView(chatView);
        var tab = parameters.tabView.getTabContents().inverse().get(chatView);
        tab.setId("phone_editor_tab_chats");
        tab.addEventListener(UIEvents.CLICK, e -> showChatPreview(selectedChat == null ? null : selectedChat.getRoomId()));
        rebuildChatFields();
    }

    private Label actionLabel(String name) {
        Label label = new Label();
        label.setText(KEY + name);
        label.textStyle(s -> s.adaptiveWidth(true).textWrap(TextWrap.NONE).fontSize(8));
        label.layout(l -> l.flexShrink(0));
        return label;
    }

    private Button iconButton(String name, String symbol, Runnable action) {
        Button button = button(name, action).setText(Component.literal(symbol));
        button.style(s -> s.tooltips(KEY + name));
        button.layout(l -> l.width(20).height(18));
        return button;
    }

    private void showChatPreview(String roomId) {
        preview.lockScreen.showEditorContent();
        if (!(preview.homeScreen.appUI instanceof ChatRoomUI)) preview.homeScreen.openApp(new com.smart.phone.ui.app.ChatRoomApp());
        if (preview.homeScreen.appUI instanceof ChatRoomUI chat) chat.showPresetRoom(roomId);
    }

    private void selectChat(String roomId, UUID messageId) {
        selectedChat = draft.getOrCreateExtensionData(PresetChatsData.class).findRoom(roomId).orElse(null);
        selectedChatMessage = selectedChat == null ? null : selectedChat.getMessages().stream()
                .filter(message -> message.getMessageId().equals(messageId)).findFirst().orElse(null);
        parameters.selectView(chatView);
        rebuildChatFields();
    }

    private void rebuildChatFields() {
        chatFields.clearAllScrollViewChildren();
        if (preview.homeScreen.appUI instanceof ChatRoomUI chat) {
            chat.selectPreviewMessage(selectedChatMessage == null ? null : selectedChatMessage.getMessageId());
        }
        removeChat.setActive(selectedChat != null);
        addChatMessage.setActive(selectedChat != null);
        removeChatMessage.setActive(selectedChatMessage != null);
        if (selectedChat == null) {
            chatFields.addScrollViewChildren(hint("selectChatHelp"));
            return;
        }
        ChatRoom room = selectedChat;
        ConfiguratorGroup fields = fieldsGroup();
        fields.addConfigurator(string("chatName", room::getDisplayNameKey, room::setDisplayNameKey, "chat_name"));
        if (room.isPresetFriend()) {
            fields.addConfigurator(string("friendPlayerId", room::getPresetFriendId, value -> {
                String previous = room.getPresetFriendId();
                String next = value.trim();
                room.setPresetFriendId(next);
                if (room.getDisplayNameKey().equals(previous)) room.setDisplayNameKey(next);
                room.getMessages().stream().filter(m -> !owner.equals(m.getSenderUuid()) && previous.equals(m.getSenderName()))
                        .forEach(m -> { m.setSenderName(next); m.setSenderUuid(new UUID(0, 0)); });
            }, "friend_id"));
            chatFields.addScrollViewChildren(hint("presetFriendHelp"));
        }
        chatFields.addScrollViewChildren(fields);
        if (selectedChatMessage == null) {
            chatFields.addScrollViewChildren(hint("selectMessageHelp"));
            return;
        }
        ChatRoomMessage message = selectedChatMessage;
        ConfiguratorGroup messageFields = fieldsGroup();
        BooleanConfigurator fromOwner = new BooleanConfigurator(KEY + "fromOwner", () -> owner.equals(message.getSenderUuid()), own -> {
                    message.setSenderUuid(own ? owner : new UUID(0, 0));
                    if (own) message.setSenderName(ownerName);
                    else if (room.isPresetFriend()) message.setSenderName(room.getPresetFriendId());
                    changed();
                }, false, true);
        fromOwner.setId("phone_editor_chat_from_owner");
        StringConfigurator avatar = string("avatarPlayerName", message::getAvatarPlayerName,
                message::setAvatarPlayerName, "chat_avatar");
        avatar.textField.setTextValidator(value -> value.matches("[A-Za-z0-9_]{0,16}"));
        messageFields.addConfigurators(
                string("sender", message::getSenderName, message::setSenderName, "chat_sender"), fromOwner,
                avatar,
                time(message::getCreatedAtMillis, message::setCreatedAtMillis, "chat_time"));
        chatFields.addScrollViewChildren(messageFields, hint("avatarHelp"), body(message::getBody, message::setBody, "chat_body"));
        UIElement imageActions = new UIElement().layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(3));
        Button chooseImage = button("chooseChatImage", () -> {
            showChatPreview(room.getRoomId());
            if (preview.homeScreen.appUI instanceof ChatRoomUI chat) chat.choosePreviewPhoto(data -> {
                message.setImageData(data);
                changed();
                rebuildChatFields();
            });
        });
        chooseImage.layout(l -> l.flex(1));
        Button removeImage = button("removeChatImage", () -> {
            message.setImageData(null);
            changed();
            rebuildChatFields();
        });
        removeImage.layout(l -> l.flex(1));
        boolean hasImage = message.getImageData() != null && message.getImageData().length > 0;
        removeImage.setActive(hasImage);
        imageActions.addChildren(chooseImage, removeImage);
        chatFields.addScrollViewChildren(imageActions);
        if (hasImage) {
            PhonePhotoAlbum.textureForMessageData(message.getMessageId(), message.getImageData()).ifPresent(texture ->
                    chatFields.addScrollViewChildren(new UIElement().setId("phone_editor_chat_image")
                            .layout(l -> l.width(96).height(54).flexShrink(0))
                            .style(s -> s.backgroundTexture(SpriteTexture.of(texture)))));
        }
    }

    private void buildMessages() {
        messagesView = view("messages");
        messageFields = new ScrollerView();
        messageFields.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        messageFields.viewContainer.layout(l -> l.paddingAll(5).gapAll(5));
        UIElement actions = new UIElement().layout(l -> l.widthPercent(100).height(24).paddingAll(3)
                .flexShrink(0).flexDirection(FlexDirection.ROW).justifyContent(AlignContent.FLEX_END).gapAll(3));
        Button add = iconButton("addOfficialMessage", "+", () -> {
            selectedOfficialMessage = new OfficialMessage(ownerName, Component.translatable(KEY + "newMessage").getString(), "");
            draft.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(selectedOfficialMessage);
            changed();
            showMessagesPreview();
            rebuildMessageFields();
        });
        removeOfficialMessage = iconButton("removeOfficialMessage", "−", () -> {
            if (selectedOfficialMessage == null) return;
            draft.getOrCreateExtensionData(OfficialMessagesData.class).deleteMessage(selectedOfficialMessage.getMessageId());
            selectedOfficialMessage = null;
            changed();
            rebuildMessageFields();
        });
        actions.addChildren(actionLabel("messageActions"), add, removeOfficialMessage);
        messagesView.addChildren(messageFields, actions);
        parameters.addView(messagesView);
        var tab = parameters.tabView.getTabContents().inverse().get(messagesView);
        tab.setId("phone_editor_tab_messages");
        tab.addEventListener(UIEvents.CLICK, event -> showMessagesPreview());
        rebuildMessageFields();
    }

    private void showMessagesPreview() {
        preview.lockScreen.showEditorContent();
        if (!(preview.homeScreen.appUI instanceof OfficialMessagesUI)) {
            preview.homeScreen.openApp(new com.smart.phone.ui.app.OfficialMessages());
        }
        if (preview.homeScreen.appUI instanceof OfficialMessagesUI messages) messages.selectPreviewMessage(
                selectedOfficialMessage == null ? null : selectedOfficialMessage.getMessageId());
    }

    private void selectOfficialMessage(UUID messageId) {
        selectedOfficialMessage = draft.getOrCreateExtensionData(OfficialMessagesData.class).findMessage(messageId).orElse(null);
        parameters.selectView(messagesView);
        rebuildMessageFields();
    }

    private void rebuildMessageFields() {
        messageFields.clearAllScrollViewChildren();
        if (preview.homeScreen.appUI instanceof OfficialMessagesUI messages) {
            messages.selectPreviewMessage(selectedOfficialMessage == null ? null : selectedOfficialMessage.getMessageId());
        }
        removeOfficialMessage.setActive(selectedOfficialMessage != null);
        if (selectedOfficialMessage == null) {
            messageFields.addScrollViewChildren(hint("selectOfficialMessageHelp"));
            return;
        }
        OfficialMessage message = selectedOfficialMessage;
        ConfiguratorGroup fields = fieldsGroup();
        BooleanConfigurator read = new BooleanConfigurator(KEY + "read", message::isRead, value -> {
            message.setRead(value); changed();
        }, false, true);
        read.setId("phone_editor_message_read");
        fields.addConfigurators(
                string("sender", message::getSender, message::setSender, "message_sender"),
                string("messageTitle", message::getTitle, message::setTitle, "message_title"),
                time(message::getCreatedAtMillis, message::setCreatedAtMillis, "message_time"), read);
        messageFields.addScrollViewChildren(fields, body(message::getBody, message::setBody, "message_body"));
    }

    private void buildNotes() {
        ScrollerView scroll = parameterView("notes");
        var data = draft.getOrCreateExtensionData(NotepadData.class);
        TextArea area = body(() -> String.join("\n", data.getText()), value -> data.setText(value.split("\n", -1)), "notes");
        area.layout(l -> l.height(160));
        scroll.addScrollViewChildren(hint("notesHelp"), area);
    }

    private StringConfigurator string(String name, Supplier<String> get, Consumer<String> set, String id) {
        StringConfigurator field = new StringConfigurator(KEY + name, get, value -> {
            set.accept(value); changed();
        }, "", true);
        field.textField.setId("phone_editor_" + id);
        return field;
    }

    private Configurator time(Supplier<Long> get, Consumer<Long> set, String id) {
        return new DateTimeConfigurator(get, value -> { set.accept(value); changed(); }, "phone_editor_" + id);
    }

    private TextArea body(Supplier<String> get, Consumer<String> set, String id) {
        TextArea area = new TextArea();
        area.setId("phone_editor_" + id);
        area.layout(l -> l.widthPercent(100).height(50).flexShrink(0).paddingAll(3));
        area.textAreaStyle(s -> s.fontSize(8));
        area.setLines(List.of(get.get().split("\n", -1)));
        area.setLinesResponder(lines -> { set.accept(String.join("\n", lines)); changed(); });
        area.addEventListener(UIEvents.TICK, event -> {
            String value = get.get();
            if (!String.join("\n", area.getLines()).equals(value)) area.setLines(value.split("\n", -1), false);
        });
        return area;
    }

    private Label hint(String name) {
        Label label = new Label();
        label.setText(KEY + name).textStyle(s -> s.fontSize(8).textWrap(TextWrap.WRAP).adaptiveHeight(true));
        label.layout(l -> l.widthPercent(100));
        return label;
    }

    private Button button(String name, Runnable action) {
        Button button = new Button().setText(KEY + name).setOnClick(event -> action.run());
        button.setId("phone_editor_" + name);
        button.layout(l -> l.height(18).flexShrink(0));
        return button;
    }
}
