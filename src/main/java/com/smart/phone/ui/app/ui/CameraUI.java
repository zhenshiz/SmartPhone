package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.smart.phone.client.camera.PhoneCameraClient;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.ui.app.PhotoAlbumApp;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.view.HomeScreen;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class CameraUI extends AppUI {
    private static final float[] ZOOM_LEVELS = {1.0f, 1.5f, 2.0f, 3.0f};
    private static final int PANEL_BACKGROUND = 0x22000000;
    private static final int VIEWFINDER_BACKGROUND = 0xCC11141A;
    private static final int TEXT_SECONDARY = 0xFFAAA3B6;

    private int zoomIndex = 0;
    private final Label zoomLabel = createLabel(Component.empty(), 5, ColorPattern.WHITE.color, 9, Horizontal.CENTER);

    public CameraUI(HomeScreen homeScreen) {
        super(homeScreen);
        minecraft.execute(() -> PhoneCameraClient.openPreview(homeScreen.getPhoneUI().phoneInfo));
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingHorizontal(3);
            layout.paddingVertical(4);
            layout.gapAll(3);
        });
        refreshZoomLabel();
        appScrollView.viewContainer.addChildren(
                createViewfinder(),
                createZoomControls(),
                createCaptureButton(),
                createAlbumButton(),
                createLatestPhoto()
        );
    }

    private UIElement createViewfinder() {
        UIElement viewfinder = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(70);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(VIEWFINDER_BACKGROUND)));

        UIElement crosshairRow = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(22);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        crosshairRow.addChildren(createLabel(Component.literal("+"), 12, ColorPattern.WHITE.color, 22, Horizontal.CENTER));
        viewfinder.addChildren(
                createLabel(Component.translatable("smartPhone.ui.app.camera.preview"), 4, TEXT_SECONDARY, 8, Horizontal.CENTER),
                crosshairRow,
                zoomLabel
        );
        return viewfinder;
    }

    private UIElement createZoomControls() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(3);
        });
        row.addChildren(
                createButton("smartPhone.ui.app.camera.zoomOut", 22, PANEL_BACKGROUND, () -> setZoomIndex(zoomIndex - 1)),
                createFlexText(Component.translatable("smartPhone.ui.app.camera.zoom"), 5, TEXT_SECONDARY, 12, Horizontal.CENTER),
                createButton("smartPhone.ui.app.camera.zoomIn", 22, PANEL_BACKGROUND, () -> setZoomIndex(zoomIndex + 1))
        );
        return row;
    }

    private Button createCaptureButton() {
        return createButton("smartPhone.ui.app.camera.capture", 62, 0x553D5D7A, () -> {
            if (!PhoneCameraClient.openPreview(homeScreen.getPhoneUI().phoneInfo)) {
                Toast.show(this, Component.translatable("smartPhone.ui.app.camera.unavailable"), 1.4f);
            }
        });
    }

    private Button createAlbumButton() {
        return createButton("smartPhone.ui.app.camera.album", 62, PANEL_BACKGROUND, () -> homeScreen.openApp(new PhotoAlbumApp()));
    }

    private UIElement createLatestPhoto() {
        Optional<PhonePhoto> latestPhoto = PhonePhotoAlbum.latestPhoto();
        if (latestPhoto.isEmpty()) {
            return createLabel(Component.translatable("smartPhone.ui.app.camera.noPhotos"), 4.5f, TEXT_SECONDARY, 10, Horizontal.CENTER);
        }

        PhonePhoto photo = latestPhoto.get();
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(96);
            layout.height(24);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(3);
            layout.gapAll(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(PANEL_BACKGROUND)));

        UIElement thumbnail = new UIElement().layout(layout -> {
            layout.width(24);
            layout.height(16);
        }).style(style -> style.backgroundTexture(photoTexture(photo)));

        UIElement textColumn = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
        });
        textColumn.addChildren(
                createLabel(Component.translatable("smartPhone.ui.app.camera.latest"), 4, TEXT_SECONDARY, 7, Horizontal.LEFT),
                createLabel(Component.literal(clip(photo.fileName(), 18)), 4.5f, ColorPattern.WHITE.color, 8, Horizontal.LEFT)
        );
        row.addChildren(thumbnail, textColumn);
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) homeScreen.openApp(new PhotoAlbumApp());
        });
        return row;
    }

    private IGuiTexture photoTexture(PhonePhoto photo) {
        Optional<ResourceLocation> texture = PhonePhotoAlbum.textureFor(photo);
        return texture.<IGuiTexture>map(SpriteTexture::of).orElseGet(() -> new ColorRectTexture(0xFF312D3A));
    }

    private void setZoomIndex(int index) {
        zoomIndex = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, index));
        refreshZoomLabel();
    }

    private float currentZoom() {
        return ZOOM_LEVELS[zoomIndex];
    }

    private void refreshZoomLabel() {
        zoomLabel.setText(Component.literal("%.1fx".formatted(currentZoom())));
    }

    private Button createButton(String key, float width, int backgroundColor, Runnable onClick) {
        Button button = new Button();
        button.layout(layout -> {
            layout.width(width);
            layout.height(13);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(backgroundColor)));
        button.text.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.marginHorizontal(0);
        });
        button.textStyle(textStyle -> {
            textStyle.fontSize(4.2f);
            textStyle.textColor(ColorPattern.WHITE.color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        button.text.setOverflowVisible(false);
        button.setText(key);
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) onClick.run();
        });
        return button;
    }

    private Label createFlexText(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = createLabel(component, fontSize, color, height, horizontal);
        label.layout(layout -> {
            layout.flex(1);
            layout.width(0);
            layout.height(height);
        });
        return label;
    }

    private Label createLabel(Component component, float fontSize, int color, float height, Horizontal horizontal) {
        Label label = new Label();
        label.setText(component);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        label.textStyle(textStyle -> {
            textStyle.fontSize(fontSize);
            textStyle.textColor(color);
            textStyle.adaptiveWidth(false);
            textStyle.adaptiveHeight(false);
            textStyle.textWrap(TextWrap.HIDE);
            textStyle.textAlignHorizontal(horizontal);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        label.setOverflowVisible(false);
        return label;
    }

    private String clip(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text == null ? "" : text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
