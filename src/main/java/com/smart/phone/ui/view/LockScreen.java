package com.smart.phone.ui.view;

import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.PhoneUI;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

// 解锁窗口
@Getter
public class LockScreen extends UIElement {
    private final PhoneUI phoneUI;
    private final HomeScreen homeScreen;

    private boolean isDragging = false;
    private float dragStartY = 0;
    private float currentOffsetY = 0;
    private final float unlockThreshold = 0.5f;

    private static final IGuiTexture UN_LOCK = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/unlock.png"));

    public LockScreen(PhoneUI phoneUI) {
        this.phoneUI = phoneUI;
        this.homeScreen = phoneUI.homeScreen;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
            layout.setPadding(YogaEdge.VERTICAL, 20);
        }).style(style -> {
            style.zIndex(10);
        }).addEventListener(UIEvents.TICK, event -> {
            event.target.getStyle().backgroundTexture(SpriteTexture.of(phoneUI.phoneInfo.getPhoneWallpaper()));
        });

        UIElement topSection = new UIElement().layout(l -> {
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
        }).addChildren(new Label().textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(12);
            textStyle.font(LDLibFonts.JETBRAINS_MONO_BOLD);
        }).addEventListener(UIEvents.TICK, event -> {
            Label target = (Label) event.target;
            target.setText("%s:%s".formatted(
                    phoneUI.phoneInfo.getIPhoneTimeSource().getHour(),
                    phoneUI.phoneInfo.getIPhoneTimeSource().getMinute()
            ));
        }), new Label().setText(Component.translatable("smartPhone.ui.lock.name", player.getDisplayName().getString())).textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(5);
        }));

        UIElement bottomSection = new UIElement().layout(l -> {
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
        }).addChildren(new UIElement().layout(layout -> layout.setHeight(12).setWidth(12).setMargin(YogaEdge.BOTTOM, 3)).style(style -> {
            style.backgroundTexture(UN_LOCK);
        }), new Label().setText("smartPhone.ui.lock.tip").textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(5);
        }));

        this.addChildren(topSection, bottomSection);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_MOVE, this::onMouseMove);
    }

    /**
     * 鼠标按下 - 开始拖拽
     */
    private void onMouseDown(UIEvent event) {
        isDragging = true;
        dragStartY = event.y;
        currentOffsetY = 0;
        event.stopPropagation();
    }

    /**
     * 鼠标移动 - 处理拖拽
     */
    private void onMouseMove(UIEvent event) {
        if (!isDragging) return;

        // 计算拖拽距离（向上为负）
        float deltaY = event.y - dragStartY;

        // 只允许向上拖拽
        if (deltaY > 0) {
            deltaY = 0;
        }

        // 计算当前偏移量
        float maxDragDistance = -getSizeHeight();
        currentOffsetY = Math.max(deltaY, maxDragDistance);

        // 应用变换：移动锁屏
        float progress = Math.abs(currentOffsetY) / getSizeHeight();

        // 更新锁屏位置
        transform(transform -> {
            transform.translate(0, currentOffsetY);
        });

        if (homeScreen != null) {
            float homeScreenOffset = getSizeHeight() * (1 - progress);
            homeScreen.transform(transform -> {
                transform.translate(0, homeScreenOffset);
            });

            homeScreen.style(style -> {
                style.opacity(progress);
            });
        }

        event.stopPropagation();
    }

    /**
     * 鼠标松开 - 判断是否解锁或回弹
     */
    private void onMouseUp(UIEvent event) {
        if (!isDragging) return;

        isDragging = false;

        float progress = Math.abs(currentOffsetY) / getSizeHeight();

        if (progress >= unlockThreshold) {
            // 超过阈值 - 完全解锁
            performUnlockAnimation();
        } else {
            // 未达到阈值 - 回弹到锁定状态
            performSnapBackAnimation();
        }

        event.stopPropagation();
    }

    /**
     * 执行解锁动画
     */
    private void performUnlockAnimation() {
        this.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, -getSizeHeight()))
                .onFinished(ui -> ui.getStyle().opacity(0))
                .start();

        homeScreen.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, 0))
                .style(PropertyRegistry.OPACITY, 1f)
                .start();
    }

    /**
     * 执行回弹动画
     */
    private void performSnapBackAnimation() {
        this.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, 0))
                .start();

        homeScreen.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, getSizeHeight()))
                .onFinished(ui -> ui.getStyle().opacity(0))
                .start();
    }
}
