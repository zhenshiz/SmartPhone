package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.smart.phone.util.common.BeanUtil;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElementUtil {
    public static SearchComponentConfigurator<String> createStrArrSearchComponentConfigurator(String name, Set<String> strArr, Supplier<String> getter, Consumer<String> setter, float fontSize) {
        return new SearchComponentConfigurator<>(name,
                getter,
                setter,
                BeanUtil.getValueOrDefault(getter.get(), ""),
                false,
                (word, searchHandler) -> {
                    String lowerWord = word.toLowerCase();
                    for (var key : strArr) {
                        if (Thread.currentThread().isInterrupted()) return;
                        if (key.toLowerCase().contains(lowerWord)) {
                            ((IResultHandler<String>) searchHandler).acceptResult(key);
                        }
                    }
                },
                (value) -> BeanUtil.getValueOrDefault(value, ""),
                value -> new Label().setText(value).textStyle(textStyle -> textStyle.fontSize(fontSize).adaptiveHeight(true))
        );
    }

    public static void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder, @NotNull UIElement parent) {
        if (menuBuilder != null && !menuBuilder.isEmpty()) {
            openMenu(posX, posY, menuBuilder.build(), node -> {
                if (node == TreeBuilder.Menu.CROSS_LINE) {
                    return new UIElement().layout(layout -> {
                        layout.setHeight(1);
                        layout.setMargin(YogaEdge.HORIZONTAL, 3);
                    }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
                }
                return new UIElement().layout(layout -> {
                            layout.setHeight(8);
                            layout.setWidthPercent(100);
                            layout.setFlexDirection(YogaFlexDirection.ROW);
                            layout.setAlignItems(YogaAlign.CENTER);
                        }).addChild(new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL).fontSize(6))
                                .setText(node.getB()).layout(layout -> layout.setFlexGrow(1)).setOverflow(YogaOverflow.HIDDEN));
            }, parent).setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
        }
    }

    public static <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider, @NotNull UIElement parent) {
        Menu<T, C> menu = new Menu<>(menuNode, uiProvider);

        float relativeX = posX - parent.getContentX();
        float relativeY = posY - parent.getContentY();

        menu.layout((layout) -> {
            layout.setMinWidth(40);

            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setPosition(YogaEdge.LEFT, relativeX);
            layout.setPosition(YogaEdge.TOP, relativeY);
        });

        menu.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            float parentW = parent.getContentWidth();
            float parentH = parent.getContentHeight();

            float menuW = menu.getSizeWidth();
            float menuH = menu.getSizeHeight();
            float menuX = menu.getLayoutX();
            float menuY = menu.getLayoutY();

            boolean changed = false;
            float newX = menuX;
            float newY = menuY;

            if (menuY + menuH > parentH) {
                newY = Math.max(0, parentH - menuH);
                changed = true;
            }

            if (menuX + menuW > parentW) {
                newX = Math.max(0, parentW - menuW);
                changed = true;
            }

            if (newX < 0) { newX = 0; changed = true; }
            if (newY < 0) { newY = 0; changed = true; }

            if (changed) {
                float finalX = newX;
                float finalY = newY;
                menu.layout(l -> {
                    l.setPosition(YogaEdge.LEFT, finalX);
                    l.setPosition(YogaEdge.TOP, finalY);
                });
            }
        });

        parent.addChildren(menu);
        return menu;
    }
}
