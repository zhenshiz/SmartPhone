package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 手机、应用和编辑器使用同一套 OreUI 控件与面板。 */
public final class PhoneTheme {
    private PhoneTheme() {}

    public static UI create(UIElement root, UI.DynamicSizeProvider size) {
        return UI.of(root, List.of(
                StylesheetManager.INSTANCE.getStylesheet(ResourceLocation.fromNamespaceAndPath("ldlib2", "lss/ore.lss")),
                StylesheetManager.INSTANCE.getStylesheet(ResourceLocation.fromNamespaceAndPath("smart_phone", "lss/phone.lss"))), size);
    }
}
