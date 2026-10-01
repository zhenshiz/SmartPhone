package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.util.PhoneEditorServer;
import com.smart.phone.util.SmartPhoneServerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@LDLRegisterClient(name = "phone_editor", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneEditorScenario implements UIScenario {
    private static final int SELECT_MODIFIER = Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL;
    private static final UUID OWNER = UUID.nameUUIDFromBytes("ldtest-editor-owner".getBytes(StandardCharsets.UTF_8));
    private static final UUID UNKNOWN = UUID.nameUUIDFromBytes("ldtest-editor-unknown".getBytes(StandardCharsets.UTF_8));

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "editor", "persistence");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        float[] swipe = new float[3];
        ItemStack boundPhone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        boundPhone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        scenario.setHeldItem(ItemStack.EMPTY)
                .server("prepare offline target and open editor by player name", ctx -> {
                    SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, new PhoneInfo());
                    ctx.server().getProfileCache().add(new GameProfile(OWNER, "EditorOwner"));
                    ctx.server().getCommands().performPrefixedCommand(
                            ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor EditorOwner");
                })
                .awaitElement("#phone_editor")
                .check("editor targets the requested offline player", ctx ->
                        OWNER.equals(((PhoneEditorUI) ctx.requireUI().ui.rootElement).getOwner()))
                .ticks(3)
                .screenshot("editor_initial")
                .click("#phone_editor_tab_notes")
                .focus("#phone_editor_notes").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Meet at the village.")
                .checkServer("editing a draft does not write SavedData", ctx ->
                        SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).getOrCreateExtensionData(NotepadData.class).getText()[0].isEmpty())
                .click("#phone_editor_tab_chats")
                .click("#phone_editor_addChat")
                .focus("#phone_editor_chat_name").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Village Guide")
                .click("#phone_editor_addChatMessage")
                .focus("#phone_editor_chat_sender").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Guide")
                .focus("#phone_editor_chat_body").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Welcome to the village!")
                .check("interactive preview shows the authored message", ctx ->
                        ctx.requireUI().ui.rootElement.selfAndAllChildren()
                                .filter(element -> element instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement)
                                .map(element -> ((com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement) element).getText().getString())
                                .anyMatch(text -> text.contains("Welcome to the village!")))
                .screenshot("editor_chat_preview")
                .click("#phone_editor_tab_messages")
                .click("#phone_editor_addOfficialMessage")
                .focus("#phone_editor_message_title").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Arrival")
                .focus("#phone_editor_message_body").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("Find the village guide.")
                .click("#phone_editor_save")
                .waitUntil("server acknowledges save", ctx -> !((PhoneEditorUI) ctx.requireUI().ui.rootElement).isDirty())
                .checkServer("save applies notes, chat and messages to target player", ctx -> valid(SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)))
                .checkServer("editor's own phone is untouched", ctx ->
                        SmartPhone.getPhoneSavedData().getPhoneInfo(ctx.player()).findExtensionData(PresetChatsData.class)
                                .map(data -> data.getRooms().isEmpty()).orElse(true))
                .checkServer("new content survives SavedData serialization", ctx -> {
                    var provider = ctx.server().registryAccess();
                    var nbt = SmartPhone.getPhoneSavedData().save(new CompoundTag(), provider);
                    return valid(PhoneSavedData.fromNbt(nbt, provider).getPhoneInfo(OWNER));
                })
                .server("ordinary phone saves cannot replace authored conversations", ctx -> {
                    PhoneInfo replacement = new PhoneInfo();
                    replacement.deserializeNBT(ctx.server().registryAccess(), SmartPhone.getPhoneSavedData()
                            .getPhoneInfo(OWNER).serializeNBT(ctx.server().registryAccess()));
                    replacement.getOrCreateExtensionData(PresetChatsData.class).getRooms().clear();
                    var phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
                    phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
                    var oldItem = ctx.player().getMainHandItem();
                    ctx.player().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, phone);
                    try { SmartPhoneServerUtil.setPhoneInfoByPlayer(ctx.player(), OWNER, replacement); }
                    finally { ctx.player().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, oldItem); }
                })
                .checkServer("preset conversation is retained", ctx ->
                        SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).getOrCreateExtensionData(PresetChatsData.class).getRooms().size() == 1)
                .step("attempt editor save for an unknown player", ctx -> RPCPacketDistributor.rpcToServer(
                        C2SPayload.SAVE_PHONE_EDITOR, UNKNOWN, UUID.randomUUID(), new PhoneInfo(), false, ""))
                .serverTicks(3)
                .checkServer("unknown player was not created by save", ctx -> !SmartPhone.getPhoneSavedData().hasPhoneInfo(UNKNOWN))
                .step("close saved editor with mouse", ctx -> {
                    var bounds = ctx.el("#phone_editor_close").bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                })
                .server("reopen the persisted editor by UUID", ctx -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER))
                .awaitElement("#phone_editor")
                .step("change GUI scale while editor is open", ctx -> {
                    ctx.mc().options.guiScale().set(1);
                    ctx.mc().resizeDisplay();
                })
                .ticks(3)
                .check("editor controls stay within the screen after resizing", ctx -> {
                    var save = ctx.el("#phone_editor_save").bounds();
                    var lock = ctx.el("#phone_editor_lock").bounds();
                    return save.isCenterOnScreen(ctx.screen().width, ctx.screen().height)
                            && lock.isCenterOnScreen(ctx.screen().width, ctx.screen().height)
                            && save.centerX() > lock.centerX();
                })
                .screenshot("editor_resized")
                .check("reopened editor loads persisted content", ctx -> valid(((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().phoneInfo))
                .click("#phone_editor_tab_notes")
                .focus("#phone_editor_notes").keyDown(SELECT_MODIFIER).key(GLFW.GLFW_KEY_A).keyUp(SELECT_MODIFIER)
                .key(Keys.BACKSPACE).type("This draft must not be saved.")
                .step("close without saving", ctx -> ctx.mc().setScreen(null))
                .checkServer("discarded draft leaves SavedData intact", ctx -> valid(SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)))
                .setHeldItem(boundPhone)
                .waitUntil("target phone opens normally", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.isSynced() && OWNER.equals(screen.getPhoneUI().getOwnerUuid()))
                .server("apply an editor save while target phone is open", ctx -> {
                    PhoneInfo changed = new PhoneInfo();
                    changed.deserializeNBT(ctx.server().registryAccess(), SmartPhone.getPhoneSavedData()
                            .getPhoneInfo(OWNER).serializeNBT(ctx.server().registryAccess()));
                    changed.getOrCreateExtensionData(NotepadData.class).setText(new String[]{"Applied immediately."});
                    PhoneEditorServer.save(ctx.player(), OWNER, UUID.randomUUID(), changed, false, "");
                })
                .waitUntil("open phone receives the new data", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.getPhoneUI().phoneInfo.getOrCreateExtensionData(NotepadData.class)
                        .getText()[0].equals("Applied immediately."))
                .check("saving updates a phone that is already open", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.getPhoneUI().phoneInfo.getOrCreateExtensionData(NotepadData.class)
                        .getText()[0].equals("Applied immediately."))
                .setHeldItem(ItemStack.EMPTY)
                .teardown("close editor", ctx -> ctx.mc().setScreen(null))
                .teardownServer("clear test phone", ctx -> ctx.player().setItemInHand(
                        net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }

    private static boolean valid(PhoneInfo info) {
        var chats = info.getOrCreateExtensionData(PresetChatsData.class).getRooms();
        var messages = info.getOrCreateExtensionData(OfficialMessagesData.class).getMessages();
        return String.join("\n", info.getOrCreateExtensionData(NotepadData.class).getText()).equals("Meet at the village.")
                && chats.size() == 1 && chats.getFirst().getDisplayNameKey().equals("Village Guide")
                && chats.getFirst().getMessages().size() == 1
                && chats.getFirst().getMessages().getFirst().getBody().equals("Welcome to the village!")
                && messages.size() == 1 && messages.getFirst().getTitle().equals("Arrival")
                && messages.getFirst().getBody().equals("Find the village guide.");
    }
}
