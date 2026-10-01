package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.app.ui.ChatRoomUI;
import com.smart.phone.ui.app.ui.OfficialMessagesUI;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Covers editing a blocked phone and interrupted standby gestures through real input. */
@LDLRegisterClient(name = "blocked_phone_editor", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class BlockedPhoneEditorScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("7247e1d6-062e-426e-9c79-7bcda8b65de5");

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "editor", "regression");
    }

    @Override public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY).server("open administrator editor for blocked phone", ctx -> {
            var info = new PhoneInfo();
            info.setBlocked(true);
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "BlockedEditor"));
            ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2),
                    "smart_phone editor " + OWNER);
        }).awaitElement("#phone_editor").ticks(2)
                .click("#phone_editor_tab_chats")
                .check("blocked phone still allows administrator chat preview", ctx ->
                        editor(ctx).getPreview().homeScreen.appUI instanceof ChatRoomUI)
                .click("#phone_editor_addChat").click("#phone_editor_addChatMessage")
                .click("#phone_editor_addPresetFriend").click("#phone_editor_addChatMessage")
                .click("#phone_editor_tab_messages").click("#phone_editor_addOfficialMessage")
                .check("blocked phone still allows administrator message preview", ctx ->
                        editor(ctx).getPreview().homeScreen.appUI instanceof OfficialMessagesUI);
        for (int i = 0; i < 5; i++) {
            s.click("#phone_editor_tab_chats").click("#phone_editor_tab_messages")
                    .click("#phone_editor_tab_notes").click("#phone_editor_tab_apps")
                    .click("#phone_editor_tab_basics").click("#phone_editor_resetPreview");
            swipe(s, "#phone_editor_lock", .1f);
            s.check("blocked standby stays locked after reset " + i, ctx ->
                    !editor(ctx).getPreview().lockScreen.isUnlocked())
                    .click("#phone_editor_tab_chats").ticks(2)
                    .check("chat remains visible after repeated tab changes " + i, ctx -> {
                        var preview = editor(ctx).getPreview();
                        return preview.homeScreen.appUI instanceof ChatRoomUI && preview.homeScreen.isVisible()
                                && preview.homeScreen.getStyle().opacity() == 1;
                    });
        }
        s.screenshot("blocked_phone_chat_preview").click("#phone_editor_save")
                .waitForText("#phone_editor_status", "已保存，该玩家的手机已更新。")
                .checkServer("editing never disables the real phone block", ctx ->
                        SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).isBlocked())
                .step("close administrator editor", ctx -> ctx.mc().setScreen(null))
                .server("allow ordinary phone swipe testing", ctx ->
                        SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).setBlocked(false));
        var item = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        item.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        for (int i = 0; i < 6; i++) {
            s.setHeldItem(item).waitUntil("held phone ready", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isRaised() && h.isSynced());
            // Start snap-back, then immediately swipe again while its animation is active.
            swipe(s, "#held_phone_lock", .65f);
            swipe(s, "#held_phone_lock", .05f);
            s.waitUntil("repeated swipe reaches desktop", ctx -> ((HeldPhoneScreen)ctx.screen()).getPhoneUI()
                            .lockScreen.getStyle().opacity() == 0)
                    .click("#phone_app_smart_phone_chat_room")
                    .check("phone remains interactive after interrupted swipe " + i, ctx ->
                            ((HeldPhoneScreen)ctx.screen()).getPhoneUI().homeScreen.appUI instanceof ChatRoomUI)
                    .setHeldItem(ItemStack.EMPTY).waitUntil("phone put away", ctx -> ctx.screen() == null);
        }
        s.teardown("close screen", ctx -> ctx.mc().setScreen(null))
                .teardownServer("remove fixture phone", ctx -> {
                    ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, new PhoneInfo());
                });
    }

    private static PhoneEditorUI editor(TestContext ctx) { return (PhoneEditorUI)ctx.requireUI().ui.rootElement; }

    private static void swipe(ScenarioBuilder s, String id, float end) {
        s.step("swipe " + id, ctx -> {
            var b = ctx.el(id).bounds();
            ctx.input().mouseDown(b.centerX(), b.y() + b.height() * .85f, 0);
            ctx.input().dragTo(b.centerX(), b.y() + b.height() * end, 0);
            ctx.input().mouseUp(b.centerX(), b.y() + b.height() * end, 0);
        });
    }
}
