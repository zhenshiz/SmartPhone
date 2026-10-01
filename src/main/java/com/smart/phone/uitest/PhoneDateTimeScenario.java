package com.smart.phone.uitest;

import com.mojang.authlib.GameProfile;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.smart.phone.SmartPhone;
import com.smart.phone.ClientConfig;
import com.smart.phone.ui.data.*;
import com.smart.phone.ui.data.chat.*;
import com.smart.phone.ui.editor.PhoneEditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.*;
import java.util.UUID;

@LDLRegisterClient(name = "phone_datetime", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneDateTimeScenario implements UIScenario {
    private static final UUID OWNER = UUID.fromString("ab291c40-9db7-41a4-86c3-b32a6ee8bd40");
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final long INITIAL = LocalDateTime.of(2024, 1, 31, 12, 34, 56).atZone(ZONE).toInstant().toEpochMilli();
    private static final long EXPECTED = LocalDateTime.of(2024, 2, 29, 23, 58, 57).atZone(ZONE).toInstant().toEpochMilli();

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("editor", "calendar", "persistence");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.step("use maximum native editor size", ctx -> {
                    ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
                    ClientConfig.EDITOR_GUI_SCALE.set(0);
                }).setHeldItem(ItemStack.EMPTY)
                .server("create dated message and conversation", ctx -> {
                    PhoneInfo info = new PhoneInfo();
                    OfficialMessage message = new OfficialMessage("Guide", "Calendar test", "Date and time");
                    message.setCreatedAtMillis(INITIAL);
                    info.getOrCreateExtensionData(OfficialMessagesData.class).addMessage(message);
                    ChatRoom room = new ChatRoom("preset:calendar", "Calendar");
                    ChatRoomMessage chat = new ChatRoomMessage(room.getRoomId(), UUID.randomUUID(), "Guide", "Hello");
                    chat.setCreatedAtMillis(INITIAL);
                    room.addMessage(chat);
                    info.getOrCreateExtensionData(PresetChatsData.class).getRooms().add(room);
                    SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
                    ctx.server().getProfileCache().add(new GameProfile(OWNER, "CalendarOwner"));
                    ctx.server().getCommands().performPrefixedCommand(ctx.player().createCommandSourceStack().withPermission(2), "smart_phone editor " + OWNER);
                })
                .awaitElement("#phone_editor")
                .click("#phone_editor_tab_messages")
                .step("select dated message", ctx -> {
                    var message = ((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().phoneInfo
                            .getOrCreateExtensionData(OfficialMessagesData.class).getMessages().getFirst();
                    click(ctx, "#official_message_" + message.getMessageId());
                })
                .step("open message date picker", ctx -> click(ctx, "#phone_editor_message_time"))
                .awaitElement("#phone_datetime_picker")
                .check("calendar fits the screen", ctx -> {
                    var root = ctx.el("#phone_datetime_picker").element();
                    var bounds = ElementBounds.of(((com.smart.phone.ui.editor.DateTimePicker) root).overlay);
                    return bounds.y() >= 0 && bounds.bottom() <= ctx.screen().height
                            && bounds.x() >= 0 && bounds.right() <= ctx.screen().width;
                })
                .click("#phone_datetime_nextMonth")
                .check("leap February includes February 29", ctx -> ctx.el("#phone_datetime_day_2024-02-29").element().isActive())
                .click("#phone_datetime_day_2024-02-29");
        replace(scenario, "hour", "24");
        scenario.ticks(2).check("invalid hour prevents confirmation", ctx -> !ctx.el("#phone_datetime_confirm").element().isActive());
        replace(scenario, "hour", "23");
        replace(scenario, "minute", "58");
        replace(scenario, "second", "57");
        scenario.ticks(2).screenshot("date_and_time_selection");
        clickClosing(scenario, "#phone_datetime_cancel");
        scenario.check("cancel leaves message time and draft unchanged", ctx -> {
                    var editor = (PhoneEditorUI) ctx.requireUI().ui.rootElement;
                    return !editor.isDirty() && editor.getPreview().phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class)
                            .getMessages().getFirst().getCreatedAtMillis() == INITIAL;
                })
                .step("open message date picker", ctx -> click(ctx, "#phone_editor_message_time"))
                .click("#phone_datetime_nextYear")
                .click("#phone_datetime_nextMonth")
                .check("non-leap February stops at 28", ctx -> ctx.el("#phone_datetime_day_2025-02-28").element().isActive()
                        && !ctx.el("#phone_datetime_day_2025-03-01").element().isActive())
                .click("#phone_datetime_previousYear")
                .click("#phone_datetime_day_2024-02-29");
        replace(scenario, "hour", "23");
        replace(scenario, "minute", "58");
        replace(scenario, "second", "57");
        clickClosing(scenario, "#phone_datetime_confirm");
        scenario.check("selected date and seconds update the draft", ctx -> ((PhoneEditorUI) ctx.requireUI().ui.rootElement)
                        .getPreview().phoneInfo.getOrCreateExtensionData(OfficialMessagesData.class).getMessages().getFirst().getCreatedAtMillis() == EXPECTED)
                .checkServer("calendar confirmation has not saved to server yet", ctx -> SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER)
                        .getOrCreateExtensionData(OfficialMessagesData.class).getMessages().getFirst().getCreatedAtMillis() == INITIAL)
                .click("#phone_editor_tab_chats")
                .step("select calendar channel", ctx -> click(ctx, "#chat_room_preset_calendar"))
                .step("select its message", ctx -> {
                    var room = ((PhoneEditorUI) ctx.requireUI().ui.rootElement).getPreview().phoneInfo
                            .getOrCreateExtensionData(PresetChatsData.class).getRooms().getFirst();
                    click(ctx, "#chat_message_" + room.getMessages().getFirst().getMessageId());
                })
                .step("open chat date picker", ctx -> click(ctx, "#phone_editor_chat_time"))
                .click("#phone_datetime_nextMonth")
                .click("#phone_datetime_day_2024-02-29");
        replace(scenario, "hour", "23");
        replace(scenario, "minute", "58");
        replace(scenario, "second", "57");
        clickClosing(scenario, "#phone_datetime_confirm");
        scenario.click("#phone_editor_save")
                .waitUntil("save acknowledged", ctx -> !((PhoneEditorUI) ctx.requireUI().ui.rootElement).isDirty())
                .checkServer("both message types persist the selected instant", ctx -> {
                    var data = SmartPhone.getPhoneSavedData().getPhoneInfo(OWNER);
                    return data.getOrCreateExtensionData(OfficialMessagesData.class).getMessages().getFirst().getCreatedAtMillis() == EXPECTED
                            && data.getOrCreateExtensionData(PresetChatsData.class).getRooms().getFirst().getMessages().getFirst().getCreatedAtMillis() == EXPECTED;
                })
                .step("open chat date picker", ctx -> click(ctx, "#phone_editor_chat_time"))
                .check("picker reloads saved seconds", ctx -> ((com.lowdragmc.lowdraglib2.gui.ui.elements.TextField)
                        ctx.el("#phone_datetime_second").element()).getText().equals("57"))
                .screenshot("saved_date_reopened")
                .key(Keys.ESCAPE)
                .check("Escape closes only the calendar without changing the draft", ctx -> ctx.exists("#phone_editor")
                        && !ctx.exists("#phone_datetime_picker")
                        && !((PhoneEditorUI) ctx.requireUI().ui.rootElement).isDirty())
                .teardown("close date editor and restore size", ctx -> {
                    ctx.mc().setScreen(null);
                    if (ctx.state().containsKey("editorScale")) ClientConfig.EDITOR_GUI_SCALE.set(ctx.get("editorScale"));
                });
    }

    private static void replace(ScenarioBuilder scenario, String field, String value) {
        scenario.focus("#phone_datetime_" + field)
                .keyDown(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(GLFW.GLFW_KEY_A).keyUp(Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_SUPER : Keys.LEFT_CONTROL)
                .key(Keys.BACKSPACE).type(value);
    }

    private static void click(TestContext ctx, String selector) {
        var bounds = ctx.el(selector).bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
    }

    private static void clickClosing(ScenarioBuilder scenario, String selector) {
        scenario.step("click " + selector, ctx -> {
            click(ctx, selector);
        }).ticks(2);
    }
}
