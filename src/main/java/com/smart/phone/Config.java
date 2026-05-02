package com.smart.phone;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Set;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //左边距 %
    public static ModConfigSpec.DoubleValue PHONE_MARGIN_LEFT;

    //上边框 %
    public static ModConfigSpec.DoubleValue PHONE_MARGIN_TOP;

    // 禁用的 app 注册名，逗号分隔，如 "smart_phone:camera,smart_phone:phone_call"
    public static ModConfigSpec.ConfigValue<String> DISABLED_APPS;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        PHONE_MARGIN_LEFT = CONFIG_BUILDER.defineInRange("phoneMarginLeft", 0f, -100, 100);
        PHONE_MARGIN_TOP = CONFIG_BUILDER.defineInRange("phoneMarginTop", 0f, -100, 100);
        CONFIG_BUILDER.pop();

        CONFIG_BUILDER.push("apps");
        DISABLED_APPS = CONFIG_BUILDER
                .comment("逗号分隔，支持 \"smart_phone:camera, smart_phone:phone_call\" 等格式")
                .define("disabledApps", "");
        CONFIG_BUILDER.pop();

        CONFIG_SPEC = CONFIG_BUILDER.build();
    }

    /**
     * 判断 app 是否被配置启用。disabledApps 中的 app 会被禁用。
     */
    public static boolean isAppEnabled(String appId) {
        if (appId == null || appId.isEmpty()) return true;
        String raw = DISABLED_APPS.get();
        if (raw == null || raw.isBlank()) return true;
        // 逗号分隔，支持 "smart_phone:camera, smart_phone:phone_call" 等格式
        for (String entry : raw.split(",")) {
            if (entry.trim().equals(appId)) return false;
        }
        return true;
    }
}
