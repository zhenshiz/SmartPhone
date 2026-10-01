package com.smart.phone;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec.IntValue EDITOR_GUI_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("editor");
        EDITOR_GUI_SCALE = builder
                .translation("smartPhone.editor.interfaceSize")
                .comment("手机数据编辑器的独立界面尺寸；0 为自动，不影响游戏 GUI 缩放。")
                .defineInRange("guiScale", 2, 0, Integer.MAX_VALUE);
        builder.pop();
        CONFIG_SPEC = builder.build();
    }

    private ClientConfig() {}
}
