package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.security.PhoneSecurityServer;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.util.PhoneEditorServer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@LDLRegisterClient(name = "phone_editor_passcode", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneEditorPasscodeScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("6eb5a21a-a46c-4c16-9563-c40f7cfa93c0");
    private static final String FIELD = "#phone_editor_passcode";
    private static final int SELECT = Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL;

    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "editor", "security");
    }

    @Override public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY).server("prepare known offline phone owner", ctx -> {
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, new PhoneInfo());
            SmartPhone.getPhoneSavedData().setPasscode(OWNER, null);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "PasscodeEditor"));
        });
        open(s);
        s.check("unprotected phone has an empty numeric password field", ctx ->
                value(ctx).isEmpty() && !ctx.el("#phone_editor_clearPasscode").element().isDisplayed());
        type(s, "12a34");
        s.check("letters cannot be entered", ctx -> value(ctx).equals("1234"))
                .click("#phone_editor_save")
                .waitForText("#phone_editor_status", "密码必须为6位数字。")
                .checkServer("partial password cannot be saved", ctx -> !PhoneSecurityServer.enabled(OWNER));
        type(s, "1234567");
        s.check("password field stops at six digits", ctx -> value(ctx).equals("123456"))
                .step("paste non-digit password", ctx -> ((TextField) ctx.el(FIELD).element()).insertText("a"))
                .check("non-digit paste is rejected", ctx -> value(ctx).equals("123456"));
        type(s, "001234");
        s.screenshot("editor_passcode_value");
        save(s);
        s.checkServer("six digits retain leading zeroes and persist as a digest", ctx -> {
            var data = SmartPhone.getPhoneSavedData();
            var provider = ctx.server().registryAccess();
            var reloaded = PhoneSavedData.fromNbt(data.save(new CompoundTag(), provider), provider).getPasscode(OWNER);
            return reloaded != null && reloaded.matches("001234") && !reloaded.matches("123400")
                    && !data.getPhoneInfo(OWNER).serializeNBT(provider).contains("_passcode");
        });
        close(s); open(s);
        s.check("existing password is not prefilled and can be cleared explicitly", ctx ->
                value(ctx).isEmpty() && ctx.el("#phone_editor_clearPasscode").element().isDisplayed())
                .check("password field and clear button fit on one row", ctx -> {
                    var field = ctx.el(FIELD).bounds();
                    var clear = ctx.el("#phone_editor_clearPasscode").bounds();
                    return field.right() < clear.x() && Math.abs(field.centerY() - clear.centerY()) < 2;
                }).screenshot("existing_passcode_controls");
        s.click("#phone_editor_tab_notes").focus("#phone_editor_notes").type("Preserve password");
        save(s);
        s.checkServer("saving other fields preserves the existing password", ctx ->
                SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("001234"));
        s.click("#phone_editor_tab_basics");
        type(s, "654321"); save(s);
        s.checkServer("editor replaces password without old-password confirmation", ctx ->
                SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("654321")
                        && !SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("001234"));
        close(s); open(s);
        s.click("#phone_editor_clearPasscode"); save(s);
        s.checkServer("clearing the existing password disables protection", ctx -> !PhoneSecurityServer.enabled(OWNER));
        type(s, "123456"); close(s);
        s.checkServer("discarding a draft does not change password", ctx -> !PhoneSecurityServer.enabled(OWNER));
        open(s);
        for (String invalid : new String[]{"12345", "1234567", "abcdef"}) {
            s.step("submit invalid password directly to server: " + invalid, ctx -> {
                var draft = new PhoneInfo();
                draft.setPhoneWallpaper(SmartPhone.id("textures/ui/banner.png"));
                RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_EDITOR, OWNER, UUID.randomUUID(), draft, true, invalid);
            }).serverTicks(2).checkServer("server rejects whole invalid save: " + invalid, ctx ->
                    !PhoneSecurityServer.enabled(OWNER) && !SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                            .getPhoneWallpaper().equals(SmartPhone.id("textures/ui/banner.png")));
        }
        close(s);
        ItemStack phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(phone).waitUntil("bound phone opens without password", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.isSynced() && screen.isRaised() && !screen.getPhoneUI().isAccessLocked())
                .server("admin enables password on the currently open phone", ctx -> PhoneEditorServer.save(ctx.player(), OWNER,
                        UUID.randomUUID(), SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER), true, "006789"))
                .waitUntil("open phone is immediately locked", ctx -> ((HeldPhoneScreen)ctx.screen()).getPhoneUI().isAccessLocked())
                .checkServer("old session has no authorization", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .step("swipe standby", ctx -> {
                    var bounds = ctx.el("#held_phone_lock").bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.y() + bounds.height() * .85f, 0);
                    ctx.input().dragTo(bounds.centerX(), bounds.y() + bounds.height() * .15f, 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.y() + bounds.height() * .15f, 0);
                }).awaitElement("#phone_passcode");
        for (char digit : "006789".toCharArray()) {
            s.step("enter configured digit", ctx -> {
                var bounds = ctx.el("#phone_pin_" + digit).bounds();
                ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
            });
        }
        s.waitUntil("editor-configured password unlocks the real phone", ctx -> !((HeldPhoneScreen)ctx.screen()).getPhoneUI().isAccessLocked())
                .checkServer("new password authorizes the holder", ctx -> PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .server("admin disables password on the open phone", ctx -> PhoneEditorServer.save(ctx.player(), OWNER,
                        UUID.randomUUID(), SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER), true, ""))
                .waitUntil("open phone receives disabled status", ctx -> !((HeldPhoneScreen)ctx.screen()).getPhoneUI().isPasscodeEnabled())
                .checkServer("phone remains usable without a password", ctx -> PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .teardown("close editor or phone", ctx -> ctx.mc().setScreen(null))
                .teardownServer("clear test phone and password", ctx -> {
                    SmartPhone.getPhoneSavedData().setPasscode(OWNER, null);
                    ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                });
    }

    private static String value(TestContext ctx) { return ((TextField)ctx.el(FIELD).element()).getText(); }
    private static void open(ScenarioBuilder s) {
        s.server("open target phone editor", ctx -> ctx.server().getCommands().performPrefixedCommand(
                ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER)).awaitElement(FIELD);
    }
    private static void type(ScenarioBuilder s, String text) {
        s.focus(FIELD).keyDown(SELECT).key(GLFW.GLFW_KEY_A).keyUp(SELECT).key(GLFW.GLFW_KEY_BACKSPACE);
        if (!text.isEmpty()) s.type(text);
    }
    private static void save(ScenarioBuilder s) {
        s.click("#phone_editor_save").waitForText("#phone_editor_status", "已保存，该玩家的手机已更新。");
    }
    private static void close(ScenarioBuilder s) { s.step("close editor", ctx -> ctx.mc().setScreen(null)); }
}
