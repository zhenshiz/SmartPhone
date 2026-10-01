package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.client.camera.PhoneCameraClient;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.security.PhonePasscode;
import com.smart.phone.security.PhoneSecurityServer;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.util.PhoneEditorServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

@LDLRegisterClient(name = "phone_config", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneConfigScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("ec3003e8-0744-4b27-b6c9-b8f3f26a00ce");
    private static final UUID UNKNOWN = UUID.fromString("ec3003e8-0744-4b27-b6c9-b8f3f26a00cf");
    private static final String NAME = "ConfigOwner";

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "config", "security");
    }

    @Override public void define(ScenarioBuilder s) {
        var item = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        item.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(ItemStack.EMPTY).server("create known offline phone", ctx -> {
            var info = new PhoneInfo();
            info.getOrCreateExtensionData(NotepadData.class).setText(new String[]{"Private note"});
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            SmartPhone.getPhoneSavedData().setPasscode(OWNER, null);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, NAME));
        }).checkServer("new and old records default to unlocked with visible indicators", ctx -> {
            var legacy = new PhoneInfo();
            legacy.deserializeNBT(ctx.server().registryAccess(), new CompoundTag());
            return !legacy.isBlocked() && !legacy.isHideDate() && !legacy.isHideStatusIcons() && !legacy.isHideOwnerName() && !legacy.isHideLockIcon();
        }).setHeldItem(item).waitUntil("bound phone ready", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced() && h.isRaised())
                .ticks(2).check("normal time and icons are shown", ctx -> shown(ctx, "phone_standby_time")
                        && shown(ctx, "phone_header_time") && shown(ctx, "phone_status_icons") && shown(ctx, "phone_unlock_hint")
                        && shown(ctx, "phone_owner_name") && shown(ctx, "phone_lock_icon"));
        swipe(s);
        s.waitUntil("phone unlocked", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 0)
                .click("#phone_app_smart_phone_camera")
                .waitUntil("camera is active", ctx -> PhoneCameraClient.isCameraActive() && ctx.screen() == null);
        command(s, NAME, "locked true");
        s.waitUntil("administrator lock exits camera", ctx -> ctx.screen() instanceof HeldPhoneScreen && ui(ctx).phoneInfo.isBlocked())
                .ticks(2).check("blocked standby shows closed lock without swipe hint", ctx -> ui(ctx).isAccessLocked()
                        && !PhoneCameraClient.isCameraActive() && ui(ctx).homeScreen.appUI == null
                        && !shown(ctx, "phone_unlock_hint")
                        && ((SpriteTexture)ctx.el("#phone_lock_icon").element().getStyle().backgroundTexture())
                        .getImageLocation().equals(SmartPhone.id("textures/ui/lock.png")))
                .checkServer("server revokes current session", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER)
                        && !PhoneSecurityServer.canUsePhone(ctx.player()));
        swipe(s);
        s.ticks(2).check("blocked phone cannot swipe or show password keypad", ctx -> !ui(ctx).lockScreen.isUnlocked()
                        && ui(ctx).lockScreen.getCurrentOffsetY() == 0 && !ctx.exists("#phone_passcode"))
                .check("blocked snapshot excludes private data", ctx -> ui(ctx).phoneInfo.getExtensionData().isEmpty());
        command(s, OWNER.toString(), "hide_date true");
        s.waitUntil("hide date reaches UI", ctx -> !shown(ctx, "phone_standby_time") && !shown(ctx, "phone_header_time"))
                .check("date toggle leaves icons visible", ctx -> shown(ctx, "phone_status_icons"));
        command(s, NAME, "hide_status_icons true");
        s.waitUntil("status icons hidden", ctx -> !shown(ctx, "phone_status_icons"))
                .screenshot("blocked_hidden_indicators");
        command(s, NAME, "hide_owner_name true");
        s.waitUntil("owner label hidden independently", ctx -> !shown(ctx, "phone_owner_name"))
                .check("hiding owner keeps lock visible", ctx -> shown(ctx, "phone_lock_icon"));
        command(s, NAME, "hide_lock_icon true");
        s.waitUntil("lock icon hidden", ctx -> !shown(ctx, "phone_lock_icon"))
                .check("hidden decorations do not unlock phone", ctx -> ui(ctx).phoneInfo.isBlocked() && ui(ctx).isAccessLocked())
                .screenshot("wallpaper_only_blocked_phone");
        command(s, NAME, "wallpaper smart_phone:textures/ui/banner.png");
        s.waitUntil("wallpaper updates while blocked", ctx -> ui(ctx).phoneInfo.getPhoneWallpaper().equals(SmartPhone.id("textures/ui/banner.png")))
                .checkServer("all settings survive SavedData reload", ctx -> {
                    var data = SmartPhone.getPhoneSavedData();
                    var provider = ctx.server().registryAccess();
                    var loaded = PhoneSavedData.fromNbt(data.save(new CompoundTag(), provider), provider).getPhoneInfo(OWNER);
                    return loaded.isBlocked() && loaded.isHideDate() && loaded.isHideStatusIcons()
                            && loaded.isHideOwnerName() && loaded.isHideLockIcon()
                            && loaded.getPhoneWallpaper().equals(SmartPhone.id("textures/ui/banner.png"));
                }).step("attempt to remove administrator flags using ordinary save", ctx ->
                        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, OWNER, new PhoneInfo()))
                .serverTicks(2).checkServer("blocked ordinary save is rejected", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).isBlocked())
                .server("enable a password without removing block", ctx -> {
                    SmartPhone.getPhoneSavedData().setPasscode(OWNER, PhonePasscode.create("123456"));
                    PhoneEditorServer.syncChanges(ctx.server(), OWNER, true);
                }).ticks(2).step("attempt known correct password behind administrator lock", ctx ->
                        RPCPacketDistributor.rpcToServer(C2SPayload.UNLOCK_PHONE, ui(ctx).getAccessToken(), "123456"))
                .serverTicks(2).checkServer("correct password cannot bypass administrator lock", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER));
        command(s, NAME, "locked false");
        s.waitUntil("block lifted but password still required", ctx -> !ui(ctx).phoneInfo.isBlocked() && ui(ctx).isAccessLocked());
        swipe(s);
        s.awaitElement("#phone_passcode");
        for (char digit : "123456".toCharArray()) s.step("enter password digit", ctx -> click(ctx, "#phone_pin_" + digit));
        s.waitUntil("correct password unlocks after block lifted", ctx -> !ui(ctx).isAccessLocked())
                .waitUntil("unlock animation complete", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 0)
                .checkServer("authorization restored", ctx -> PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .step("attempt to overwrite admin display flags with ordinary save", ctx -> {
                    var draft = new PhoneInfo();
                    RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, OWNER, draft);
                }).serverTicks(2).checkServer("ordinary saves preserve administrator display flags", ctx -> {
                    var info = SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER);
                    return info.isHideDate() && info.isHideStatusIcons() && info.isHideOwnerName() && info.isHideLockIcon();
                });
        command(s, NAME, "hide_date false");
        command(s, NAME, "hide_status_icons false");
        s.waitUntil("indicators become visible again", ctx -> shown(ctx, "phone_header_time") && shown(ctx, "phone_status_icons"));
        command(s, NAME, "hide_owner_name false");
        command(s, NAME, "hide_lock_icon false");
        s.waitUntil("owner and lock become visible again", ctx -> shown(ctx, "phone_owner_name") && shown(ctx, "phone_lock_icon"));
        // Also cover a lock arriving while the swipe animation is still running.
        s.step("return phone to standby", ctx -> ui(ctx).lockScreen.resetLocked());
        swipe(s);
        command(s, NAME, "locked true");
        s.waitUntil("locked during animation", ctx -> ui(ctx).phoneInfo.isBlocked()).waitMs(650)
                .check("old animation cannot hide blocked standby", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 1
                        && !ui(ctx).homeScreen.isVisible() && !ui(ctx).lockScreen.isUnlocked())
                .screenshot("blocked_visible_indicators");
        s.checkServer("non-admin cannot execute config command", ctx -> {
            var source = ctx.player().createCommandSourceStack().withPermission(0);
            try {
                ctx.server().getCommands().getDispatcher().execute("smart_phone config " + NAME + " locked false", source);
                return false;
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
                return SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).isBlocked();
            }
        });
        command(s, UNKNOWN.toString(), "locked true");
        s.checkServer("unknown player is not created by config command", ctx -> !SmartPhone.getPhoneSavedData().hasPhoneInfo(UNKNOWN))
                .setHeldItem(ItemStack.EMPTY).waitUntil("held phone closes", ctx -> ctx.screen() == null)
                .server("open editor", ctx -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + NAME))
                .awaitElement("#phone_editor").ticks(2)
                .check("editor includes all five switches", ctx -> booleanField(ctx, "blocked") != null
                        && booleanField(ctx, "hideDate") != null && booleanField(ctx, "hideStatusIcons") != null
                        && booleanField(ctx, "hideOwnerName") != null && booleanField(ctx, "hideLockIcon") != null)
                .screenshot("editor_phone_config")
                .step("turn off block in editor", ctx -> {
                    var toggle = booleanField(ctx, "blocked").toggle.toggleButton;
                    var bounds = ctx.query().where(e -> e == toggle).one().bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                }).check("editor switch updates preview draft", ctx -> !((PhoneEditorUI)ctx.requireUI().ui.rootElement).getPreview().phoneInfo.isBlocked())
                .click("#phone_editor_save").waitForText("#phone_editor_status", "已保存，该玩家的手机已更新。")
                .checkServer("editor saves the switch and preserves password", ctx -> !SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER).isBlocked()
                        && SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("123456"))
                .teardown("close phone", ctx -> ctx.mc().setScreen(null))
                .teardownServer("clear fixture flags and password", ctx -> {
                    SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, new PhoneInfo());
                    SmartPhone.getPhoneSavedData().setPasscode(OWNER, null);
                    ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                });
    }

    private static BooleanConfigurator booleanField(TestContext ctx, String field) {
        String label = Component.translatable("smartPhone.data.phoneInfo." + field).getString();
        return (BooleanConfigurator)ctx.query().type(BooleanConfigurator.class)
                .where(e -> ((BooleanConfigurator)e).label.getText().getString().equals(label)).one().element();
    }
    private static PhoneUI ui(TestContext ctx) { return ((HeldPhoneScreen)ctx.screen()).getPhoneUI(); }
    private static boolean shown(TestContext ctx, String id) { return ctx.el("#" + id).element().isVisible(); }
    private static void command(ScenarioBuilder s, String target, String options) {
        s.server("config " + target + " " + options, ctx -> ctx.server().getCommands().performPrefixedCommand(
                ctx.server().createCommandSourceStack(), "smart_phone config " + target + " " + options));
    }
    private static void click(TestContext ctx, String id) {
        var bounds = ctx.el(id).bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }
    private static void swipe(ScenarioBuilder s) {
        s.step("swipe standby", ctx -> {
            var b = ctx.el("#held_phone_lock").bounds();
            ctx.input().mouseDown(b.centerX(), b.y() + b.height() * .85f, 0);
            ctx.input().dragTo(b.centerX(), b.y() + b.height() * .1f, 0);
            ctx.input().mouseUp(b.centerX(), b.y() + b.height() * .1f, 0);
        });
    }
}
