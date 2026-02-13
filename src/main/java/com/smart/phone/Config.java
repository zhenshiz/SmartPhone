package com.smart.phone;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //左边距 %
    public static ModConfigSpec.DoubleValue PHONE_MARGIN_LEFT;

    //上边框 %
    public static ModConfigSpec.DoubleValue PHONE_MARGIN_TOP;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        PHONE_MARGIN_LEFT = CONFIG_BUILDER.defineInRange("phoneMarginLeft", 0f, -100, 100);
        PHONE_MARGIN_TOP = CONFIG_BUILDER.defineInRange("phoneMarginTop", 0f, -100, 100);
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
