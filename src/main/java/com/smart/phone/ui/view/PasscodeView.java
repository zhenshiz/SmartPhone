package com.smart.phone.ui.view;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.PhoneUI;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 六位密码页：解锁、设置确认和旧密码验证使用同一个数字键盘。 */
public final class PasscodeView extends UIElement {
    public enum Mode { UNLOCK, SET, CHANGE, DISABLE }
    private final PhoneUI phone;
    private final Mode mode;
    private final Label title = new Label();
    private final Label message = new Label();
    private final UIElement[] dots = new UIElement[6];
    private String digits = "";
    private String currentPin = "";
    private String newPin = "";
    private int stage;
    private boolean waiting;

    public PasscodeView(PhoneUI phone, Mode mode) {
        this.phone = phone;
        this.mode = mode;
        stage = mode == Mode.SET ? 1 : 0;
        setId("phone_passcode");
        setFocusable(true);
        layout(l -> l.positionType(TaffyPosition.ABSOLUTE).widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.FLEX_START).paddingTop(14).gapAll(3));
        style(s -> s.zIndex(15).backgroundTexture(new ColorRectTexture(0xFF101724)));
        title.setId("phone_passcode_title");
        title.layout(l -> l.widthPercent(100).height(10));
        title.textStyle(s -> s.fontSize(6).textShadow(false).textAlignHorizontal(Horizontal.CENTER));
        var dotsRow = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(5).height(5)
                .alignItems(AlignItems.CENTER));
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new UIElement().layout(l -> l.width(4).height(4));
            dotsRow.addChild(dots[i]);
        }
        message.setId("phone_passcode_message");
        message.setText(Component.empty());
        message.layout(l -> l.widthPercent(96).height(10));
        message.textStyle(s -> s.fontSize(4).textShadow(false).textAlignHorizontal(Horizontal.CENTER));
        addChildren(title, dotsRow, message);
        for (int row = 0; row < 4; row++) {
            var line = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(6).height(20)
                    .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER));
            for (int column = 0; column < 3; column++) {
                if (row == 3 && column != 1) {
                    line.addChild(new UIElement().layout(l -> l.width(20).height(20)));
                } else {
                    int number = row == 3 ? 0 : row * 3 + column + 1;
                    var key = button(Component.literal(String.valueOf(number)), 20, 20, () -> digit(number));
                    key.setId("phone_pin_" + number);
                    key.textStyle(s -> s.fontSize(10).textColor(0xFFF3F6FA));
                    // 数字字形右侧留有 1 像素字距，行框底部留有 2 像素；补偿后让可见笔画居中。
                    float glyphScale = 10f / key.text.getFont().lineHeight;
                    key.text.transform(t -> t.translate(glyphScale / 2, glyphScale));
                    key.buttonStyle(s -> s.baseTexture(SDFRectTexture.of(0xFF383E4B).setRadius(10))
                            .hoverTexture(SDFRectTexture.of(0xFF586170).setRadius(10))
                            .pressedTexture(SDFRectTexture.of(0xFF788392).setRadius(10)));
                    line.addChild(key);
                }
            }
            addChild(line);
        }
        var cancel = button(Component.translatable("smartPhone.security.cancel"), 42, 12, this::cancel);
        cancel.setId("phone_passcode_cancel");
        cancel.layout(l -> l.marginTop(7));
        cancel.buttonStyle(s -> s.baseTexture(IGuiTexture.EMPTY).hoverTexture(new ColorRectTexture(0x334D596C))
                .pressedTexture(new ColorRectTexture(0x66535F70)));
        addChild(cancel);
        addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode >= GLFW.GLFW_KEY_0 && e.keyCode <= GLFW.GLFW_KEY_9) digit(e.keyCode - GLFW.GLFW_KEY_0);
            else if (e.keyCode >= GLFW.GLFW_KEY_KP_0 && e.keyCode <= GLFW.GLFW_KEY_KP_9) digit(e.keyCode - GLFW.GLFW_KEY_KP_0);
            else if (e.keyCode == GLFW.GLFW_KEY_BACKSPACE && !waiting && !digits.isEmpty()) {
                digits = digits.substring(0, digits.length() - 1);
                updateDots();
            } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) cancel();
            e.stopPropagation();
        });
        updateTitle();
        updateDots();
    }

    private void digit(int number) {
        if (waiting || digits.length() >= 6) return;
        digits += number;
        message.setText(Component.empty());
        updateDots();
        if (digits.length() != 6) return;
        String entered = digits;
        if (mode == Mode.UNLOCK) {
            waiting = true;
            RPCPacketDistributor.rpcToServer(C2SPayload.UNLOCK_PHONE, phone.getAccessToken(), entered);
        } else if (stage == 0) {
            currentPin = entered;
            if (mode == Mode.DISABLE) submit("");
            else { stage = 1; clearDigits(); updateTitle(); }
        } else if (stage == 1) {
            newPin = entered;
            stage = 2;
            clearDigits();
            updateTitle();
        } else if (!newPin.equals(entered)) {
            newPin = "";
            stage = 1;
            clearDigits();
            updateTitle();
            message.setText("smartPhone.security.mismatch");
        } else submit(newPin);
    }

    private void submit(String pin) {
        waiting = true;
        RPCPacketDistributor.rpcToServer(C2SPayload.CHANGE_PASSCODE, phone.getAccessToken(), currentPin, pin);
        currentPin = "";
        newPin = "";
    }

    public void failed(String error) {
        waiting = false;
        currentPin = "";
        newPin = "";
        stage = mode == Mode.SET ? 1 : 0;
        clearDigits();
        updateTitle();
        message.setText(error);
    }

    private void clearDigits() { digits = ""; updateDots(); }
    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].style(s -> s.backgroundTexture(SDFRectTexture.of(0).setRadius(2).setStroke(.5f).setBorderColor(0xFFDEE5ED)));
            if (i < digits.length()) dots[i].style(s -> s.backgroundTexture(SDFRectTexture.of(0xFFDEE5ED).setRadius(2)));
        }
    }
    private void updateTitle() {
        title.setText("smartPhone.security." + (mode == Mode.UNLOCK ? "enter" : stage == 0 ? "current" : stage == 1 ? "new" : "confirm"));
    }
    private void cancel() {
        if (waiting) return;
        digits = currentPin = newPin = "";
        phone.closePasscode();
    }
    private static Button button(Component text, float width, float height, Runnable action) {
        var button = new Button().setText(text);
        button.layout(l -> l.width(width).height(height).paddingAll(0).paddingBottom(0).flexShrink(0));
        button.text.layout(l -> l.widthPercent(100).heightPercent(100).marginAll(0));
        button.textStyle(s -> s.fontSize(5).textColor(0xFFF3F6FA).textShadow(false).adaptiveWidth(false).adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        button.setOnClick(e -> { if (e.button == 0) action.run(); });
        return button;
    }
}
