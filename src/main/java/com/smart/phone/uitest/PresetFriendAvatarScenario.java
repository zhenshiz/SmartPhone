package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.authlib.GameProfile;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.app.ChatRoomApp;
import com.smart.phone.ui.components.PlayerHeadElement;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import com.smart.phone.ui.data.PresetChatsData;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@LDLRegisterClient(name = "preset_friend_avatar", group = "smart_phone_online",
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PresetFriendAvatarScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("ebca6d1a-52ae-4a70-9a4e-dae37e93fe41");
    private static final String ROOM = "preset:friend_avatar";
    private static final String ROW = "#chat_room_preset_friend_avatar";
    private static final String AVATAR = "#chat_friend_avatar_preset_friend_avatar";

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(25_000).tags("editor", "avatar", "online");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("save editor scale", ctx -> {
            ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
            ClientConfig.EDITOR_GUI_SCALE.set(2);
        }).setHeldItem(ItemStack.EMPTY).server("create friend with independent avatar and later owner reply", ctx -> {
            PhoneInfo info = new PhoneInfo();
            ChatRoom room = friend(ROOM, "张三");
            ChatRoomMessage incoming = new ChatRoomMessage(ROOM, null, "张三", "好友头像应在列表和聊天中一致。");
            incoming.setAvatarPlayerName("Notch");
            incoming.setCreatedAtMillis(1000);
            room.addMessage(incoming);
            ChatRoomMessage outgoing = new ChatRoomMessage(ROOM, OWNER, "AvatarOwner", "自己的回复不能改变好友头像。");
            outgoing.setAvatarPlayerName("jeb_");
            outgoing.setCreatedAtMillis(2000);
            room.addMessage(outgoing);
            ChatRoom empty = friend("preset:empty_friend", "李四");
            ChatRoom ownerOnly = friend("preset:owner_only", "王五");
            ownerOnly.addMessage(new ChatRoomMessage(ownerOnly.getRoomId(), OWNER, "AvatarOwner", "只有自己的消息"));
            info.getOrCreateExtensionData(PresetChatsData.class).getRooms().addAll(java.util.List.of(room, empty, ownerOnly));
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "AvatarOwner"));
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2),
                    "smart_phone editor " + OWNER);
        }).awaitElement("#phone_editor").click("#phone_editor_tab_chats").click("#chat_room_tab_friends")
                .waitUntil("friend list loads official skin before entering the conversation", ctx ->
                        head(ctx, AVATAR).getSkin(ctx.mc()).getPath().startsWith("skins/"))
                .check("empty and owner-only conversations keep their friend's default avatar", ctx ->
                        head(ctx, "#chat_friend_avatar_preset_empty_friend").getSkin(ctx.mc()).equals(defaultSkin("李四"))
                                && head(ctx, "#chat_friend_avatar_preset_owner_only").getSkin(ctx.mc()).equals(defaultSkin("王五")))
                .step("remember list avatar", ctx -> ctx.put("notchSkin", head(ctx, AVATAR).getSkin(ctx.mc())))
                .screenshot("friend_list_official_avatar")
                .click(ROW).waitUntil("conversation matches the list", ctx ->
                        messageHead(ctx).getSkin(ctx.mc()).equals(ctx.get("notchSkin")));
        selectIncoming(s);
        replace(s, "jeb_");
        s.waitUntil("message avatar changes to the new official account", ctx ->
                        messageHead(ctx).getSkin(ctx.mc()).getPath().startsWith("skins/")
                                && !messageHead(ctx).getSkin(ctx.mc()).equals(ctx.get("notchSkin")))
                .step("remember changed avatar", ctx -> ctx.put("jebSkin", messageHead(ctx).getSkin(ctx.mc())))
                .click("#chat_room_back")
                .waitUntil("returning list uses changed friend avatar", ctx -> head(ctx, AVATAR).getSkin(ctx.mc()).equals(ctx.get("jebSkin")))
                .click(ROW);
        selectIncoming(s);
        replace(s, "");
        s.click("#chat_room_back").check("clearing avatar restores friend default in list", ctx ->
                        head(ctx, AVATAR).getSkin(ctx.mc()).equals(defaultSkin("张三")))
                .click(ROW);
        selectIncoming(s);
        replace(s, "Notch");
        s.click("#chat_room_back").click("#phone_editor_save")
                .waitUntil("save acknowledged", ctx -> !((PhoneEditorUI)ctx.requireUI().ui.rootElement).isDirty())
                .checkServer("friend avatar survives SavedData serialization", ctx -> {
                    var data = SmartPhone.getPhoneSavedData();
                    var provider = ctx.server().registryAccess();
                    var reloaded = PhoneSavedData.fromNbt(data.save(new CompoundTag(), provider), provider);
                    return reloaded.getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class)
                            .findRoom(ROOM).orElseThrow().getMessages().getFirst().getAvatarPlayerName().equals("Notch");
                }).closeScreen();
        ItemStack phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(phone);
        openFriends(s);
        s.waitUntil("normal held phone list uses saved avatar", ctx -> head(ctx, AVATAR).getSkin(ctx.mc()).equals(ctx.get("notchSkin")))
                .click(ROW).waitUntil("normal chat uses the same avatar", ctx -> messageHead(ctx).getSkin(ctx.mc()).equals(ctx.get("notchSkin")))
                .focus("#chat_room_input").type("Reply from another holder").click("#chat_room_send")
                .waitUntil("reply synced", ctx -> room(ctx).getMessages().size() == 3)
                .click("#chat_room_back")
                .waitUntil("owner reply does not replace friend avatar", ctx -> head(ctx, AVATAR).getSkin(ctx.mc()).equals(ctx.get("notchSkin")))
                .screenshot("held_phone_friend_avatar")
                .setHeldItem(ItemStack.EMPTY).closeScreen().setHeldItem(phone);
        openFriends(s);
        s.waitUntil("reopening retains friend avatar", ctx -> head(ctx, AVATAR).getSkin(ctx.mc()).equals(ctx.get("notchSkin")))
                .setHeldItem(ItemStack.EMPTY)
                .teardown("restore editor scale", ctx -> {
                    ctx.mc().setScreen(null);
                    Integer scale = ctx.get("editorScale");
                    if (scale != null) ClientConfig.EDITOR_GUI_SCALE.set(scale);
                });
    }

    private static ChatRoom friend(String id, String name) {
        ChatRoom room = new ChatRoom(id, name);
        room.setPresetFriend(true);
        room.setPresetFriendId(name);
        return room;
    }

    private static PhoneUI phoneUI(TestContext ctx) {
        return ctx.screen() instanceof HeldPhoneScreen screen ? screen.getPhoneUI()
                : ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview();
    }

    private static ChatRoom room(TestContext ctx) {
        return phoneUI(ctx).phoneInfo.getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow();
    }

    private static PlayerHeadElement head(TestContext ctx, String selector) {
        return (PlayerHeadElement)ctx.el(selector).element();
    }

    private static PlayerHeadElement messageHead(TestContext ctx) {
        return head(ctx, "#chat_avatar_" + room(ctx).getMessages().getFirst().getMessageId());
    }

    private static net.minecraft.resources.ResourceLocation defaultSkin(String name) {
        return DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8))).texture();
    }

    private static void selectIncoming(ScenarioBuilder s) {
        s.step("select friend's message", ctx -> {
            var bounds = ctx.el("#chat_message_" + room(ctx).getMessages().getFirst().getMessageId()).bounds();
            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
        });
    }

    private static void replace(ScenarioBuilder s, String value) {
        s.focus("#phone_editor_chat_avatar").keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL).key(Keys.BACKSPACE);
        if (!value.isEmpty()) s.type(value);
        s.blur();
    }

    private static void openFriends(ScenarioBuilder s) {
        s.waitUntil("held phone synced", ctx -> ctx.screen() instanceof HeldPhoneScreen screen && screen.isSynced() && screen.isRaised())
                .step("open normal chat app", ctx -> {
                    var ui = phoneUI(ctx);
                    ui.screenContainer.removeChild(ui.lockScreen);
                    ui.homeScreen.openApp(new ChatRoomApp());
                }).click("#chat_room_tab_friends");
    }
}
