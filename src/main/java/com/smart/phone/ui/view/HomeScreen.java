package com.smart.phone.ui.view;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.util.SmartPhoneClientUtil;
import com.smart.phone.util.UIElementUtil;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import java.util.Collections;
import java.util.List;

// 手机桌面
@Getter
public class HomeScreen extends UIElement {
    private final PhoneUI phoneUI;
    public final ScrollerView appScrollView = new  ScrollerView();
    public final UIElement backButton;
    //当前正在显示的APP
    public UIElement appUI;
    public IApp iApp;

    private boolean isDraggingAction = false;
    private long mouseDownTime = 0;

    public HomeScreen(PhoneUI phoneUI) {
        this.phoneUI = phoneUI;

        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).style(style -> {
            style.zIndex(1);
        }).addEventListener(UIEvents.TICK, event -> {
            event.target.getStyle().backgroundTexture(SpriteTexture.of(phoneUI.phoneInfo.getPhoneWallpaper()));
        });

        backButton = new UIElement().layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setWidthPercent(100);
            layout.setHeight(5);
            layout.bottom(0);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setHeight(1);
            layout.setWidthPercent(50);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ColorPattern.BLACK.color))).addEventListener(UIEvents.CLICK, event -> {
            if (this.appUI != null) {
                this.iApp.onClose(appUI);
                this.removeChild(appUI);
                this.appUI = null;
                this.iApp = null;

                appScrollView.viewContainer.setVisible(true);
            }
        }));

        appScrollView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.top(8);
            layout.setFlex(1);
        });
        appScrollView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setGap(YogaGutter.ALL, 3);
        });
        appScrollView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        appScrollView.scrollerStyle(style -> {
            style.verticalScrollDisplay(ScrollDisplay.NEVER);
            style.horizontalScrollDisplay(ScrollDisplay.NEVER);
        });
        reloadAppView();

        this.addChildren(appScrollView);
    }

    public void reloadAppView() {
        appScrollView.clearAllScrollViewChildren();
        List<IApp> installedApps = phoneUI.phoneInfo.getInstalledApps();
        for (IApp iApp : installedApps) {
            UIElement appIcon = new UIElement().layout(layout -> {
                layout.setFlexDirection(YogaFlexDirection.COLUMN);
                layout.setAlignItems(YogaAlign.CENTER);
                layout.setWidthPercent(30);
            });

            UIElement iconBackground = new UIElement().layout(layout -> {
                layout.setWidth(12).setHeight(12).setMargin(YogaEdge.BOTTOM, 2).setJustifyContent(YogaJustify.CENTER).setAlignItems(YogaAlign.CENTER);
            }).style(style -> style.backgroundTexture(iApp.getIcon()));

            iconBackground.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    mouseDownTime = System.currentTimeMillis();
                }
            });

            iconBackground.addEventListener(UIEvents.MOUSE_UP, event -> {
                mouseDownTime = 0;
                isDraggingAction = false;
            });

            iconBackground.addEventListener(UIEvents.MOUSE_MOVE, event -> {
                if (iconBackground.isMouseDown(0) && mouseDownTime != 0 && !isDraggingAction) {
                    if (System.currentTimeMillis() - mouseDownTime > 400) {
                        IGuiTexture dragTexture = createCombinedIcon(Sprites.RECT_RD_SOLID, iApp.getIcon(), 0.5f);

                        iconBackground.startDrag(iApp, dragTexture);
                        isDraggingAction = true;
                        mouseDownTime = 0;
                    }
                } else if (!iconBackground.isMouseDown(0)) {
                    mouseDownTime = 0;
                }
            });

            appIcon.addEventListener(UIEvents.DRAG_PERFORM, event -> {
                Object data = null;
                if (event.dragHandler != null) data = event.dragHandler.draggingObject;
                if (data == null) data = event.customData;

                if (data instanceof IApp sourceApp && sourceApp != iApp) {
                    int fromIndex = installedApps.indexOf(sourceApp);
                    int toIndex = installedApps.indexOf(iApp);

                    if (fromIndex != -1 && toIndex != -1) {
                        Collections.swap(installedApps, fromIndex, toIndex);
                        savePhoneData();
                        reloadAppView();
                    }
                } else {
                    //没有选择任何一个ui 取消拖拽标记即可
                    isDraggingAction = false;
                }
            });

            iconBackground.addEventListener(UIEvents.CLICK, event -> {
                if (event.button == 0) {
                    if (isDraggingAction) return;

                    if (!iApp.canOpen().allowed()) {
                        Toast.show(this, iApp.canOpen().reason(), 3f);
                        return;
                    }
                    UIElement appUI = iApp.createAppUI(this);
                    appUI.addChildren(backButton);
                    this.appUI = appUI;
                    this.iApp = iApp;
                    appScrollView.viewContainer.setVisible(false);
                    this.addChildren(appUI);
                    iApp.onOpen(appUI);
                } else if (event.button == 1) {
                    UIElement clickedElement = event.currentElement;
                    float posX = clickedElement.getPositionX();
                    float posY = clickedElement.getPositionY() + clickedElement.getSizeHeight();

                    TreeBuilder.Menu menu = TreeBuilder.Menu.start().leaf("smartPhone.ui.button.delete", () -> {
                        handleAppUninstall(iApp);
                    });
                    UIElementUtil.openMenu(posX, posY, menu, this);
                }
            });

            appIcon.addChildren(iconBackground,
                    new Label().setText(iApp.getDisplayName()).textStyle(textStyle -> textStyle.adaptiveHeight(true).adaptiveWidth(true).fontSize(5)));
            appScrollView.viewContainer.addChildren(appIcon);
        }
    }

    private void handleAppUninstall(IApp app) {
        if (app.isUninstall()) {
            phoneUI.phoneInfo.getInstalledApps().remove(app);
            savePhoneData();
            Toast.show(this, Component.translatable("smartPhone.ui.homeScreen.uninstall", app.getDisplayName().getString()), 2.0f);
            reloadAppView();
        } else {
            Toast.show(this, Component.translatable("smartPhone.ui.homeScreen.not.uninstall"), 2.0f);
        }
    }

    public void savePhoneData() {
        SmartPhoneClientUtil.setPhoneInfoByPlayer(this.phoneUI.phoneInfo);
    }

    public IGuiTexture createCombinedIcon(IGuiTexture background, IGuiTexture icon, float iconScale) {
        return new IGuiTexture() {
            @Override
            public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
                float iconW = width * iconScale;
                float iconH = height * iconScale;
                float iconX = x + (width - iconW) / 2;
                float iconY = y + (height - iconH) / 2;

                background.draw(graphics, mouseX, mouseY, iconX, iconY, iconW, iconH, partialTicks);
                icon.draw(graphics, mouseX, mouseY, iconX, iconY, iconW, iconH, partialTicks);
            }

            @Override
            public IGuiTexture copy() {
                return createCombinedIcon(background.copy(), icon.copy(), iconScale);
            }
        };
    }
}
