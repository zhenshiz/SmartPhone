package com.smart.phone.ui.components;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;
import org.appliedenergistics.yoga.style.StyleSizeLength;

public class Toast extends UIElement {

    public Toast(Component message, float durationSeconds) {
        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setPosition(YogaEdge.TOP, 10);
            layout.setAlignSelf(YogaAlign.CENTER);
            layout.setWidthPercent(80);
            layout.setPadding(YogaEdge.ALL, 5);
        });

        this.style(style -> {
            style.backgroundTexture(Sprites.RECT_DARK);
            style.opacity(0f);
        });

        Label textLabel = new Label();
        textLabel.setText(message);
        textLabel.textStyle(style -> style
                .textColor(ColorPattern.WHITE.color)
                .textWrap(TextWrap.WRAP)
                .fontSize(6)
                .adaptiveHeight(true)
                .adaptiveWidth(true)
        );
        this.addChild(textLabel);

        // 进度条
        UIElement progressBar = new UIElement();
        progressBar.layout(layout -> layout
                .setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.BOTTOM, 0)
                .setPosition(YogaEdge.LEFT, 0)
                .setHeight(1)
                .setWidthPercent(0) // 初始宽度 0
        );
        progressBar.style(style -> style.backgroundTexture(ColorPattern.GREEN.rectTexture()));
        this.addChild(progressBar);

        // Toast 淡入动画
        this.animation(anim -> anim
                .duration(0.2f)
                .style(PropertyRegistry.OPACITY, 1f)
                .start()
        );

        // 进度条动画
        progressBar.animation(anim -> anim
                .duration(durationSeconds)
                .style(YogaProperties.WIDTH, StyleSizeLength.percent(100))
                .onFinished(target -> startFadeOut(durationSeconds))
                .start()
        );
    }

    private void startFadeOut(float durationSeconds) {
        this.animation(anim -> anim
                .duration(durationSeconds)
                .style(PropertyRegistry.OPACITY, 0f)
                .onFinished(target -> {
                    this.removeSelf();
                })
                .start()
        );
    }

    public static void show(UIElement container, Component message, float duration) {
        Toast toast = new Toast(message, duration);
        container.addChild(toast);
    }
}