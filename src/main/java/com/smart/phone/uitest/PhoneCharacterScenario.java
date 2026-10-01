package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.security.PhonePasscode;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.util.PhoneOwnerResolver;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Exercises named fictional owners through commands, item components and SavedData. */
@LDLRegisterClient(name = "phone_character", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneCharacterScenario implements UIScenario {
    private static final String NAME = "张三";
    private static final java.util.UUID OWNER = PhoneOwnerResolver.characterId(NAME);

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "owner", "persistence");
    }

    @Override public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY);
        command(s, "smart_phone character register \"" + NAME + "\"");
        s.checkServer("character registered without a Minecraft player profile", ctx -> {
            var data = SmartPhone.getPhoneSavedData();
            return OWNER.equals(data.getCharacters().find(NAME)) && data.hasPhoneInfo(OWNER)
                    && ctx.server().getPlayerList().getPlayer(OWNER) == null;
        }).server("seed character phone content", ctx -> {
            var data = SmartPhone.getPhoneSavedData();
            data.getPhoneInfo(OWNER).getOrCreateExtensionData(NotepadData.class).setText(new String[]{"Character note"});
            data.getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class).getRooms()
                    .add(new ChatRoom("preset:character", "Character channel"));
        });
        command(s, "smart_phone character register \"" + NAME + "\"");
        s.checkServer("duplicate registration preserves existing contents", ctx ->
                SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).getOrCreateExtensionData(NotepadData.class).getText()[0].equals("Character note"));
        command(s, "smart_phone editor \"" + NAME + "\"");
        s.awaitElement("#phone_editor").check("editor targets character by name", ctx ->
                        ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getOwner().equals(OWNER)
                                && ((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview().getOwnerName().equals(NAME))
                .click("#phone_editor_tab_messages").click("#phone_editor_addOfficialMessage")
                .click("#phone_editor_save").waitForText("#phone_editor_status", "已保存，该玩家的手机已更新。")
                .checkServer("editor writes character content", ctx -> !SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(OfficialMessagesData.class).getMessages().isEmpty())
                .step("close editor", ctx -> ctx.mc().setScreen(null));
        command(s, "smart_phone config \"" + NAME + "\" hide_owner_name true");
        command(s, "smart_phone config \"" + NAME + "\" hide_lock_icon true");
        s.server("add credential for persistence check", ctx -> SmartPhone.getPhoneSavedData().setPasscode(OWNER, PhonePasscode.create("001234")))
                .checkServer("character registry, settings, content and credentials survive reload", ctx -> {
                    var registry = ctx.server().registryAccess();
                    var data = PhoneSavedData.fromNbt(SmartPhone.getPhoneSavedData().save(new CompoundTag(), registry), registry);
                    return OWNER.equals(data.getCharacters().find(NAME)) && NAME.equals(data.getCharacters().name(OWNER))
                            && data.getPhoneInfo(OWNER).isHideOwnerName() && data.getPhoneInfo(OWNER).isHideLockIcon()
                            && data.getPhoneInfo(OWNER).getOrCreateExtensionData(NotepadData.class).getText()[0].equals("Character note")
                            && data.getPasscode(OWNER).matches("001234");
                }).checkServer("legacy UUID saves and item components remain readable", ctx -> {
                    var registry = ctx.server().registryAccess();
                    var playerId = ctx.player().getUUID();
                    var legacy = new CompoundTag();
                    legacy.put(playerId.toString(), new PhoneInfo().serializeNBT(registry));
                    var loaded = PhoneSavedData.fromNbt(legacy, registry);
                    var oldComponent = UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, playerId).getOrThrow();
                    return loaded.hasPhoneInfo(playerId) && loaded.getCharacters().names().isEmpty()
                            && PhoneOwnerResolver.COMPONENT_CODEC.parse(NbtOps.INSTANCE, oldComponent).getOrThrow().equals(playerId);
                }).server("remove test passcode", ctx -> SmartPhone.getPhoneSavedData().setPasscode(OWNER, null));
        command(s, "item replace entity @s weapon.mainhand with smart_phone:phone[smart_phone:phone_owner=\"" + NAME + "\"]");
        s.waitUntil("named item component opens character phone", ctx -> ctx.screen() instanceof HeldPhoneScreen h
                        && h.isSynced() && h.isRaised() && OWNER.equals(h.getPhoneUI().getOwnerUuid()))
                .check("character content and display flags reach phone", ctx -> {
                    var phone = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    return NAME.equals(phone.getOwnerName()) && phone.phoneInfo.isHideOwnerName() && phone.phoneInfo.isHideLockIcon()
                            && phone.phoneInfo.getOrCreateExtensionData(NotepadData.class).getText()[0].equals("Character note");
                });
        command(s, "smart_phone config \"" + NAME + "\" hide_owner_name false");
        s.waitUntil("owner name visible on actual standby", ctx -> ctx.el("#phone_owner_name").element().isVisible())
                .checkTextContains("#phone_owner_name", NAME)
                .screenshot("character_owner_standby")
                .check("tooltip names the fictional owner", ctx -> ctx.player().getMainHandItem()
                        .getTooltipLines(Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL).stream()
                        .anyMatch(line -> line.getString().contains(NAME)))
                .step("swipe character phone", ctx -> {
                    var b = ctx.el("#held_phone_lock").bounds();
                    ctx.input().mouseDown(b.centerX(), b.y() + b.height() * .85f, 0);
                    ctx.input().dragTo(b.centerX(), b.y() + b.height() * .05f, 0);
                    ctx.input().mouseUp(b.centerX(), b.y() + b.height() * .05f, 0);
                }).waitUntil("character phone unlocked", ctx -> ((HeldPhoneScreen)ctx.screen()).getPhoneUI().lockScreen.getStyle().opacity() == 0)
                .click("#phone_app_smart_phone_chat_room").click("#chat_room_preset_character")
                .focus("#chat_room_input").type("Reply as character").click("#chat_room_send")
                .waitUntilServer("character owns preset reply", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom("preset:character").orElseThrow().getMessages().size() == 1)
                .checkServer("reply sender is character rather than holder", ctx -> {
                    var message = SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class)
                            .findRoom("preset:character").orElseThrow().getMessages().getFirst();
                    return message.getSenderUuid().equals(OWNER) && message.getSenderName().equals(NAME);
                }).click("#chat_room_back").click("#chat_room_global")
                .focus("#chat_room_input").type("Public character message").click("#chat_room_send")
                .waitUntilServer("public chat uses phone owner identity", ctx -> SmartPhone.getChatRoomSavedData()
                        .createRoomSnapshot("global").getMessages().stream().anyMatch(m -> m.getSenderUuid().equals(OWNER)
                                && m.getSenderName().equals(NAME) && m.getBody().equals("Public character message")))
                .setHeldItem(ItemStack.EMPTY).waitUntil("put character phone away", ctx -> ctx.screen() == null)
                .setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                .waitUntil("unbound phone opens holder data", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced())
                .checkServer("unbound phone still binds to real holder", ctx -> ctx.player().getUUID().equals(
                        ctx.player().getMainHandItem().get(SmartPhoneRegistries.PHONE_OWNER.get())));
        command(s, "smart_phone bind \"" + NAME + "\"");
        s.waitUntil("bind command switches phone to fictional owner", ctx -> ctx.screen() instanceof HeldPhoneScreen h
                        && h.isSynced() && h.getPhoneUI().getOwnerUuid().equals(OWNER))
                .checkServer("holder phone data stays separate", ctx -> !SmartPhone.getPhoneSavedData().getPhoneInfo(ctx.player())
                        .getOrCreateExtensionData(NotepadData.class).getText()[0].equals("Character note"))
                .teardown("close phone", ctx -> ctx.mc().setScreen(null))
                .teardownServer("clear held fixture", ctx -> ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }

    private static void command(ScenarioBuilder s, String command) {
        s.server(command, ctx -> ctx.server().getCommands().performPrefixedCommand(
                ctx.player().createCommandSourceStack().withPermission(2), command));
    }
}
