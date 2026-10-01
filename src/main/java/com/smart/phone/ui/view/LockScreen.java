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
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.PhoneUI;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

// 解锁窗口
@Getter
public class LockScreen extends UIElement {
    private final PhoneUI phoneUI;
    private final HomeScreen homeScreen;

    private boolean isDragging = false;
    private boolean unlocked = false;
    private float dragStartY = 0;
    private float currentOffsetY = 0;
    private final float unlockThreshold = 0.5f;
    private ISubscription lockAnimation;
    private ISubscription homeAnimation;

    private static final IGuiTexture UN_LOCK = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/unlock.png"));
    private static final IGuiTexture LOCKED = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/lock.png"));

    public LockScreen(PhoneUI phoneUI) {
        this.phoneUI = phoneUI;
        this.homeScreen = phoneUI.homeScreen;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        this.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.paddingVertical(20);
        }).style(style -> {
            style.zIndex(10);
        }).addEventListener(UIEvents.TICK, event -> {
            event.target.getStyle().backgroundTexture(SpriteTexture.of(phoneUI.phoneInfo.getPhoneWallpaper()));
        });

        UIElement topSection = new UIElement().layout(l -> {
            l.flexDirection(FlexDirection.COLUMN);
            l.alignItems(AlignItems.CENTER);
        }).addChildren(new Label().textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(12);
            textStyle.font(LDLibFonts.JETBRAINS_MONO_BOLD);
        }).setId("phone_standby_time").addEventListener(UIEvents.TICK, event -> {
            Label target = (Label) event.target;
            target.setVisible(!phoneUI.phoneInfo.isHideDate());
            target.setText("%s:%s".formatted(
                    phoneUI.phoneInfo.getIPhoneTimeSource().getHour(),
                    phoneUI.phoneInfo.getIPhoneTimeSource().getMinute()
            ));
        }), new Label().textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(5);
        }).setId("phone_owner_name").addEventListener(UIEvents.TICK, event -> {
            event.target.setVisible(!phoneUI.phoneInfo.isHideOwnerName());
            ((Label) event.target).setText(Component.translatable("smartPhone.ui.lock.name", phoneUI.getOwnerName()));
        }));

        UIElement bottomSection = new UIElement().layout(l -> {
            l.flexDirection(FlexDirection.COLUMN);
            l.alignItems(AlignItems.CENTER);
        }).addChildren(new UIElement().layout(layout -> layout.height(12).width(12).marginBottom(3)).style(style -> {
            style.backgroundTexture(UN_LOCK);
        }).setId("phone_lock_icon").addEventListener(UIEvents.TICK, event -> {
            event.target.setVisible(!phoneUI.phoneInfo.isHideLockIcon());
            event.target.style(s -> s.backgroundTexture(phoneUI.phoneInfo.isBlocked() ? LOCKED : UN_LOCK));
        }),
                new Label().setText("smartPhone.ui.lock.tip").textStyle(textStyle -> {
            textStyle.adaptiveHeight(true);
            textStyle.adaptiveWidth(true);
            textStyle.fontSize(5);
        }).setId("phone_unlock_hint").addEventListener(UIEvents.TICK,
                event -> event.target.setVisible(!phoneUI.phoneInfo.isBlocked())));

        this.addChildren(topSection, bottomSection);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        // 拖动源持续接收更新和松开，即使指针已经滑出手机或锁屏自身正在移动。
        addEventListener(UIEvents.DRAG_END, this::onMouseUp);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onMouseMove);
    }

    public boolean isUnlocked() {
        return !phoneUI.isAccessLocked() && (unlocked || currentOffsetY <= -getSizeHeight() * unlockThreshold);
    }

    /**
     * 鼠标按下 - 开始拖拽
     */
    private void onMouseDown(UIEvent event) {
        if (event.button != 0) return;
        if (phoneUI.phoneInfo.isBlocked()) { event.stopPropagation(); return; }
        stopAnimations();
        isDragging = true;
        dragStartY = phoneUI.worldToLocal(new Vector2f(event.x, event.y)).y;
        currentOffsetY = 0;
        transform(t -> t.translate(0, 0));
        startDrag(this, null);
        event.stopPropagation();
    }

    /**
     * 鼠标移动 - 处理拖拽
     */
    private void onMouseMove(UIEvent event) {
        if (!isDragging || phoneUI.phoneInfo.isBlocked()) return;

        // 计算拖拽距离（向上为负）
        // 用手机的本地坐标比较距离，预览缩放后仍保持相同的滑动解锁比例。
        float deltaY = phoneUI.worldToLocal(new Vector2f(event.x, event.y)).y - dragStartY;

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

        if (homeScreen != null && !phoneUI.isAccessLocked()) {
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
        if (!isDragging || phoneUI.phoneInfo.isBlocked()) return;

        isDragging = false;

        float progress = Math.abs(currentOffsetY) / getSizeHeight();

        if (progress >= unlockThreshold) {
            // 超过阈值 - 完全解锁
            if (phoneUI.isAccessLocked()) {
                resetLocked();
                phoneUI.showPasscode(PasscodeView.Mode.UNLOCK);
            } else performUnlockAnimation();
        } else {
            // 未达到阈值 - 回弹到锁定状态
            performSnapBackAnimation();
        }

        event.stopPropagation();
    }

    /**
     * 执行解锁动画
     */
    public void resetLocked() {
        stopAnimations();
        unlocked = false;
        isDragging = false;
        currentOffsetY = 0;
        transform(t -> t.translate(0, 0));
        style(s -> s.opacity(1));
        if (getParent() == null) phoneUI.screenContainer.addChild(this);
        homeScreen.transform(t -> t.translate(0, getSizeHeight()));
        homeScreen.style(s -> s.opacity(0));
    }

    public void unlockAfterVerification() { performUnlockAnimation(); }

    /** 在管理员内容预览中立即显示桌面，不改变草稿的拦截设置。 */
    public void showEditorContent() {
        if (!phoneUI.isPreview()) return;
        stopAnimations();
        isDragging = false;
        unlocked = true;
        phoneUI.screenContainer.removeChild(this);
        homeScreen.setActive(true);
        homeScreen.setVisible(true);
        homeScreen.transform(t -> t.translate(0, 0));
        homeScreen.style(s -> s.opacity(1));
    }

    private void performUnlockAnimation() {
        if (phoneUI.phoneInfo.isBlocked()) return;
        stopAnimations();
        unlocked = true;
        lockAnimation = this.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, -getSizeHeight()))
                .onFinished(ui -> ui.getStyle().opacity(0))
                .start();

        homeAnimation = homeScreen.animation()
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
        stopAnimations();
        unlocked = false;
        currentOffsetY = 0;
        lockAnimation = this.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, 0))
                .start();

        homeAnimation = homeScreen.animation()
                .duration(0.5f)
                .ease(Eases.QUAD_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(0, getSizeHeight()))
                .onFinished(ui -> ui.getStyle().opacity(0))
                .start();
    }

    private void stopAnimations() {
        if (lockAnimation != null) lockAnimation.unsubscribe();
        if (homeAnimation != null) homeAnimation.unsubscribe();
        getStyleBag().removeCandidates(slot -> slot.origin() == StyleOrigin.ANIMATION);
        homeScreen.getStyleBag().removeCandidates(slot -> slot.origin() == StyleOrigin.ANIMATION);
    }
}
