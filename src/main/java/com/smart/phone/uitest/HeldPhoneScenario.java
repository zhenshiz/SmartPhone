package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.smart.phone.Config;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@LDLRegisterClient(
        name = "held_phone",
        group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY
)
public final class HeldPhoneScenario implements UIScenario {
    private static final String APP_STORE = "#phone_app_smart_phone_app_store";
    private static final String LOCK_SCREEN = "#held_phone_lock";

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("ui", "phone");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        float[] swipe = new float[3];
        scenario
                .step("require the held phone option", context ->
                        context.require("held phone mode is enabled", Config.HELD_PHONE_MODE.get()))
                .step("client selects phone before the server sees the hotbar switch", context ->
                        context.mc().player.setItemInHand(InteractionHand.MAIN_HAND,
                                SmartPhoneRegistries.PHONE.get().getDefaultInstance()))
                .awaitScreen(HeldPhoneScreen.class)
                .awaitModularUI()
                .awaitElement(LOCK_SCREEN)
                .serverTicks(2)
                .check("early server request leaves the screen waiting for sync", context ->
                        context.screen() instanceof HeldPhoneScreen screen && !screen.isSynced())
                .server("server receives the selected phone", context ->
                        context.player().setItemInHand(InteractionHand.MAIN_HAND,
                                SmartPhoneRegistries.PHONE.get().getDefaultInstance()))
                .waitUntil("phone raise animation completes", context ->
                        context.screen() instanceof HeldPhoneScreen screen && screen.isRaised())
                .waitUntil("saved phone data arrives", context ->
                        context.screen() instanceof HeldPhoneScreen screen && screen.isSynced())
                .check("held phone releases the mouse for direct interaction", context ->
                        !context.mc().mouseHandler.isMouseGrabbed())
                .check("phone rests on the right side of the screen", context -> {
                    var bounds = context.el(APP_STORE).bounds();
                    return bounds.centerX() > context.screen().width / 2f
                            && bounds.isCenterOnScreen(context.screen().width, context.screen().height);
                })
                .check("phone keeps a portrait aspect across window shapes", context -> {
                    for (var window : new com.lowdragmc.lowdraglib2.math.Size[]{
                            com.lowdragmc.lowdraglib2.math.Size.of(1280, 720),
                            com.lowdragmc.lowdraglib2.math.Size.of(1024, 768),
                            com.lowdragmc.lowdraglib2.math.Size.of(2560, 1080)}) {
                        var canvas = PhoneUI.getAutoGuiScaledSize(window);
                        float aspect = canvas.height * (1424f / 1536f) / (canvas.width * (808f / 2816f));
                        if (Math.abs(aspect - 1.88f) > .04f) return false;
                    }
                    return true;
                })
                .check("phone opens on its lock screen", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    PhoneUI phoneUI = (PhoneUI) screen.getModularUI().ui.rootElement;
                    return phoneUI.lockScreen.getParent() == phoneUI.screenContainer;
                })
                .screenshot("held_phone_locked")
                .step("locate lock screen swipe", context -> {
                    var bounds = context.el(LOCK_SCREEN).bounds();
                    swipe[0] = bounds.centerX();
                    swipe[1] = bounds.y() + bounds.height() * 0.78f;
                    swipe[2] = bounds.y() + bounds.height() * 0.12f;
                })
                .step("park the cached hover away from the phone", context ->
                        context.input().placeCursor(-10_000, -10_000))
                .step("press lock screen through Screen without the test driver's hover refresh", context ->
                        context.screen().mouseClicked(swipe[0], swipe[1], Keys.MOUSE_LEFT))
                .check("real mouse press locates the lock screen from its coordinates", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    return screen.getPhoneUI().lockScreen.isDragging();
                })
                .step("swipe up through Screen without the test driver's hover refresh", context -> {
                    context.screen().mouseMoved(swipe[0], swipe[2]);
                    context.screen().mouseDragged(swipe[0], swipe[2], Keys.MOUSE_LEFT,
                            0, swipe[2] - swipe[1]);
                })
                .step("release lock screen through Screen", context ->
                        context.screen().mouseReleased(swipe[0], swipe[2], Keys.MOUSE_LEFT))
                .waitUntil("lock screen unlock animation completes", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    PhoneUI phoneUI = (PhoneUI) screen.getModularUI().ui.rootElement;
                    return phoneUI.lockScreen.isUnlocked();
                })
                .click(APP_STORE)
                .waitUntil("app store opens from a mouse click", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    PhoneUI phoneUI = (PhoneUI) screen.getModularUI().ui.rootElement;
                    return phoneUI.homeScreen.appUI != null;
                })
                .check("held phone accepts mouse interaction", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    PhoneUI phoneUI = (PhoneUI) screen.getModularUI().ui.rootElement;
                    return phoneUI.homeScreen.appUI != null;
                })
                .screenshot("held_phone_app_store")
                .setHeldItem(ItemStack.EMPTY)
                .waitUntil("putting phone away closes its UI", context -> context.screen() == null)
                .check("held phone closes after switching item", context -> context.screen() == null)
                .teardown("put phone away and close test screen", context -> context.mc().setScreen(null))
                .teardownServer("clear held item", context -> context.player().setItemInHand(
                        net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }
}
