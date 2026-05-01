package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.math.Size;
import net.minecraft.client.Minecraft;

public final class PhoneUiScale {
    private static final float DEFAULT_SCREEN_WIDTH_RATIO = 0.8f;
    private static final float DEFAULT_SCREEN_HEIGHT_RATIO = 0.8f;

    private PhoneUiScale() {
    }

    public static Size defaultAutoSize(Size screenSize) {
        Minecraft minecraft = Minecraft.getInstance();
        double currentScale = currentGuiScale(minecraft);
        int autoScale = autoGuiScale(minecraft);
        float scaleRatio = (float) (currentScale / autoScale);
        return Size.of(
                Math.max(1, Math.round(screenSize.width * scaleRatio * DEFAULT_SCREEN_WIDTH_RATIO)),
                Math.max(1, Math.round(screenSize.height * scaleRatio * DEFAULT_SCREEN_HEIGHT_RATIO))
        );
    }

    public static float autoScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();
        return (float) (autoGuiScale(minecraft) / currentGuiScale(minecraft));
    }

    private static int autoGuiScale(Minecraft minecraft) {
        return Math.max(1, minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode()));
    }

    private static double currentGuiScale(Minecraft minecraft) {
        return Math.max(1.0D, minecraft.getWindow().getGuiScale());
    }
}
