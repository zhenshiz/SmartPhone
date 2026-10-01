package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.network.c2s.ChatRoomImagePayload;
import com.smart.phone.security.PhoneSecurityServer;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import com.smart.phone.ui.data.PresetChatsData;
import com.smart.phone.ui.data.chat.ChatRoom;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** 通过真实键盘 UI、C2S 校验和 SavedData 验证借来的手机也受密码保护。 */
@LDLRegisterClient(name = "phone_passcode", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhonePasscodeScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("9c530100-0ada-444d-a4ca-719c351d21d1");
    private static final String ROOM = "preset:private";
    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("phone", "passcode", "security");
    }

    @Override public void define(ScenarioBuilder s) {
        ItemStack phone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        phone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        s.setHeldItem(ItemStack.EMPTY).server("create a borrowed phone with private records", ctx -> {
            var info = new PhoneInfo();
            var room = new ChatRoom(ROOM, "Private records");
            room.addMessage(new ChatRoomMessage(ROOM, OWNER, "Owner", "Private saved content"));
            info.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
            SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
            ctx.server().getProfileCache().add(new GameProfile(OWNER, "PinOwner"));
        });
        open(s, phone);
        swipe(s);
        s.waitUntil("unprotected phone unlocks by swiping", ctx -> ui(ctx).lockScreen.isUnlocked())
                .waitUntil("desktop unlock animation finished", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 0)
                .click("#phone_app_smart_phone_setting").step("click #phone_passcode_setting", ctx -> click(ctx, "#phone_passcode_setting"))
                .check("keypad contains only ten numbers and cancel", ctx -> ctx.count("#phone_passcode") == 1
                        && ctx.count("#phone_passcode_cancel") == 1
                        && java.util.stream.IntStream.range(0,10).allMatch(i -> ctx.exists("#phone_pin_" + i)))
                .screenshot("set_six_digit_passcode");
        pin(s, "123456"); pin(s, "123450");
        s.waitForText("#phone_passcode_message", "两次密码不同，请重新设置");
        pin(s, "123456"); pin(s, "123456");
        s.waitUntil("passcode enabled", ctx -> ui(ctx).isPasscodeEnabled() && !ctx.exists("#phone_passcode"))
                .checkServer("credential persists separately as a salted digest", ctx -> {
                    var data = SmartPhone.getPhoneSavedData();
                    var provider = ctx.server().registryAccess();
                    var tag = data.save(new CompoundTag(), provider);
                    var reloaded = PhoneSavedData.fromNbt(tag, provider).getPasscode(OWNER);
                    return reloaded != null && reloaded.matches("123456") && !reloaded.matches("654321")
                            && !data.getPhoneInfo(OWNER).serializeNBT(provider).contains("_passcode")
                            && !tag.getCompound(OWNER.toString()).getCompound("_passcode").getAsString().contains("123456");
                });
        s.step("remember previous unlock session", ctx -> ctx.put("oldToken", ui(ctx).getAccessToken()));
        close(s); open(s, phone);
        s.step("try the previous session token", ctx -> RPCPacketDistributor.rpcToServer(C2SPayload.UNLOCK_PHONE,
                        (UUID)ctx.get("oldToken"), "123456"))
                .serverTicks(2)
                .checkServer("stale token cannot unlock a new session", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .check("borrowed phone opens locked without private records", ctx -> ui(ctx).isAccessLocked()
                        && ui(ctx).phoneInfo.findExtensionData(PresetChatsData.class).map(p -> p.getRooms().isEmpty()).orElse(true))
                .checkServer("holder has no server authorization", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .step("attempt direct writes while locked", ctx -> {
                    var draft = new PhoneInfo();
                    draft.setPhoneWallpaper(SmartPhone.id("textures/ui/banner.png"));
                    RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO, OWNER, draft);
                    RPCPacketDistributor.rpcToServer(C2SPayload.SEND_PRESET_CHAT, OWNER, "Bypass", new ChatRoomImagePayload(ROOM, null));
                }).serverTicks(2)
                .checkServer("server rejects writes behind the lock page", ctx -> {
                    var info = SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER);
                    return !info.getPhoneWallpaper().equals(SmartPhone.id("textures/ui/banner.png"))
                            && info.getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 1;
                });
        swipe(s);
        s.click("#phone_pin_1").click("#phone_pin_2").screenshot("unlock_partial_pin")
                .step("click #phone_passcode_cancel", ctx -> click(ctx, "#phone_passcode_cancel"))
                .check("cancel returns to standby without unlocking", ctx -> ui(ctx).isAccessLocked() && !ctx.exists("#phone_passcode"));
        swipe(s); pin(s, "000000");
        s.waitForText("#phone_passcode_message", "密码错误，请重试")
                .checkServer("wrong password does not authorize access", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER));
        for (int i = 0; i < 4; i++) {
            pin(s, "000000");
            s.waitForText("#phone_passcode_message", i == 3 ? "尝试过多，请等候30秒" : "密码错误，请重试");
        }
        pin(s, "123456");
        s.waitForText("#phone_passcode_message", "尝试过多，请等候30秒")
                .checkServer("correct code cannot bypass the cooldown", ctx -> !PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .timeoutMs(35_000).waitMs(30_100);
        pin(s, "123456");
        s.waitUntil("correct password unlocks", ctx -> !ui(ctx).isAccessLocked() && !ctx.exists("#phone_passcode"))
                .check("private data arrives only after successful verification", ctx -> ui(ctx).phoneInfo
                        .getOrCreateExtensionData(PresetChatsData.class).findRoom(ROOM).orElseThrow().getMessages().size() == 1)
                .checkServer("correct password authorizes borrowed phone", ctx -> PhoneSecurityServer.canAccess(ctx.player(), OWNER))
                .waitUntil("desktop unlock animation finished", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 0)
                .click("#phone_app_smart_phone_setting").step("click #phone_passcode_setting", ctx -> click(ctx, "#phone_passcode_setting"));
        pin(s, "123456"); pin(s, "654321"); pin(s, "654321");
        s.waitUntil("change password completes", ctx -> !ctx.exists("#phone_passcode"))
                .checkServer("new password replaces old password", ctx -> SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("654321")
                        && !SmartPhone.getPhoneSavedData().getPasscode(OWNER).matches("123456"));
        close(s); open(s, phone); swipe(s);
        s.key(org.lwjgl.glfw.GLFW.GLFW_KEY_9).key(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE);
        for (char digit : "654321".toCharArray()) s.key(org.lwjgl.glfw.GLFW.GLFW_KEY_0 + digit - '0');
        s.waitUntil("changed password unlocks", ctx -> !ui(ctx).isAccessLocked())
                .waitUntil("desktop unlock animation finished", ctx -> ui(ctx).lockScreen.getStyle().opacity() == 0)
                .click("#phone_app_smart_phone_setting").step("click #phone_passcode_disable", ctx -> click(ctx, "#phone_passcode_disable"));
        pin(s, "000000");
        s.waitForText("#phone_passcode_message", "密码错误，请重试")
                .checkServer("wrong current password cannot disable lock", ctx -> PhoneSecurityServer.enabled(OWNER));
        pin(s, "654321");
        s.waitUntil("passcode turned off", ctx -> !ui(ctx).isPasscodeEnabled() && !ctx.exists("#phone_passcode"));
        close(s); open(s, phone); swipe(s);
        s.waitUntil("disabled password returns to swipe-only unlock", ctx -> ui(ctx).lockScreen.isUnlocked())
                .checkServer("credential removed", ctx -> !PhoneSecurityServer.enabled(OWNER))
                .teardown("close phone", ctx -> ctx.mc().setScreen(null))
                .teardownServer("clear test passcode and held item", ctx -> {
                    SmartPhone.getPhoneSavedData().setPasscode(OWNER, null);
                    ctx.player().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                });
    }
    private static PhoneUI ui(TestContext ctx) { return ((HeldPhoneScreen) ctx.screen()).getPhoneUI(); }
    private static void click(TestContext ctx, String selector) {
        var bounds = ctx.el(selector).bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }
    private static void pin(ScenarioBuilder s, String pin) {
        for (char digit : pin.toCharArray()) s.step("enter passcode digit", ctx -> click(ctx, "#phone_pin_" + digit));
    }
    private static void open(ScenarioBuilder s, ItemStack item) {
        s.setHeldItem(item.copy()).waitUntil("phone synced and raised", ctx -> ctx.screen() instanceof HeldPhoneScreen phone
                && phone.isSynced() && phone.isRaised());
    }
    private static void close(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY).waitUntil("phone put away", ctx -> ctx.screen() == null);
    }
    private static void swipe(ScenarioBuilder s) {
        s.step("swipe up from standby", ctx -> {
            var bounds = ElementBounds.of(ui(ctx).lockScreen);
            float x = bounds.centerX(), from = bounds.y() + bounds.height() * .85f, to = bounds.y() + bounds.height() * .15f;
            ctx.input().mouseDown(x, from, 0);
            ctx.input().dragTo(x, to, 0);
            ctx.input().mouseUp(x, to, 0);
        });
    }
}
