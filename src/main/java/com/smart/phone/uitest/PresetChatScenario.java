package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.app.ChatRoomApp;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.data.chat.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.util.SmartPhoneServerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@LDLRegisterClient(name = "preset_chat", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PresetChatScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("32a63578-8e1a-48d1-8927-45e836d69407");
    private static final UUID FIRST = UUID.fromString("57365c57-61ac-416a-8ebe-e4659a86a6fb");
    private static final String ROOM = "preset:guide";

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("editor", "chat", "persistence");
    }

    @Override public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY).server("create two channels including trailing empty message", ctx -> {
            PhoneInfo info = new PhoneInfo();
            ChatRoom room = new ChatRoom(ROOM, "Village Guide");
            ChatRoomMessage first = new ChatRoomMessage(ROOM, new UUID(0, 0), "Guide", "Welcome to the village");
            first.setMessageId(FIRST);
            first.setCreatedAtMillis(1000);
            room.addMessage(first);
            room.addMessage(new ChatRoomMessage(ROOM, new UUID(0, 0), "Guide", ""));
            info.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
            info.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(new ChatRoom("preset:shop", "Shopkeeper"));
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "GuidePhone"));
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER);
        }).awaitElement("#phone_editor").click("#phone_editor_tab_chats")
                .check("list summary ignores trailing empty draft", ctx -> ctx.el("#chat_room_preset_guide").element()
                        .selfAndAllChildren().filter(e -> e instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement)
                        .anyMatch(e -> ((com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement)e).getText().getString().contains("Welcome")))
                .step("select channel in preview", ctx -> click(ctx, "#chat_room_preset_guide"))
                .check("channel selection shows its name, without all messages", ctx ->
                        ((TextField)ctx.el("#phone_editor_chat_name").element()).getText().equals("Village Guide")
                                && !ctx.exists("#phone_editor_chat_body"))
                .step("select first bubble", ctx -> click(ctx, "#chat_message_" + FIRST))
                .check("only selected message has fields", ctx -> ctx.count("#phone_editor_chat_body") == 1)
                .focus("#phone_editor_chat_body").keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(Keys.BACKSPACE).type("Edited greeting")
                .check("preview changes with selected message", ctx -> ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview()
                        .phoneInfo.getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow()
                        .getMessages().getFirst().getBody().equals("Edited greeting"))
                .click("#phone_editor_addChatMessage")
                .check("plus creates and selects one draft message", ctx -> ctx.count("#phone_editor_chat_body") == 1
                        && ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview().phoneInfo
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 3)
                .click("#phone_editor_removeMessage")
                .check("minus removes only the selected message", ctx -> !ctx.exists("#phone_editor_chat_body")
                        && ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview().phoneInfo
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 2)
                .step("back to channels", ctx -> click(ctx, "#chat_room_back"))
                .step("select second channel", ctx -> click(ctx, "#chat_room_preset_shop"))
                .check("selection follows the second channel", ctx -> ((TextField)ctx.el("#phone_editor_chat_name").element())
                        .getText().equals("Shopkeeper") && !ctx.exists("#phone_editor_chat_body"))
                .click("#phone_editor_removeChat")
                .check("channel minus removes only selected channel", ctx -> !ctx.exists("#chat_room_preset_shop") && ctx.exists("#chat_room_preset_guide"))
                .step("select remaining channel", ctx -> click(ctx, "#chat_room_preset_guide"))
                .step("select message for capture", ctx -> click(ctx, "#chat_message_" + FIRST))
                .screenshot("ore_selected_message_editor")
                .click("#phone_editor_save")
                .waitUntil("editor save acknowledged", ctx -> !((PhoneEditorUI)ctx.requireUI().ui.rootElement).isDirty())
                .step("close editor", ctx -> ctx.mc().setScreen(null));
        ItemStack phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(phone).waitUntil("bound phone synced", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced() && h.isRaised())
                .step("open production chat app", ctx -> {
                    PhoneUI ui = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    ui.screenContainer.removeChild(ui.lockScreen);
                    ui.homeScreen.openApp(new ChatRoomApp());
                }).step("open saved preset channel", ctx -> click(ctx, "#chat_room_preset_guide"))
                .focus("#chat_room_input")
                .type("A long message that extends beyond the compact phone input width")
                .key(GLFW.GLFW_KEY_ENTER).type(" still on one line")
                .screenshot("compact_single_line_draft")
                .check("single-line composer scrolls the caret without scrollbar widgets", ctx -> {
                    var input = (TextField)ctx.el("#chat_room_input").element();
                    return input.getElementName().equals("text-field") && input.getChildren().isEmpty() && input.getDisplayOffset() > 0
                            && input.getContentHeight() >= input.getTextFieldStyle().fontSize()
                            && input.getText().equals("A long message that extends beyond the compact phone input width still on one line")
                            && composerFits(ctx);
                })
                .step("paste multiple lines", ctx -> ((TextField)ctx.el("#chat_room_input").element()).insertText(" pasted\r\nsecond\nthird"))
                .check("pasted line breaks are converted to spaces", ctx -> {
                    var input = (TextField)ctx.el("#chat_room_input").element();
                    return !input.getText().contains("\n") && !input.getText().contains("\r")
                            && input.getText().endsWith(" pasted second third");
                })
                .keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(Keys.BACKSPACE).type("I can reply here")
                .click("#chat_room_send")
                .waitUntil("server reply is shown without leaving channel", ctx -> ((PhoneUI)ctx.requireUI().ui.rootElement).phoneInfo
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 3)
                .check("input clears after sending and keeps its usable size", ctx -> {
                    var input = (TextField)ctx.el("#chat_room_input").element();
                    return input.isVisible() && input.getText().isEmpty()
                            && input.getContentHeight() >= input.getTextFieldStyle().fontSize() && composerFits(ctx);
                })
                .checkServer("bound owner's phone stores reply and original records", ctx -> {
                    var room = SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow();
                    return room.getMessages().size() == 3 && room.getMessages().getLast().getBody().equals("I can reply here")
                            && room.getMessages().getLast().getSenderUuid().equals(OWNER)
                            && room.getMessages().getFirst().getBody().equals("Edited greeting");
                }).checkServer("reply survives SavedData reload", ctx -> {
                    var provider = ctx.server().registryAccess();
                    return PhoneSavedData.fromNbt(SmartPhone.getPhoneSavedData().save(new CompoundTag(), provider), provider)
                            .getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 3;
                }).screenshot("ore_preset_player_reply")
                .setHeldItem(ItemStack.EMPTY)
                .server("reject writes to another phone without holding it", ctx -> SmartPhoneServerUtil.sendPresetChat(ctx.player(), OWNER,
                        "Unauthorized", new com.smart.phone.network.c2s.ChatRoomImagePayload(ROOM, null)))
                .checkServer("unauthorized write is ignored", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 3)
                .teardown("close phone", ctx -> ctx.mc().setScreen(null));
    }

    private static boolean composerFits(TestContext ctx) {
        var input = ctx.el("#chat_room_input").bounds();
        var attach = ctx.el("#chat_room_attach").bounds();
        var send = ctx.el("#chat_room_send").bounds();
        return input.width() > 0 && input.right() < attach.x() && attach.right() < send.x()
                && Math.abs(input.centerY() - attach.centerY()) < 1
                && Math.abs(attach.centerY() - send.centerY()) < 1
                && input.bottom() < ctx.mc().getWindow().getGuiScaledHeight();
    }

    private static void click(TestContext ctx, String selector) {
        var bounds = ctx.el(selector).bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }
}
