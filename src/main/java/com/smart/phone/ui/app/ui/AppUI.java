package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.client.Minecraft;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaPositionType;

// APP UI的基础配置 避免重复配置
public class AppUI extends UIElement {
    public final HomeScreen homeScreen;
    public final ScrollerView appScrollView = new ScrollerView();
    public final Minecraft minecraft = Minecraft.getInstance();

    public AppUI(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;

        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setPadding(YogaEdge.TOP, 8);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setAlignItems(YogaAlign.CENTER);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        appScrollView.viewContainer.layout(layout -> layout.setAlignItems(YogaAlign.CENTER));
        appScrollView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        appScrollView.scrollerStyle(style -> {
            style.verticalScrollDisplay(ScrollDisplay.NEVER);
            style.horizontalScrollDisplay(ScrollDisplay.NEVER);
        }).layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlexGrow(1);
        });

        this.addChildren(appScrollView);
    }
}
