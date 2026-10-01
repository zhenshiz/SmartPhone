package com.smart.phone.ui.editor;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.client.camera.PhonePhoto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** 在原图像素坐标中保留笔画，预览与保存共用同一份图像。 */
public final class PhotoDoodleCanvas extends UIElement implements AutoCloseable {
    private record Point(float x, float y) {}
    private record Stroke(int color, float radius, List<Point> points) {}
    private final NativeImage original;
    private final DynamicTexture texture;
    private final ResourceLocation location;
    private final SpriteTexture sprite;
    private final List<Stroke> strokes = new ArrayList<>();
    private final Runnable changed;
    private Stroke activeStroke;
    private Point previous;
    private int color = 0xFFF05B50;
    private float brushSize = 2;
    private boolean needsUpload;
    private boolean closed;

    public PhotoDoodleCanvas(PhonePhoto photo, Runnable changed) throws IOException {
        this.changed = changed;
        try (var input = Files.newInputStream(photo.path())) {
            original = NativeImage.read(input);
        }
        NativeImage working = new NativeImage(original.getWidth(), original.getHeight(), false);
        working.copyFrom(original);
        texture = new DynamicTexture(working);
        location = Minecraft.getInstance().getTextureManager().register("smart_phone_photo_edit", texture);
        sprite = SpriteTexture.of(location);
        setId("photo_edit_canvas");
        addClass("preview_bg");
        layout(l -> l.widthPercent(100).flex(1).minHeight(0));
    }

    public void setColor(int argb) {
        color = argb;
    }

    public void setBrushSize(float size) {
        brushSize = size;
    }

    private float imageScale() {
        return Math.min(getContentWidth() / original.getWidth(), getContentHeight() / original.getHeight());
    }

    private float imageX() {
        return getContentX() + (getContentWidth() - original.getWidth() * imageScale()) / 2;
    }

    private float imageY() {
        return getContentY() + (getContentHeight() - original.getHeight() * imageScale()) / 2;
    }

    private Point imagePoint(double screenX, double screenY) {
        float scale = imageScale();
        if (scale <= 0) return null;
        var local = getLocalMouse((float) screenX, (float) screenY);
        float x = (local.x - imageX()) / scale;
        float y = (local.y - imageY()) / scale;
        return x >= 0 && y >= 0 && x < original.getWidth() && y < original.getHeight() ? new Point(x, y) : null;
    }

    public boolean begin(double x, double y) {
        Point point = imagePoint(x, y);
        if (point == null || closed) return false;
        // NativeImage 使用 ABGR，色板和 UI 使用 ARGB。
        int abgr = (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >>> 16) & 0xFF);
        activeStroke = new Stroke(abgr, Math.max(.75f, brushSize / imageScale() / 2), new ArrayList<>());
        strokes.add(activeStroke);
        previous = null;
        append(point);
        changed.run();
        return true;
    }

    public boolean drag(double x, double y) {
        if (activeStroke == null) return false;
        append(imagePoint(x, y));
        return true;
    }

    private void append(Point point) {
        if (point == null) {
            if (previous != null) activeStroke.points.add(null);
            previous = null;
            return;
        }
        if (point.equals(previous)) return;
        activeStroke.points.add(point);
        paint(previous == null ? point : previous, point, activeStroke);
        previous = point;
        needsUpload = true;
    }

    public boolean end() {
        boolean wasDrawing = activeStroke != null;
        activeStroke = null;
        previous = null;
        return wasDrawing;
    }

    public boolean isDirty() {
        return !strokes.isEmpty();
    }

    public void undo() {
        end();
        if (strokes.isEmpty()) return;
        strokes.removeLast();
        texture.getPixels().copyFrom(original);
        for (Stroke stroke : strokes) {
            Point last = null;
            for (Point point : stroke.points) {
                if (point != null) paint(last == null ? point : last, point, stroke);
                last = point;
            }
        }
        needsUpload = true;
        changed.run();
    }

    private void paint(Point from, Point to, Stroke stroke) {
        NativeImage image = texture.getPixels();
        float radius = stroke.radius;
        int left = Math.max(0, (int) Math.floor(Math.min(from.x, to.x) - radius));
        int top = Math.max(0, (int) Math.floor(Math.min(from.y, to.y) - radius));
        int right = Math.min(image.getWidth() - 1, (int) Math.ceil(Math.max(from.x, to.x) + radius));
        int bottom = Math.min(image.getHeight() - 1, (int) Math.ceil(Math.max(from.y, to.y) + radius));
        float dx = to.x - from.x, dy = to.y - from.y;
        float lengthSquared = dx * dx + dy * dy;
        for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
            float t = lengthSquared == 0 ? 0 : Math.clamp(((x + .5f - from.x) * dx + (y + .5f - from.y) * dy) / lengthSquared, 0, 1);
            float distanceX = x + .5f - from.x - t * dx;
            float distanceY = y + .5f - from.y - t * dy;
            if (distanceX * distanceX + distanceY * distanceY <= radius * radius) image.setPixelRGBA(x, y, stroke.color);
        }
    }

    public NativeImage copyImage() {
        NativeImage copy = new NativeImage(original.getWidth(), original.getHeight(), false);
        copy.copyFrom(texture.getPixels());
        return copy;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (closed || imageScale() <= 0) return;
        if (needsUpload) {
            texture.upload();
            needsUpload = false;
        }
        context.drawTexture(sprite, imageX(), imageY(), original.getWidth() * imageScale(), original.getHeight() * imageScale());
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        original.close();
        // TextureManager 释放纹理时一并关闭 working NativeImage。
        Minecraft.getInstance().getTextureManager().release(location);
        strokes.clear();
    }
}
