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
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.ui.app.CameraApp;
import com.smart.phone.ui.components.Toast;
import com.smart.phone.ui.view.HomeScreen;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Optional;

public class PhotoAlbumUI extends AppUI {
    private static final ZoneId PHOTO_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(PHOTO_ZONE);
    private static final int ROW_BACKGROUND = 0x22000000;
    private static final int TEXT_SECONDARY = 0xFFAAA3B6;
    private final AtomicBoolean importing = new AtomicBoolean();

    public PhotoAlbumUI(HomeScreen homeScreen) {
        super(homeScreen);
        showList();
    }

    private void applyListLayout() {
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingHorizontal(2);
            layout.paddingVertical(3);
            layout.gapAll(1);
        });
    }

    private void applyDetailLayout() {
        appScrollView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingHorizontal(0);
            layout.paddingVertical(4);
            layout.gapAll(3);
        });
    }

    private void showList() {
        applyListLayout();
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(createAlbumActions());
        List<PhonePhoto> photos = PhonePhotoAlbum.listPhotos();
        if (photos.isEmpty()) {
            appScrollView.viewContainer.addChildren(createLabel(Component.translatable("smartPhone.ui.app.photoAlbum.empty"), 5.5f, TEXT_SECONDARY, 14, Horizontal.CENTER));
            return;
        }
        appScrollView.viewContainer.addChildren(
                createLabel(Component.translatable("smartPhone.ui.app.photoAlbum.savedPhotos"), 5, ColorPattern.WHITE.color, 10, Horizontal.CENTER),
                createPhotoGrid(photos)
        );
    }

    private UIElement createPhotoGrid(List<PhonePhoto> photos) {
        UIElement grid = new UIElement().layout(layout -> {
            layout.widthPercent(98);
            layout.flexDirection(FlexDirection.ROW);
            layout.flexWrap(FlexWrap.WRAP);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingAll(2);
            layout.gapAll(3);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ROW_BACKGROUND)));
        photos.forEach(photo -> grid.addChildren(createPhotoTile(photo)));
        return grid;
    }

    private UIElement createPhotoTile(PhonePhoto photo) {
        UIElement tile = new UIElement().layout(layout -> {
            layout.width(32);
            layout.height(34);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });
        UIElement thumbnail = new UIElement().layout(layout -> {
            layout.width(28);
            layout.height(22);
        }).style(style -> style.backgroundTexture(photoTexture(photo)));
        tile.addChildren(
                thumbnail,
                createLabel(Component.literal(formatTileTime(photo.lastModifiedMillis())), 3.4f, TEXT_SECONDARY, 6, Horizontal.CENTER)
        );
        tile.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) openPhoto(photo);
        });
        return tile;
    }

    private void openPhoto(PhonePhoto photo) {
        applyDetailLayout();
        appScrollView.clearAllScrollViewChildren();
        appScrollView.viewContainer.addChildren(
                createBackRow(),
                createPreview(photo),
                createLabel(Component.literal(photo.fileName()), 4.5f, ColorPattern.WHITE.color, 8, Horizontal.CENTER),
                createLabel(Component.literal(formatTime(photo.lastModifiedMillis())), 4, TEXT_SECONDARY, 7, Horizontal.CENTER),
                createDetailButtons(photo)
        );
    }

    private UIElement createPreview(PhonePhoto photo) {
        // 16:9 横屏比例预览，匹配相机拍摄的照片比例
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(50);
        }).style(style -> style.backgroundTexture(photoTexture(photo)));
    }

    private UIElement createBackRow() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(13);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(ROW_BACKGROUND)));
        row.addChildren(createLabel(Component.translatable("smartPhone.ui.app.photoAlbum.back"), 4.5f, ColorPattern.WHITE.color, 10, Horizontal.CENTER));
        row.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) showList();
        });
        return row;
    }

    private UIElement createDetailButtons(PhonePhoto photo) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(3);
        });
        row.addChildren(
                createButton("smartPhone.ui.app.photoAlbum.camera", 36, ROW_BACKGROUND, () -> homeScreen.openApp(new CameraApp())),
                createButton("smartPhone.ui.app.photoAlbum.delete", 36, 0x553A2530, () -> deletePhoto(photo))
        );
        return row;
    }

    private Button createCameraButton() {
        return createButton("smartPhone.ui.app.photoAlbum.camera", 58, ROW_BACKGROUND, () -> homeScreen.openApp(new CameraApp()));
    }

    private UIElement createAlbumActions() {
        UIElement actions = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(3);
        });
        actions.addChildren(
                createButton("smartPhone.ui.app.photoAlbum.import", 28, ROW_BACKGROUND, this::importPhoto),
                createButton("smartPhone.ui.app.photoAlbum.camera", 28, ROW_BACKGROUND, () -> homeScreen.openApp(new CameraApp()))
        );
        return actions;
    }

    private void importPhoto() {
        if (!importing.compareAndSet(false, true)) return;

        Path albumDirectory = PhonePhotoAlbum.photoDirectory();
        Path initialDirectory = Files.isDirectory(albumDirectory)
                ? albumDirectory
                : minecraft.gameDirectory.toPath();
        String title = Component.translatable("smartPhone.ui.app.photoAlbum.importDialogTitle").getString();
        String filterDescription = Component.translatable("smartPhone.ui.app.photoAlbum.importDialogFilter").getString();

        CompletableFuture.supplyAsync(
                        () -> PhonePhotoAlbum.chooseImageForImport(initialDirectory, title, filterDescription),
                        Util.backgroundExecutor()
                )
                .thenApply(selected -> selected.map(PhonePhotoAlbum::importImage).orElse(null))
                .whenComplete((result, exception) -> minecraft.execute(() -> finishImport(result, exception)));
    }

    private void finishImport(PhonePhotoAlbum.ImportResult result, Throwable exception) {
        importing.set(false);
        if (homeScreen.appUI != this) return;

        if (exception != null || result == null) return;
        if (result.isSuccess()) {
            Toast.show(this, Component.translatable("smartPhone.ui.app.photoAlbum.imported"), 1.2f);
            showList();
            return;
        }

        Toast.show(this, Component.translatable(importFailureKey(result.failure())), 1.8f);
    }

    private String importFailureKey(PhonePhotoAlbum.ImportStatus status) {
        if (status == null) return "smartPhone.ui.app.photoAlbum.importFailed";
        return switch (status) {
            case UNSUPPORTED_FORMAT -> "smartPhone.ui.app.photoAlbum.importUnsupported";
            case TOO_LARGE -> "smartPhone.ui.app.photoAlbum.importTooLarge";
            case INVALID_IMAGE -> "smartPhone.ui.app.photoAlbum.importInvalid";
            case FAILED -> "smartPhone.ui.app.photoAlbum.importFailed";
        };
    }

    private void deletePhoto(PhonePhoto photo) {
        if (PhonePhotoAlbum.delete(photo)) {
            Toast.show(this, Component.translatable("smartPhone.ui.app.photoAlbum.deleted"), 1.2f);
        }
        showList();
    }

    private IGuiTexture photoTexture(PhonePhoto photo) {
        Optional<ResourceLocation> texture = PhonePhotoAlbum.textureFor(photo);
        return texture.<IGuiTexture>map(SpriteTexture::of).orElseGet(() -> new ColorRectTexture(0xFF312D3A));
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
            textStyle.fontSize(4.1f);
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

    private String formatTime(long millis) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private String formatTileTime(long millis) {
        return DateTimeFormatter.ofPattern("HH:mm").withZone(PHOTO_ZONE).format(Instant.ofEpochMilli(millis));
    }

    private String clip(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text == null ? "" : text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
