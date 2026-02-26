package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import com.smart.phone.util.UIElementUtil;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

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
        searchComponent.layout(layout -> layout.widthPercent(100));
        appScrollView.viewContainer.layout(layout -> layout.gapAll(2));
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
            layout.widthPercent(100);
            layout.height(30);
            layout.flexDirection(FlexDirection.ROW);
            // 修正了原来的布局逻辑错误：将元素分布到左右两端，并在垂直方向居中
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        });

        // 左侧
        UIElement left = new UIElement().layout(layout -> layout
                .flexDirection(FlexDirection.ROW)
                .widthPercent(70)
                .heightPercent(100)
                .justifyContent(AlignContent.FLEX_START)
                .alignItems(AlignItems.CENTER)
                .gapAll(2)
        );

        UIElement iconBackground = new UIElement().layout(layout -> {
            layout.width(20)
                    .height(20)
                    .justifyContent(AlignContent.CENTER)
                    .alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(iApp.getIcon()));

        left.addChildren(iconBackground, new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_START);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(2);
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

        // 右侧
        UIElement right = new UIElement().layout(layout -> layout
                .widthPercent(30)
                .heightPercent(100)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
        );

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