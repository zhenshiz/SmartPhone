package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.authlib.GameProfile;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.app.OfficialMessages;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@LDLRegisterClient(name = "official_message_editor", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class OfficialMessageEditorScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("e8ab0fc9-c698-4156-a136-c094486aa3e4");
    private static final UUID FIRST = UUID.fromString("534ff2d2-9c5f-41b4-9dcf-bd63d33b1a60");
    private static final UUID SECOND = UUID.fromString("acb72fbc-22fd-4d7f-af5d-6e7c5d1e7b2d");

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("editor", "messages", "persistence");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("use largest native editor size", ctx -> {
            ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
            ClientConfig.EDITOR_GUI_SCALE.set(0);
        }).setHeldItem(ItemStack.EMPTY).server("create two distinct unread messages", ctx -> {
            PhoneInfo info = new PhoneInfo();
            OfficialMessage first = new OfficialMessage("Guide", "First notice", "First body");
            first.setMessageId(FIRST);
            first.setCreatedAtMillis(2000);
            OfficialMessage second = new OfficialMessage("Mayor", "Second notice", "Second body");
            second.setMessageId(SECOND);
            second.setCreatedAtMillis(1000);
            info.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(first);
            info.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(second);
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "MessageOwner"));
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER);
        }).awaitElement("#phone_editor").click("#phone_editor_tab_messages")
                .check("no fields before selecting a message", ctx -> !ctx.exists("#phone_editor_message_title")
                        && !ctx.el("#phone_editor_removeOfficialMessage").element().isActive())
                .click("#official_message_" + FIRST)
                .check("selection shows only one message without marking read", ctx -> ctx.count("#phone_editor_message_body") == 1
                        && title(ctx).equals("First notice") && !data(ctx).findMessage(FIRST).orElseThrow().isRead()
                        && !editor(ctx).isDirty());
        replace(s, "message_title", "Edited first");
        replace(s, "message_body", "Edited first body");
        s.check("editing affects only the selected draft", ctx -> data(ctx).findMessage(FIRST).orElseThrow().getBody().equals("Edited first body")
                        && data(ctx).findMessage(SECOND).orElseThrow().getBody().equals("Second body"))
                .checkServer("changes are not saved yet", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(OfficialMessagesData.class).findMessage(FIRST).orElseThrow().getTitle().equals("First notice"))
                .click("#official_message_" + SECOND)
                .check("switching selection replaces fields and highlight", ctx -> title(ctx).equals("Second notice")
                        && ctx.count("#phone_editor_message_title") == 1 && ctx.count(".phone_selected_official_message") == 1
                        && ctx.el("#official_message_" + SECOND).element().hasClass("phone_selected_official_message"))
                .click("#phone_editor_tab_notes")
                .click("#official_message_" + FIRST)
                .check("preview selection returns to message view with earlier edits", ctx ->
                        title(ctx).equals("Edited first") && ctx.el("#phone_editor_message_title").element().isVisible())
                .check("add and delete stay on screen at maximum scale", ctx ->
                        ctx.el("#phone_editor_addOfficialMessage").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height)
                                && ctx.el("#phone_editor_removeOfficialMessage").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height))
                .screenshot("selected_official_message")
                .click("#phone_editor_removeOfficialMessage")
                .check("delete affects only selected message and clears fields", ctx -> data(ctx).getMessages().size() == 1
                        && data(ctx).findMessage(SECOND).isPresent() && !ctx.exists("#official_message_" + FIRST)
                        && !ctx.exists("#phone_editor_message_title") && !ctx.el("#phone_editor_removeOfficialMessage").element().isActive())
                .click("#phone_editor_addOfficialMessage")
                .check("plus selects new message without exposing all messages", ctx -> data(ctx).getMessages().size() == 2
                        && ctx.count("#phone_editor_message_title") == 1 && ctx.count(".phone_selected_official_message") == 1);
        replace(s, "message_title", "New announcement");
        s.click("#phone_editor_save").waitUntil("save acknowledged", ctx -> !editor(ctx).isDirty())
                .checkServer("save retains other message and selected new content", ctx -> valid(SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)))
                .checkServer("selection edits survive serialization", ctx -> {
                    var provider = ctx.server().registryAccess();
                    return valid(PhoneSavedData.fromNbt(SmartPhone.getPhoneSavedData().save(new CompoundTag(), provider), provider).getPhoneInfo(OWNER));
                }).step("close editor", ctx -> ctx.mc().setScreen(null));
        ItemStack bound = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        bound.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(bound).waitUntil("held phone synced", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isRaised() && h.isSynced())
                .step("open normal messages app", ctx -> {
                    var phone = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    phone.screenContainer.removeChild(phone.lockScreen);
                    phone.homeScreen.openApp(new OfficialMessages());
                }).step("read message in normal phone", ctx -> {
                    var bounds = ctx.el("#official_message_" + SECOND).bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                }).serverTicks(3)
                .check("normal phone still opens detail", ctx -> ctx.exists("#official_message_delete"))
                .checkServer("normal reading still marks message read", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(OfficialMessagesData.class).findMessage(SECOND).orElseThrow().isRead())
                .teardown("restore preferences and close screen", ctx -> {
                    ctx.mc().setScreen(null);
                    if (ctx.state().containsKey("editorScale")) ClientConfig.EDITOR_GUI_SCALE.set(ctx.get("editorScale"));
                }).teardownServer("clear fixture phone", ctx -> ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }

    private static boolean valid(PhoneInfo info) {
        var messages = info.getOrCreateExtensionData(OfficialMessagesData.class);
        return messages.getMessages().size() == 2 && messages.findMessage(FIRST).isEmpty()
                && messages.findMessage(SECOND).orElseThrow().getTitle().equals("Second notice")
                && !messages.findMessage(SECOND).orElseThrow().isRead()
                && messages.getMessages().stream().anyMatch(m -> m.getTitle().equals("New announcement"));
    }

    private static PhoneEditorUI editor(TestContext ctx) { return (PhoneEditorUI)ctx.requireUI().ui.rootElement; }
    private static OfficialMessagesData data(TestContext ctx) { return editor(ctx).getPreview().phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class); }
    private static String title(TestContext ctx) { return ((TextField)ctx.el("#phone_editor_message_title").element()).getText(); }
    private static void replace(ScenarioBuilder s, String field, String text) {
        int modifier = Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL;
        s.focus("#phone_editor_" + field).keyDown(modifier).key(GLFW.GLFW_KEY_A).keyUp(modifier).key(Keys.BACKSPACE).type(text);
    }
}
