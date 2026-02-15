package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import com.smart.phone.util.UIElementUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaJustify;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AppStoreUI extends AppUI {
    @Getter
    @Setter
    private String search = "";
    public final SearchComponent<String> searchComponent;

    public AppStoreUI(HomeScreen homeScreen) {
        super(homeScreen);

        Set<IApp> iApps = SmartPhoneRegistries.filterApp(IApp::isAppStoreInstall);
        Set<String> names = iApps.stream().map(iApp -> iApp.getDisplayName().getString()).collect(Collectors.toSet());
        searchComponent = UIElementUtil.createStrArrSearchComponentConfigurator("", names, this::getSearch, this::setSearch, 6).searchComponent;
        searchComponent.textField.textFieldStyle(style -> style.fontSize(6).placeholder(Component.translatable("smartPhone.ui.app.appStore.search")));
        searchComponent.layout(layout -> layout.setWidthPercent(100));
        appScrollView.viewContainer.layout(layout -> layout.setGap(YogaGutter.ALL, 2));
        reloadAppScrollView(iApps);
    }

    public void reloadAppScrollView(Set<IApp> iApps) {
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(searchComponent);
        for (IApp iApp : iApps) {
            appScrollView.viewContainer.addChildren(createAppCard(iApp));
        }
    }

    public UIElement createAppCard(IApp iApp) {
        UIElement card = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(30);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.SPACE_BETWEEN);
        });
        //左侧
        UIElement left = new UIElement().layout(layout -> layout.setFlexDirection(YogaFlexDirection.ROW).setWidthPercent(70).setHeightPercent(100).setJustifyContent(YogaJustify.FLEX_START).setAlignItems(YogaAlign.CENTER).setGap(YogaGutter.ALL, 2));
        UIElement icon = new UIElement().layout(layout -> layout.setWidthPercent(80).setHeightPercent(80)).style(style -> style.backgroundTexture(iApp.getIcon()));
        UIElement iconBackground = new UIElement().layout(layout -> {
            layout.setWidth(20).setHeight(20).setJustifyContent(YogaJustify.CENTER).setAlignItems(YogaAlign.CENTER);
        }).style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID)).addChildren(icon);
        left.addChildren(iconBackground, new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setAlignItems(YogaAlign.FLEX_START);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(new Label().setText(iApp.getDisplayName()).textStyle(textStyle -> {
            textStyle.adaptiveWidth(true);
            textStyle.adaptiveHeight(true);
            textStyle.fontSize(6);
            textStyle.textColor(ColorPattern.WHITE.color);
        }), new Label().setText(iApp.getDescription()).textStyle(textStyle -> {
            textStyle.adaptiveWidth(true);
            textStyle.adaptiveHeight(true);
            textStyle.fontSize(4);
            textStyle.textColor(ColorPattern.T_WHITE.color);
        })));

        //右侧
        UIElement right = new UIElement().layout(layout -> layout.setWidthPercent(30).setHeightPercent(100).setJustifyContent(YogaJustify.CENTER).setAlignItems(YogaAlign.CENTER));

        Button button = new Button().textStyle(textStyle -> {
            textStyle.fontSize(5);
        });
        updateButtonState(button, iApp);
        button.addEventListener(UIEvents.CLICK, event -> {
            boolean isInstalled = checkAppInstalled(iApp);
            List<IApp> installedApps = this.homeScreen.getPhoneUI().phoneInfo.getInstalledApps();

            if (isInstalled) {
                if (!iApp.isUninstall()) {
                    Toast.show(this, Component.translatable("smartPhone.toast.app.uninstall"), 2.0f);
                    return;
                }
                installedApps.removeIf(app -> app.name().equals(iApp.name()));
            } else {
                installedApps.add(iApp);
            }

            updateButtonState(button, iApp);

            SmartPhoneClientUtil.setPhoneInfoByPlayer(this.homeScreen.getPhoneUI().phoneInfo);
            this.homeScreen.reloadAppView();
        });
        right.addChildren(button);

        card.addChildren(left, right);

        return card;
    }

    private boolean checkAppInstalled(IApp targetApp) {
        for (IApp installedApp : this.homeScreen.getPhoneUI().phoneInfo.getInstalledApps()) {
            if (installedApp.name().equals(targetApp.name())) {
                return true;
            }
        }
        return false;
    }

    private void updateButtonState(Button button, IApp app) {
        boolean isInstalled = checkAppInstalled(app);
        if (isInstalled) {
            button.setText("smartPhone.ui.app.appStore.uninstalled");
        } else {
            button.setText("smartPhone.ui.app.appStore.installed");
        }
    }
}
