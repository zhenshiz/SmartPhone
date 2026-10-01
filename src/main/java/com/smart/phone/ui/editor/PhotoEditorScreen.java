package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.smart.phone.SmartPhone;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.ui.PhoneTheme;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** 照片涂鸦工作区；取消返回原相册，保存创建新照片。 */
public final class PhotoEditorScreen extends ModularUIScreen {
    private static final String KEY = "smartPhone.ui.app.photoAlbum.edit.";
    private final Editor editor;
    private final Screen parent;
    private final Consumer<PhonePhoto> onSaved;

    private PhotoEditorScreen(Editor editor, Screen parent, Consumer<PhonePhoto> onSaved) {
        super(new ModularUI(PhoneTheme.create(editor, Editor::autoSize)), Component.translatable(KEY + "title"));
        this.editor = editor;
        this.parent = parent;
        this.onSaved = onSaved;
        editor.screen = this;
    }

    public static void open(PhonePhoto photo, Consumer<PhonePhoto> onSaved) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        Editor editor = new Editor(photo);
        minecraft.setScreen(new PhotoEditorScreen(editor, minecraft.screen, onSaved));
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && !editor.saving && editor.canvas.begin(x, y)) return true;
        modularUI.refreshHoveredElementAtScreen((float) x, (float) y);
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && editor.canvas.drag(x, y)) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && editor.canvas.drag(x, y)) {
            editor.canvas.end();
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_Z && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0 && !editor.saving) {
            editor.canvas.undo();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!editor.saving) minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        editor.canvas.close();
        super.removed();
    }

    private static final class Editor extends UIElement {
        private final PhonePhoto photo;
        private final PhotoDoodleCanvas canvas;
        private final Button save = new Button();
        private final Button undo = new Button();
        private final Button cancel = new Button();
        private final Label status = new Label();
        private final List<Button> colors = new ArrayList<>();
        private final List<Button> sizes = new ArrayList<>();
        private PhotoEditorScreen screen;
        private boolean saving;

        private Editor(PhonePhoto photo) throws IOException {
            this.photo = photo;
            setId("photo_editor");
            layout(l -> l.widthPercent(100).heightPercent(100).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
            UIElement panel = new UIElement().addClass("panel_bg").layout(l -> l.widthPercent(94).heightPercent(94).paddingAll(6).gapAll(4));
            Label title = new Label();
            title.setText(KEY + "title");
            title.layout(l -> l.height(16).flexShrink(0));
            canvas = new PhotoDoodleCanvas(photo, this::refreshButtons);
            UIElement toolbar = row(24);
            String[] names = {"red", "yellow", "green", "cyan", "blue", "white", "black"};
            int[] palette = {0xFFF05B50, 0xFFFFD35A, 0xFF55B66D, 0xFF4CC6D7, 0xFF578AF2, 0xFFFFFFFF, 0xFF202931};
            for (int i = 0; i < names.length; i++) {
                int selected = i;
                Button colorButton = new Button();
                colorButton.setId("photo_edit_color_" + names[i]);
                colorButton.layout(l -> l.width(20).height(20).flexShrink(0).paddingAll(4));
                colorButton.style(s -> s.tooltips(KEY + "color." + names[selected]));
                colorButton.removeChild(colorButton.text);
                colorButton.addChild(new UIElement().layout(l -> l.widthPercent(100).heightPercent(100))
                        .style(s -> s.backgroundTexture(new ColorRectTexture(palette[selected]))));
                colorButton.setOnClick(event -> {
                    canvas.setColor(palette[selected]);
                    select(colors, selected);
                });
                colors.add(colorButton);
                toolbar.addChild(colorButton);
            }
            select(colors, 0);
            toolbar.addChild(new UIElement().layout(l -> l.flex(1)));
            Label brushLabel = new Label();
            brushLabel.setText(KEY + "brush");
            brushLabel.textStyle(s -> s.adaptiveWidth(true));
            brushLabel.layout(l -> l.flexShrink(0));
            toolbar.addChild(brushLabel);
            for (int i = 0; i < 3; i++) {
                int selected = i;
                Button size = new Button();
                size.setId("photo_edit_size_" + i);
                size.setText(KEY + "size." + i);
                size.layout(l -> l.width(30).height(20).flexShrink(0));
                size.setOnClick(event -> {
                    canvas.setBrushSize(new float[]{1, 2, 4}[selected]);
                    select(sizes, selected);
                });
                sizes.add(size);
                toolbar.addChild(size);
            }
            select(sizes, 1);
            UIElement actions = row(24);
            status.setId("photo_edit_status");
            status.setText(KEY + "hint");
            status.layout(l -> l.flex(1).minWidth(0));
            status.textStyle(s -> s.fontSize(8).adaptiveWidth(false));
            configureButton(save, "save", 76, this::save);
            configureButton(undo, "undo", 48, canvas::undo);
            configureButton(cancel, "cancel", 48, () -> screen.onClose());
            actions.addChildren(status, save, undo, cancel);
            panel.addChildren(title, canvas, toolbar, actions);
            addChild(panel);
            refreshButtons();
        }

        private static UIElement row(float height) {
            return new UIElement().layout(l -> l.widthPercent(100).height(height).flexShrink(0)
                    .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(4));
        }

        private static void configureButton(Button button, String name, float width, Runnable action) {
            button.setId("photo_edit_" + name);
            button.setText(KEY + name);
            button.layout(l -> l.width(width).height(22).flexShrink(0));
            button.addEventListener(UIEvents.CLICK, event -> {
                if (event.button == 0 && button.isActive()) action.run();
            });
        }

        private static void select(List<Button> buttons, int selected) {
            for (int i = 0; i < buttons.size(); i++) {
                if (i == selected) buttons.get(i).addClass("photo_edit_selected");
                else buttons.get(i).removeClass("photo_edit_selected");
            }
        }

        private void refreshButtons() {
            save.setActive(!saving && canvas.isDirty());
            undo.setActive(!saving && canvas.isDirty());
            cancel.setActive(!saving);
            colors.forEach(button -> button.setActive(!saving));
            sizes.forEach(button -> button.setActive(!saving));
        }

        private void save() {
            if (saving || !canvas.isDirty()) return;
            canvas.end();
            var image = canvas.copyImage();
            saving = true;
            status.setText(KEY + "saving");
            refreshButtons();
            CompletableFuture.supplyAsync(() -> {
                try (image) {
                    return PhonePhotoAlbum.saveEditedCopy(photo, image);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }, Util.backgroundExecutor()).whenComplete((saved, error) -> Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().screen != screen) return;
                saving = false;
                if (error != null) {
                    SmartPhone.LOGGER.warn("Failed to save edited phone photo {}", photo.path(), error);
                    status.setText(KEY + "saveFailed");
                    refreshButtons();
                } else {
                    Minecraft.getInstance().setScreen(screen.parent);
                    screen.onSaved.accept(saved);
                }
            }));
        }

        private static float scale() {
            var mc = Minecraft.getInstance();
            return Math.min(2, mc.getWindow().calculateScale(0, mc.isEnforceUnicode())) / (float) mc.getWindow().getGuiScale();
        }

        private static Size autoSize(Size screenSize) {
            return Size.of(Math.round(screenSize.width / scale()), Math.round(screenSize.height / scale()));
        }

        @Override
        public void initScreen(int width, int height) {
            canvas.end();
            super.initScreen(width, height);
            transform(t -> t.pivot(.5f, .5f).scale(scale()));
        }
    }
}
