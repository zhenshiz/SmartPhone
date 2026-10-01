package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.app.ChatRoomApp;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.data.chat.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/** Covers authored fictional friends, album images and avatars through production UI and SavedData. */
@LDLRegisterClient(name = "preset_chat_media", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PresetChatMediaScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("880a1a16-8b91-4ab2-ae9e-a2d808ef9ab7");
    private static final String FRIEND = "StoryGuide_27";
    private static final String PHOTO_A = "ldtest_preset_media_a.png";
    private static final String PHOTO_B = "ldtest_preset_media_b.png";

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("editor", "chat", "persistence");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("create isolated album fixtures", ctx -> {
            ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
            ClientConfig.EDITOR_GUI_SCALE.set(2);
            try {
                createPhoto(PHOTO_A, 0xFF2277DD);
                createPhoto(PHOTO_B, 0xFF55BB33);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        }).setHeldItem(ItemStack.EMPTY).server("open editor for a real owner without the fictional friend", ctx -> {
            PhoneInfo info = new PhoneInfo();
            info.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(new ChatRoom("preset:channel", "Village channel"));
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "MediaOwner"));
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER);
        }).awaitElement("#phone_editor").click("#phone_editor_tab_chats")
                .click("#phone_editor_addPresetFriend")
                .check("new friend appears only under friends", ctx -> ctx.exists("#phone_editor_friend_id")
                        && !ctx.exists("#chat_room_preset_channel") && room(ctx).isPresetFriend());
        replace(s, "friend_id", FRIEND);
        s.check("fictional ID and conversation name update", ctx -> room(ctx).getPresetFriendId().equals(FRIEND)
                        && room(ctx).getDisplayNameKey().equals(FRIEND))
                .click("#phone_editor_addChatMessage");
        replace(s, "chat_body", "Meet me beside the village gate.");
        s.check("new message uses the fictional sender", ctx -> room(ctx).getMessages().getFirst().getSenderName().equals(FRIEND))
                .click("#phone_editor_chooseChatImage")
                .check("editor reuses album picker inside phone", ctx -> ctx.exists(photoSelector(PHOTO_A))
                        && !ctx.el("#chat_room_send").element().getParent().isDisplayed())
                .step("choose first album photo", ctx -> click(ctx, photoSelector(PHOTO_A)))
                .check("image and text coexist on selected message", ctx -> {
                    var message = room(ctx).getMessages().getFirst();
                    ctx.put("firstTexture", PhonePhotoAlbum.textureForMessageData(message.getMessageId(), message.getImageData()).orElseThrow());
                    return Arrays.equals(message.getImageData(), bytes(PHOTO_A)) && ctx.exists("#chat_image_" + message.getMessageId())
                            && message.getBody().equals("Meet me beside the village gate.");
                }).click("#phone_editor_chooseChatImage")
                .step("replace photo on the same message", ctx -> click(ctx, photoSelector(PHOTO_B)))
                .check("replacing photo updates texture cache without changing message ID", ctx -> {
                    var message = room(ctx).getMessages().getFirst();
                    return Arrays.equals(message.getImageData(), bytes(PHOTO_B))
                            && !PhonePhotoAlbum.textureForMessageData(message.getMessageId(), message.getImageData()).orElseThrow().equals(ctx.get("firstTexture"));
                }).click("#phone_editor_removeChatImage")
                .check("remove image preserves text", ctx -> room(ctx).getMessages().getFirst().getImageData() == null
                        && room(ctx).getMessages().getFirst().getBody().equals("Meet me beside the village gate."))
                .click("#phone_editor_chooseChatImage")
                .step("choose final photo", ctx -> click(ctx, photoSelector(PHOTO_B)))
                .click("#phone_editor_addChatMessage")
                .click("#phone_editor_chat_from_owner toggle button");
        replace(s, "chat_body", "I will be there soon.");
        replace(s, "chat_avatar", "Notch");
        s.check("avatar override preserves sender identity and other messages", ctx ->
                room(ctx).getMessages().getLast().getAvatarPlayerName().equals("Notch")
                        && room(ctx).getMessages().getLast().getSenderName().equals("MediaOwner")
                        && room(ctx).getMessages().getFirst().getAvatarPlayerName().isEmpty());
        s.check("incoming avatar is left, owner's avatar is right", ctx -> {
                    var messages = room(ctx).getMessages();
                    var incoming = ctx.el("#chat_message_" + messages.getFirst().getMessageId()).element();
                    var outgoing = ctx.el("#chat_message_" + messages.getLast().getMessageId()).element();
                    return incoming.getChildren().getFirst().getId().startsWith("chat_avatar_")
                            && outgoing.getChildren().getLast().getId().startsWith("chat_avatar_")
                            && messages.getLast().getSenderUuid().equals(OWNER);
                }).step("select image message again", ctx -> click(ctx, "#chat_message_" + room(ctx).getMessages().getFirst().getMessageId()))
                .screenshot("friend_image_editor")
                .step("back to friends", ctx -> click(ctx, "#chat_room_back"))
                .step("channels tab", ctx -> click(ctx, "#chat_room_tab_rooms"))
                .check("channels and fictional friends stay separate", ctx -> ctx.exists("#chat_room_preset_channel")
                        && !ctx.exists("#chat_room_" + room(ctx).getRoomId().replace(':', '_')))
                .click("#phone_editor_save")
                .waitUntil("save acknowledged", ctx -> !editor(ctx).isDirty())
                .checkServer("fictional friend and binary image survive SavedData reload", ctx -> {
                    var provider = ctx.server().registryAccess();
                    var saved = SmartPhone.getPhoneSavedData();
                    var reloaded = PhoneSavedData.fromNbt(saved.save(new CompoundTag(), provider), provider);
                    var original = friend(saved.getPhoneInfo(OWNER));
                    var copy = friend(reloaded.getPhoneInfo(OWNER));
                    return copy.getPresetFriendId().equals(FRIEND) && copy.getMessages().size() == 2
                            && copy.getMessages().getFirst().getImageData().length > 0
                            && Arrays.equals(copy.getMessages().getFirst().getImageData(), original.getMessages().getFirst().getImageData())
                            && copy.getMessages().getLast().getSenderUuid().equals(OWNER)
                            && copy.getMessages().getLast().getAvatarPlayerName().equals("Notch");
                }).step("close editor and remove source photos", ctx -> {
                    ctx.mc().setScreen(null);
                    deletePhoto(PHOTO_A); deletePhoto(PHOTO_B);
                });
        ItemStack phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(phone).waitUntil("held phone synced", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced() && h.isRaised())
                .step("open normal chat app", ctx -> {
                    var ui = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    ui.screenContainer.removeChild(ui.lockScreen);
                    ui.homeScreen.openApp(new ChatRoomApp());
                }).step("open friend tab", ctx -> click(ctx, "#chat_room_tab_friends"))
                .step("open authored friend", ctx -> {
                    var info = ((HeldPhoneScreen)ctx.screen()).getPhoneUI().phoneInfo;
                    click(ctx, "#chat_room_" + friend(info).getRoomId().replace(':', '_'));
                }).check("saved photo renders without local source file", ctx -> ctx.count(".phone_bubble") == 1
                        && ctx.count(".phone_bubble_self") == 1 && ctx.exists("#chat_room_input"))
                .check("avatar override is synced to the normal phone", ctx ->
                        friend(((HeldPhoneScreen)ctx.screen()).getPhoneUI().phoneInfo).getMessages().getLast()
                                .getAvatarPlayerName().equals("Notch"))
                .check("portrait phone and message controls remain within screen", ctx -> {
                    var phoneUI = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    var screen = phoneUI.screenContainer;
                    float aspect = screen.getSizeHeight() / screen.getSizeWidth();
                    return aspect > 1.9f && aspect < 2f
                            && ctx.el("#chat_room_send").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height);
                }).focus("#chat_room_input").type("A reply to my fictional friend")
                .click("#chat_room_send")
                .waitUntil("reply returns to held phone", ctx -> friend(((HeldPhoneScreen)ctx.screen()).getPhoneUI().phoneInfo).getMessages().size() == 3)
                .checkServer("only the player reply is appended", ctx -> {
                    var messages = friend(SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)).getMessages();
                    return messages.size() == 3 && messages.getLast().getBody().equals("A reply to my fictional friend")
                            && messages.getLast().getSenderUuid().equals(OWNER);
                }).screenshot("friend_image_player_reply")
                .setHeldItem(ItemStack.EMPTY)
                .teardown("restore client settings and remove fixtures", ctx -> {
                    ctx.mc().setScreen(null);
                    Integer scale = ctx.get("editorScale");
                    if (scale != null) ClientConfig.EDITOR_GUI_SCALE.set(scale);
                    deletePhoto(PHOTO_A); deletePhoto(PHOTO_B);
                });
    }

    private static PhoneEditorUI editor(TestContext ctx) { return (PhoneEditorUI) ctx.requireUI().ui.rootElement; }
    private static ChatRoom room(TestContext ctx) { return friend(editor(ctx).getPreview().phoneInfo); }
    private static ChatRoom friend(PhoneInfo info) {
        return info.getOrCreateExtensionData(PresetChatsData.class).getRooms().stream().filter(ChatRoom::isPresetFriend).findFirst().orElseThrow();
    }
    private static void replace(ScenarioBuilder s, String id, String text) {
        s.focus("#phone_editor_" + id).keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL).key(Keys.BACKSPACE).type(text);
    }
    private static void click(TestContext ctx, String selector) {
        var bounds = ctx.el(selector).bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }
    private static String photoSelector(String name) { return "#chat_room_photo_" + name.replace('.', '_'); }
    private static Path path(String name) { return PhonePhotoAlbum.photoDirectory().resolve(name); }
    private static void deletePhoto(String name) { PhonePhotoAlbum.delete(new PhonePhoto(path(name), 0, 0)); }
    private static byte[] bytes(String name) { return PhonePhotoAlbum.createThumbnailBytes(new PhonePhoto(path(name), 0, 0)).orElseThrow(); }
    private static void createPhoto(String name, int color) throws Exception {
        Files.createDirectories(path(name).getParent());
        try (NativeImage image = new NativeImage(160, 90, false)) {
            for (int y = 0; y < 90; y++) for (int x = 0; x < 160; x++) image.setPixelRGBA(x, y, color + ((x / 20 + y / 15) % 2) * 0x000C0C0C);
            image.writeToFile(path(name));
        }
    }
}
