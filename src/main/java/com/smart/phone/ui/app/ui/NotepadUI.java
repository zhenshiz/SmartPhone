package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.smart.phone.ui.data.NotepadData;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class NotepadUI extends AppUI {
    public final TextArea textArea;

    public NotepadUI(HomeScreen homeScreen) {
        super(homeScreen);

        textArea = new TextArea();
        textArea.textAreaStyle(style -> {
            style.fontSize(6);
            style.placeholder(Component.empty());
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.flexGrow(1);
            layout.paddingAll(6);
        });

        NotepadData data = homeScreen.getPhoneUI().phoneInfo.getOrCreateExtensionData(NotepadData.class);
        textArea.setLines(List.of(data.getText()));

        textArea.setLinesResponder(lines -> {
            data.setText(lines);
            homeScreen.savePhoneData();
        });

        this.removeChild(appScrollView);
        this.addChildren(textArea);
    }
}
