package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.smart.phone.ui.view.HomeScreen;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;

// APP UI的基础配置 避免重复配置
public class AppUI extends UIElement {
    public final HomeScreen homeScreen;
    public final ScrollerView appScrollView = new ScrollerView();
    public final Minecraft minecraft = Minecraft.getInstance();

    public AppUI(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;

        this.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.paddingTop(8);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        appScrollView.viewContainer.layout(layout -> layout.alignItems(AlignItems.CENTER));
        appScrollView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        appScrollView.scrollerStyle(style -> {
            style.verticalScrollDisplay(ScrollDisplay.NEVER);
            style.horizontalScrollDisplay(ScrollDisplay.NEVER);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        this.addChildren(appScrollView);
    }
}