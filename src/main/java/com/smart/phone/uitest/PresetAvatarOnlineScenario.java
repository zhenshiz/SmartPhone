package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.components.PlayerHeadElement;
import com.smart.phone.ui.data.PresetChatsData;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** 验证正版账号的真实皮肤下载；独立分组以免离线回归依赖外部服务。 */
@LDLRegisterClient(name = "preset_avatar_online", group = "smart_phone_online",
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PresetAvatarOnlineScenario implements UIScenario {
    private static final String ROOM = "preset:avatar";

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(25_000).tags("editor", "avatar", "online");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("save editor scale", ctx -> {
            ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
            ClientConfig.EDITOR_GUI_SCALE.set(2);
        }).setHeldItem(ItemStack.EMPTY).server("create fictional sender", ctx -> {
            var owner = SmartPhone.getPhoneSavedData().registerCharacter("头像测试角色");
            var room = new ChatRoom(ROOM, "头像预设");
            room.addMessage(new ChatRoomMessage(ROOM, owner, "张三", "名称不变，只更换头像。"));
            SmartPhone.getPhoneSavedData().getPhoneInfo(owner).getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2),
                    "smart_phone editor \"头像测试角色\"");
        }).awaitElement("#phone_editor").click("#phone_editor_tab_chats").click("#chat_room_preset_avatar")
                .step("select message", ctx -> {
                    var bounds = ctx.el("#chat_message_" + message(ctx).getMessageId()).bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                });
        replace(s, "Notch");
        s.waitUntil("official skin downloaded for absent player", ctx -> avatar(ctx).getSkin(ctx.mc()).getPath().startsWith("skins/"))
                .check("skin override keeps fictional name and owner-side bubble", ctx -> {
                    ctx.put("notchSkin", avatar(ctx).getSkin(ctx.mc()));
                    return message(ctx).getSenderName().equals("张三")
                            && ctx.el("#chat_message_" + message(ctx).getMessageId()).element().getChildren().getLast() == avatar(ctx)
                            && ctx.mc().getConnection().getPlayerInfo("Notch") == null;
                }).screenshot("official_avatar_editor");
        replace(s, "jeb_");
        s.waitUntil("changed ID replaces loaded skin", ctx -> avatar(ctx).getSkin(ctx.mc()).getPath().startsWith("skins/")
                        && !avatar(ctx).getSkin(ctx.mc()).equals(ctx.get("notchSkin")));
        replace(s, "");
        s.check("clearing override restores the character default", ctx -> message(ctx).getAvatarPlayerName().isEmpty()
                        && avatar(ctx).getSkin(ctx.mc()).equals(DefaultPlayerSkin.get(message(ctx).getSenderUuid()).texture()))
                .typeInto("#phone_editor_chat_avatar", "Notch!")
                .check("invalid username characters are rejected", ctx -> message(ctx).getAvatarPlayerName().equals("Notch"))
                .teardown("close and restore scale", ctx -> {
                    ctx.mc().setScreen(null);
                    ClientConfig.EDITOR_GUI_SCALE.set((Integer)ctx.get("editorScale"));
                });
    }

    private static ChatRoomMessage message(TestContext ctx) {
        return ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview().phoneInfo.getOrCreateExtensionData(PresetChatsData.class)
                .findRoom(ROOM).orElseThrow().getMessages().getFirst();
    }

    private static PlayerHeadElement avatar(TestContext ctx) {
        return (PlayerHeadElement)ctx.el("#chat_avatar_" + message(ctx).getMessageId()).element();
    }

    private static void replace(ScenarioBuilder s, String value) {
        s.focus("#phone_editor_chat_avatar").keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL).key(Keys.BACKSPACE);
        if (!value.isEmpty()) s.type(value);
        s.blur();
    }
}
