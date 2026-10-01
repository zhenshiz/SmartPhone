package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.*;
import java.util.function.Consumer;

/** 日期与时间的临时选择；仅确定时提交，取消不更改原值。 */
public final class DateTimePicker extends Dialog {
    private static final String KEY = "smartPhone.editor.calendar.";
    private final ZoneId zone = ZoneId.systemDefault();
    private final Consumer<Long> onConfirm;
    private final UIElement calendar = new UIElement();
    private final TextField year = numberField("year", 4);
    private final TextField hour = numberField("hour", 2);
    private final TextField minute = numberField("minute", 2);
    private final TextField second = numberField("second", 2);
    private final Label monthLabel = new Label();
    private final Label error = new Label();
    private final Button confirm;
    private LocalDate selected;
    private YearMonth month;

    public DateTimePicker(long initialMillis, Consumer<Long> onConfirm) {
        this.onConfirm = onConfirm;
        var initial = Instant.ofEpochMilli(initialMillis).atZone(zone);
        selected = initial.toLocalDate();
        month = YearMonth.from(selected);
        setId("phone_datetime_picker");
        setTitle(KEY + "title");
        setAutoClose(false);
        overlay.layout(l -> l.width(272));
        titleBar.layout(l -> l.paddingAll(3));
        contentContainer.layout(l -> l.paddingAll(4).gapAll(2));
        buttonContainer.layout(l -> l.paddingAll(3));

        UIElement navigation = row(18);
        year.layout(l -> l.width(40));
        year.setText(Integer.toString(month.getYear()), false);
        year.setTextResponder(value -> {
            Integer parsed = parse(year, 1, 9999);
            if (parsed != null) {
                month = month.withYear(parsed);
                selected = month.atDay(Math.min(selected.getDayOfMonth(), month.lengthOfMonth()));
                rebuildCalendar();
            }
        });
        monthLabel.textStyle(s -> s.textAlignHorizontal(Horizontal.CENTER).textWrap(TextWrap.NONE));
        monthLabel.layout(l -> l.flex(1));
        navigation.addChildren(action("previousYear", "«", () -> moveMonths(-12)),
                action("previousMonth", "‹", () -> moveMonths(-1)), year, label("yearUnit"), monthLabel,
                action("nextMonth", "›", () -> moveMonths(1)), action("nextYear", "»", () -> moveMonths(12)));
        UIElement weekdays = row(12);
        for (String day : new String[]{"sun", "mon", "tue", "wed", "thu", "fri", "sat"}) {
            Label label = label(day);
            label.layout(l -> l.flex(1));
            label.textStyle(s -> s.textAlignHorizontal(Horizontal.CENTER));
            weekdays.addChild(label);
        }
        calendar.layout(l -> l.widthPercent(100).gapAll(1));
        UIElement time = row(19);
        time.addChildren(label("hour"), hour, label("minute"), minute, label("second"), second);
        hour.layout(l -> l.flex(1));
        minute.layout(l -> l.flex(1));
        second.layout(l -> l.flex(1));
        setTime(initial.toLocalTime());
        Label timezone = new Label();
        timezone.setText(Component.translatable(KEY + "zone", zone.getId()));
        timezone.layout(l -> l.widthPercent(100));
        timezone.textStyle(s -> s.textWrap(TextWrap.HIDE));
        error.setId("phone_datetime_error");
        error.layout(l -> l.widthPercent(100).height(10));
        error.textStyle(s -> s.textWrap(TextWrap.HIDE));
        addContent(navigation).addContent(weekdays).addContent(calendar).addContent(time).addContent(timezone).addContent(error);
        confirm = new Button().setText("ldlib.gui.tips.confirm").setOnClick(event -> {
            Long millis = selectedMillis();
            if (millis == null) return;
            this.onConfirm.accept(millis);
            close();
        });
        confirm.setId("phone_datetime_confirm");
        Button now = new Button().setText(KEY + "now").setOnClick(event -> {
            var value = ZonedDateTime.now(zone);
            selected = value.toLocalDate();
            month = YearMonth.from(selected);
            year.setText(Integer.toString(month.getYear()), false);
            setTime(value.toLocalTime());
            rebuildCalendar();
        });
        now.setId("phone_datetime_now");
        Button cancel = new Button().setText("ldlib.gui.tips.cancel").setOnClick(event -> close());
        cancel.setId("phone_datetime_cancel");
        addButton(now).addButton(confirm).addButton(cancel);
        addEventListener(UIEvents.TICK, event -> {
            boolean valid = selectedMillis() != null;
            confirm.setActive(valid);
            error.setText(valid ? Component.empty() : Component.translatable(KEY + "invalid"));
        });
        rebuildCalendar();
    }

    @Override
    protected void keyDown(UIEvent event) {
        if (event.keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            event.stopPropagation();
        } else {
            super.keyDown(event);
        }
    }

    private void moveMonths(int offset) {
        YearMonth next = month.plusMonths(offset);
        if (next.getYear() < 1 || next.getYear() > 9999) return;
        month = next;
        selected = month.atDay(Math.min(selected.getDayOfMonth(), month.lengthOfMonth()));
        year.setText(Integer.toString(month.getYear()), false);
        rebuildCalendar();
    }

    private void rebuildCalendar() {
        monthLabel.setText(Component.translatable(KEY + "month", month.getMonthValue()));
        calendar.clearAllChildren();
        LocalDate first = month.atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() % 7);
        for (int week = 0; week < 6; week++) {
            UIElement row = row(15);
            for (int day = 0; day < 7; day++) {
                LocalDate date = start.plusDays(week * 7L + day);
                Tab cell = new Tab().setText(Integer.toString(date.getDayOfMonth()));
                cell.setId("phone_datetime_day_" + date);
                cell.layout(l -> l.flex(1).height(15).paddingAll(1));
                cell.text.layout(l -> l.widthPercent(100));
                cell.text.textStyle(s -> s.adaptiveWidth(false));
                cell.setSelected(selected.equals(date));
                if (!YearMonth.from(date).equals(month)) {
                    cell.setActive(false);
                    cell.style(s -> s.opacity(0.4f));
                }
                cell.addEventListener(UIEvents.CLICK, event -> {
                    if (event.button != 0) return;
                    selected = date;
                    rebuildCalendar();
                });
                row.addChild(cell);
            }
            calendar.addChild(row);
        }
    }

    private Long selectedMillis() {
        Integer y = parse(year, 1, 9999);
        Integer h = parse(hour, 0, 23);
        Integer m = parse(minute, 0, 59);
        Integer s = parse(second, 0, 59);
        if (y == null || h == null || m == null || s == null) return null;
        LocalDateTime dateTime = selected.atTime(h, m, s);
        var offsets = zone.getRules().getValidOffsets(dateTime);
        // 夏令时跳过的本地时间不能悄悄改成另一个时刻。
        if (offsets.isEmpty()) return null;
        return dateTime.toInstant(offsets.getFirst()).toEpochMilli();
    }

    private void setTime(LocalTime time) {
        hour.setText(String.format("%02d", time.getHour()), false);
        minute.setText(String.format("%02d", time.getMinute()), false);
        second.setText(String.format("%02d", time.getSecond()), false);
    }

    private static Integer parse(TextField field, int min, int max) {
        try {
            int value = Integer.parseInt(field.getRawText());
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static TextField numberField(String id, int length) {
        TextField field = new TextField();
        field.setId("phone_datetime_" + id);
        field.setTextValidator(value -> value.matches("[0-9]{1," + length + "}"));
        field.setCharValidator(Character::isDigit);
        field.layout(l -> l.height(16));
        return field;
    }

    private static UIElement row(int height) {
        return new UIElement().layout(l -> l.widthPercent(100).height(height).flexShrink(0)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));
    }

    private static Label label(String key) {
        Label label = new Label();
        label.setText(KEY + key);
        label.textStyle(s -> s.adaptiveWidth(true));
        label.layout(l -> l.flexShrink(0));
        return label;
    }

    private Button action(String id, String symbol, Runnable action) {
        Button button = new Button().setText(Component.literal(symbol)).setOnClick(event -> action.run());
        button.setId("phone_datetime_" + id);
        button.style(s -> s.tooltips(KEY + id));
        button.layout(l -> l.width(18).height(16).flexShrink(0));
        return button;
    }
}
