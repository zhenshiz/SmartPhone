package com.smart.phone.ui.app;

import net.minecraft.network.chat.Component;

public record AppOpenResult(boolean allowed, Component reason) {
    public static AppOpenResult allow() {
        return new AppOpenResult(true, Component.empty());
    }

    public static AppOpenResult deny(String translationKey) {
        return new AppOpenResult(false, Component.translatable(translationKey));
    }
}