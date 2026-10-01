package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 将存档时间戳显示为本地日期，并通过日历编辑。 */
public final class DateTimeConfigurator extends Configurator {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    public DateTimeConfigurator(Supplier<Long> getter, Consumer<Long> setter, String id) {
        super("smartPhone.editor.dateTime");
        Button button = new Button();
        button.setId(id);
        button.layout(l -> l.widthPercent(100).height(16));
        Runnable refresh = () -> button.setText(Component.literal(FORMAT.format(
                Instant.ofEpochMilli(getter.get()).atZone(ZoneId.systemDefault()))));
        refresh.run();
        button.addEventListener(UIEvents.TICK, event -> refresh.run());
        button.setOnClick(event -> new DateTimePicker(getter.get(), setter).show(getModularUI()));
        inlineContainer.addChild(button);
    }
}
