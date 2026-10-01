package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.smart.phone.ClientConfig;
import net.minecraft.network.chat.Component;

/** 使用原生 GUI 坐标系，编辑器移除时恢复游戏窗口尺寸。 */
public final class PhoneEditorScreen extends ModularUIScreen {
    private final PhoneEditorUI editor;

    public PhoneEditorScreen(ModularUI ui, PhoneEditorUI editor) {
        super(ui, Component.translatable("smartPhone.editor.title"));
        this.editor = editor;
    }

    @Override
    public void init() {
        var window = minecraft.getWindow();
        window.setGuiScale(window.calculateScale(ClientConfig.EDITOR_GUI_SCALE.get(), minecraft.isEnforceUnicode()));
        width = window.getGuiScaledWidth();
        height = window.getGuiScaledHeight();
        super.init();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Screen 默认先处理 Esc；有弹窗时交给 ModularUI 内的弹窗处理。
        return editor.select("dialog").findAny().isEmpty();
    }

    @Override
    public void onClose() {
        editor.requestClose();
    }

    @Override
    public void removed() {
        super.removed();
        // 不写 options.guiScale；Esc、命令换屏及断开连接都经过 removed。
        var window = minecraft.getWindow();
        window.setGuiScale(window.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode()));
    }
}
