package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.data.NotepadData;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@LDLRegisterClient(name = "phone_editor_scale", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneEditorScaleScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "editor", "layout");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        float[] swipe = new float[3];
        scenario.step("remember client preferences", ctx -> {
                    ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
                    ctx.put("gameScale", ctx.mc().options.guiScale().get());
                    ctx.put("windowScale", ctx.mc().getWindow().getGuiScale());
                })
                .setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                .waitUntil("ordinary phone is raised", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.isRaised() && screen.isSynced())
                .step("remember ordinary phone size", ctx -> {
                    var bounds = ctx.el("#held_phone_lock").bounds();
                    ctx.put("phoneWidth", bounds.width());
                    ctx.put("phoneHeight", bounds.height());
                })
                .setHeldItem(ItemStack.EMPTY)
                .waitUntil("phone is closed", ctx -> ctx.screen() == null)
                .server("open real phone editor", ctx -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor"))
                .awaitElement("#phone_editor_size");
        selectSize(scenario, 1);
        scenario.check("size preference does not edit the phone draft", ctx ->
                        !((PhoneEditorUI) ctx.requireUI().ui.rootElement).isDirty())
                .step("remember small control size", ctx -> ctx.put("smallControlHeight", ctx.el("#phone_editor_save").bounds().height() * ctx.mc().getWindow().getGuiScale()))
                .screenshot("editor_size_1")
                .click("#phone_editor_tab_notes")
                .click("#phone_editor_notes")
                .type("Scale draft")
                .check("typing works at size one", ctx -> String.join("\n", ((PhoneEditorUI) ctx.requireUI().ui.rootElement)
                        .getPreview().phoneInfo.getOrCreateExtensionData(NotepadData.class).getText()).contains("Scale draft"));
        selectSize(scenario, 2);
        scenario.check("controls double in size at level two", ctx -> Math.abs(
                        ctx.el("#phone_editor_save").bounds().height() * ctx.mc().getWindow().getGuiScale() / (double) ctx.get("smallControlHeight") - 2f) < .05f)
                .check("resizing preserves the draft", ctx -> ((PhoneEditorUI) ctx.requireUI().ui.rootElement).isDirty()
                        && String.join("\n", ((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().phoneInfo
                        .getOrCreateExtensionData(NotepadData.class).getText()).contains("Scale draft"));
        selectSize(scenario, 0);
        scenario.check("automatic size follows the window limit", ctx -> {
                    int maximum = ctx.mc().getWindow().calculateScale(0, ctx.mc().isEnforceUnicode());
                    return Math.abs(ctx.el("#phone_editor_save").bounds().height() * ctx.mc().getWindow().getGuiScale()
                            / (double) ctx.get("smallControlHeight") - maximum) < .05f;
                })
                .check("editor controls remain on screen", ctx ->
                        ctx.el("#phone_editor_size").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height)
                        && ctx.el("#phone_editor_save").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height))
                .step("locate scaled preview swipe", ctx -> {
                    var bounds = ctx.el("#phone_editor_lock").bounds();
                    swipe[0] = bounds.centerX();
                    swipe[1] = bounds.y() + bounds.height() * .85f;
                    swipe[2] = bounds.y() + bounds.height() * .05f;
                })
                .step("press scaled preview", ctx -> ctx.input().mouseDown(swipe[0], swipe[1], 0))
                .step("swipe scaled preview", ctx -> ctx.input().dragTo(swipe[0], swipe[2], 0))
                .step("release scaled preview", ctx -> ctx.input().mouseUp(swipe[0], swipe[2], 0))
                .waitUntil("scaled preview unlocks", ctx -> ((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().lockScreen.isUnlocked())
                .waitMs(600)
                .click("#phone_app_smart_phone_chat_room")
                .check("preview app responds after resizing", ctx -> ((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().homeScreen.appUI != null)
                .screenshot("editor_size_auto")
                .hover("#phone_editor_size")
                .ticks(2)
                .screenshot("editor_native_tooltip")
                .check("game preference is unchanged while editor uses native scale", ctx ->
                        ctx.mc().options.guiScale().get().equals(ctx.get("gameScale"))
                        && ctx.mc().getWindow().getGuiScale() == ctx.mc().getWindow().calculateScale(0, ctx.mc().isEnforceUnicode()))
                .step("leave editor without saving the draft", ctx -> ctx.mc().setScreen(null))
                .server("reopen editor to verify independent preference", ctx -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor"))
                .awaitElement("#phone_editor_size")
                .check("editor remembers the automatic size preference", ctx ->
                        Integer.valueOf(0).equals(((Selector<?>) ctx.el("#phone_editor_size").element()).getValue())
                        && ClientConfig.EDITOR_GUI_SCALE.get() == 0)
                .step("close editor with Escape", ctx -> ctx.screen().onClose())
                .setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                .waitUntil("ordinary phone is raised again", ctx -> ctx.screen() instanceof HeldPhoneScreen screen
                        && screen.isRaised() && screen.isSynced())
                .check("ordinary phone dimensions are unchanged", ctx -> {
                    var bounds = ctx.el("#held_phone_lock").bounds();
                    return Math.abs(bounds.width() - (float) ctx.get("phoneWidth")) < 1f
                            && Math.abs(bounds.height() - (float) ctx.get("phoneHeight")) < 1f;
                })
                .check("game GUI scale stays unchanged after editor exit", ctx ->
                        ctx.mc().options.guiScale().get().equals(ctx.get("gameScale"))
                        && Double.valueOf(ctx.mc().getWindow().getGuiScale()).equals(ctx.get("windowScale")))
                .teardown("restore editor preference and close screen", ctx -> {
                    if (ctx.state().containsKey("editorScale")) {
                        ClientConfig.EDITOR_GUI_SCALE.set(ctx.get("editorScale"));
                        ClientConfig.EDITOR_GUI_SCALE.save();
                    }
                    ctx.mc().setScreen(null);
                })
                .teardownServer("clear test phone", ctx -> ctx.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }

    private static void selectSize(ScenarioBuilder scenario, int size) {
        scenario.click("#phone_editor_size")
                .awaitElement("#phone_editor_size_menu")
                .check("dropdown is anchored directly below its selector", ctx -> {
                    var anchor = ctx.el("#phone_editor_size").bounds();
                    var menu = ctx.el("#phone_editor_size_menu").bounds();
                    return Math.abs(anchor.x() - menu.x()) < 2 && Math.abs(anchor.bottom() - menu.y()) < 2;
                })
                .screenshot("size_menu_before_" + size)
                .step("choose editor size " + size, ctx -> {
                    var bounds = ctx.el("#phone_editor_size_menu #phone_editor_size_option_" + size).bounds();
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                    ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
                })
                .ticks(3)
                .check("size selection is applied: " + size, ctx -> Integer.valueOf(size).equals(
                        ((Selector<?>) ctx.el("#phone_editor_size").element()).getValue()));
    }
}
