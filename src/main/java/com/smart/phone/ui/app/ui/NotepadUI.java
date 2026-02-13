package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
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
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        textArea.setLines(List.of(homeScreen.getPhoneUI().phoneInfo.getNotepadText()));

        textArea.setLinesResponder(lines -> {
            homeScreen.getPhoneUI().phoneInfo.setNotepadText(lines);
        });

        appScrollView.viewContainer.addChildren(textArea);
    }
}
