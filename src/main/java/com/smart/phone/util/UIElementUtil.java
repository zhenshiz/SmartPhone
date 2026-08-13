package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.smart.phone.ui.app.ui.game.Direction;
import com.viscript_lib.util.BeanUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
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
                        layout.height(1);
                        layout.marginHorizontal(3);
                    }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
                }
                return new UIElement().layout(layout -> {
                    layout.height(8);
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL).fontSize(6))
                        .setText(node.getB()).layout(layout -> layout.flexGrow(1)).setOverflowVisible(false));
            }, parent).setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
        }
    }

    public static UIElement createControlButtons(Consumer<Direction> onDirectionClick) {
        UIElement controls = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(80);
                    layout.height(30);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.CENTER);
                });

        // 上按钮
        UIElement topButton = new UIElement().layout(layout -> layout.justifyContent(AlignContent.CENTER).flexDirection(FlexDirection.ROW).widthPercent(100).heightPercent(50));
        Button upButton = createDirectionButton("W", Direction.UP, onDirectionClick);
        topButton.addChildren(upButton);

        // 下按钮
        UIElement bottomButton = new UIElement().layout(layout -> layout.justifyContent(AlignContent.CENTER).flexDirection(FlexDirection.ROW).widthPercent(100).heightPercent(50));
        Button leftButton = createDirectionButton("A", Direction.LEFT, onDirectionClick);
        Button downButton = createDirectionButton("S", Direction.DOWN, onDirectionClick);
        Button rightButton = createDirectionButton("D", Direction.RIGHT, onDirectionClick);
        bottomButton.addChildren(leftButton, downButton, rightButton);

        controls.addChildren(topButton, bottomButton);

        return controls;
    }

    private static Button createDirectionButton(String text, Direction dir, Consumer<Direction> onDirectionClick) {
        Button button = new Button();
        button.setText(text);
        button.textStyle(style -> style.fontSize(6));
        button.layout(layout -> {
            layout.widthPercent(30);
            layout.heightPercent(100);
        });
        button.style(style -> style.backgroundTexture(new ColorRectTexture(ColorPattern.T_GRAY.color)));
        button.addEventListener(UIEvents.CLICK, event -> onDirectionClick.accept(dir));
        return button;
    }

    private static <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider, @NotNull UIElement parent) {
        Menu<T, C> menu = new Menu<>(menuNode, uiProvider);

        float relativeX = posX - parent.getContentX();
        float relativeY = posY - parent.getContentY();

        menu.layout((layout) -> {
            layout.minWidth(40);

            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(relativeX);
            layout.top(relativeY);
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

            if (newX < 0) {
                newX = 0;
                changed = true;
            }
            if (newY < 0) {
                newY = 0;
                changed = true;
            }

            if (changed) {
                float finalX = newX;
                float finalY = newY;
                menu.layout(l -> {
                    l.left(finalX);
                    l.top(finalY);
                });
            }
        });

        parent.addChildren(menu);
        return menu;
    }
}