package com.smart.phone.client.camera;

import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.ui.app.PhotoAlbumApp;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public class PhoneCameraClient {
    private static final float[] ZOOM_LEVELS = {1.0f, 1.5f, 2.0f, 3.0f};
    private static final int CAPTURE_DELAY_FRAMES = 2;
    private static final int TEXT = 0xFFEAE7EE;
    private static final int TEXT_MUTED = 0xAAEAE7EE;
    private static final int LINE = 0xAAECEAF2;
    private static final int ACCENT = 0xCCF0D36E;

    private static CameraSession session;
    private static CaptureRequest pendingCapture;
    private static PhoneInfo queuedPhoneInfo;
    private static int queuedPhoneOpenTicks;

    public static boolean openPreview(PhoneInfo phoneInfo) {
        return openPreview(phoneInfo, 0);
    }

    public static boolean openPreview(PhoneInfo phoneInfo, int zoomIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (phoneInfo == null || minecraft.player == null || minecraft.level == null) return false;

        if (session == null) {
            session = new CameraSession(
                    phoneInfo,
                    clampZoomIndex(zoomIndex),
                    minecraft.options.fov().get(),
                    minecraft.options.hideGui,
                    minecraft.options.getCameraType()
            );
        } else {
            session.phoneInfo = phoneInfo;
            session.zoomIndex = clampZoomIndex(zoomIndex);
        }

        pendingCapture = null;
        minecraft.setScreen(null);
        minecraft.mouseHandler.grabMouse();
        applyCameraOptions(session);
        return true;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (queuedPhoneInfo != null && --queuedPhoneOpenTicks <= 0) {
            PhoneInfo phoneInfo = queuedPhoneInfo;
            queuedPhoneInfo = null;
            SmartPhoneClientUtil.openUnlockedPhone(phoneInfo);
        }

        CameraSession active = session;
        if (active == null) return;
        if (minecraft.player == null || minecraft.level == null) {
            closeToGame();
            return;
        }
        if (minecraft.screen != null) {
            closeToGame();
            return;
        }

        active.tickStatus();
        applyCameraOptions(active);
    }

    public static boolean renderCameraOverlay(GuiGraphics graphics) {
        CameraSession active = session;
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.player == null || minecraft.level == null || minecraft.screen != null) return false;

        updateLayout(active, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        if (!isCaptureFrame()) {
            drawOverlay(graphics, minecraft.font, active);
        }
        return true;
    }

    public static void handleMouseButton(InputEvent.MouseButton.Pre event) {
        if (!canHandleWorldInput() || event.getAction() != GLFW.GLFW_PRESS) return;
        if (pendingCapture != null) {
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if ((event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0) {
                openAlbumFromCamera();
            } else {
                requestCaptureFromCameraMode();
            }
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            openAlbumFromCamera();
            event.setCanceled(true);
        }
    }

    public static void handleMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!canHandleWorldInput()) return;
        if (pendingCapture == null) {
            if (event.getScrollDeltaY() > 0) {
                setZoomIndex(session.zoomIndex + 1);
            } else if (event.getScrollDeltaY() < 0) {
                setZoomIndex(session.zoomIndex - 1);
            }
        }
        event.setCanceled(true);
    }

    public static void handleKey(InputEvent.Key event) {
        if (!canHandleWorldInput() || event.getAction() != GLFW.GLFW_PRESS || pendingCapture != null) return;
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            closeToPhone();
        } else if (event.getKey() == GLFW.GLFW_KEY_C) {
            requestCaptureFromCameraMode();
        } else if (event.getKey() == GLFW.GLFW_KEY_G) {
            openAlbumFromCamera();
        }
    }

    public static void handleInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isCameraActive()) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public static boolean shouldHideFirstPersonHand() {
        Minecraft minecraft = Minecraft.getInstance();
        return session != null && minecraft.screen == null;
    }

    public static void captureOnRenderFrame() {
        CaptureRequest request = pendingCapture;
        if (request == null) return;
        if (request.framesUntilCapture-- > 0) return;

        pendingCapture = null;
        Minecraft minecraft = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            PhonePhoto photo = saveCapturedPhoto(image, request);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.4f, 0.7f));
            CameraSession active = session;
            if (active != null) {
                active.showStatus(Component.translatable("smartPhone.ui.app.camera.capturedShort"));
            } else if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("smartPhone.ui.app.camera.captured", photo.fileName()), true);
            }
        } catch (Exception exception) {
            CameraSession active = session;
            if (active != null) {
                active.showStatus(Component.translatable("smartPhone.ui.app.camera.captureFailed"));
            } else if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("smartPhone.ui.app.camera.captureFailed"), true);
            }
        } finally {
            CameraSession active = session;
            if (active != null) {
                applyCameraOptions(active);
            }
        }
    }

    private static boolean requestCaptureFromCameraMode() {
        CameraSession active = session;
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || pendingCapture != null || minecraft.player == null || minecraft.level == null) return false;

        updateLayout(active, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        Layout layout = active.layout;
        pendingCapture = new CaptureRequest(
                new CaptureViewport(layout.viewport.x, layout.viewport.y, layout.viewport.width, layout.viewport.height, layout.screenWidth, layout.screenHeight),
                CAPTURE_DELAY_FRAMES
        );
        active.showStatus(Component.empty());
        return true;
    }

    private static PhonePhoto saveCapturedPhoto(NativeImage image, CaptureRequest request) throws Exception {
        CaptureViewport viewport = request.viewport;
        if (viewport == null || viewport.width <= 0 || viewport.height <= 0 || viewport.screenWidth <= 0 || viewport.screenHeight <= 0) {
            return PhonePhotoAlbum.saveScreenshot(image);
        }

        int cropX = Math.round(viewport.x * image.getWidth() / (float) viewport.screenWidth);
        int cropY = Math.round(viewport.y * image.getHeight() / (float) viewport.screenHeight);
        int cropWidth = Math.round(viewport.width * image.getWidth() / (float) viewport.screenWidth);
        int cropHeight = Math.round(viewport.height * image.getHeight() / (float) viewport.screenHeight);
        return PhonePhotoAlbum.saveScreenshot(image, cropX, cropY, cropWidth, cropHeight);
    }

    private static void openAlbumFromCamera() {
        CameraSession active = session;
        if (active == null || pendingCapture != null) return;
        PhoneInfo phoneInfo = active.phoneInfo;
        closeSession();
        SmartPhoneClientUtil.openPhoneApp(phoneInfo, new PhotoAlbumApp());
    }

    private static void closeToPhone() {
        CameraSession active = session;
        if (active == null) return;
        PhoneInfo phoneInfo = active.phoneInfo;
        closeSession();
        queuedPhoneInfo = phoneInfo;
        queuedPhoneOpenTicks = 2;
        SmartPhoneClientUtil.openUnlockedPhone(phoneInfo);
    }

    private static void closeToGame() {
        closeSession();
    }

    private static void closeSession() {
        CameraSession active = session;
        if (active == null) return;
        pendingCapture = null;
        restoreCameraOptions(active);
        session = null;
    }

    private static boolean isCameraActive() {
        return session != null;
    }

    private static boolean canHandleWorldInput() {
        Minecraft minecraft = Minecraft.getInstance();
        return session != null && minecraft.player != null && minecraft.level != null && minecraft.screen == null;
    }

    private static boolean isCaptureFrame() {
        return pendingCapture != null && pendingCapture.framesUntilCapture <= 0;
    }

    private static void setZoomIndex(int index) {
        CameraSession active = session;
        if (active == null) return;
        active.zoomIndex = clampZoomIndex(index);
        applyCameraOptions(active);
    }

    private static int clampZoomIndex(int index) {
        return Math.max(0, Math.min(ZOOM_LEVELS.length - 1, index));
    }

    private static float currentZoom(CameraSession active) {
        return ZOOM_LEVELS[active.zoomIndex];
    }

    private static int targetFov(int originalFov, float zoom) {
        float safeZoom = Math.max(1.0f, zoom);
        int fov = Math.round(originalFov / safeZoom);
        return Math.max(30, Math.min(110, fov));
    }

    private static void applyCameraOptions(CameraSession active) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = false;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.options.fov().set(targetFov(active.originalFov, currentZoom(active)));
    }

    private static void restoreCameraOptions(CameraSession active) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.fov().set(active.originalFov);
        minecraft.options.hideGui = active.originalHideGui;
        minecraft.options.setCameraType(active.originalCameraType);
    }

    private static void updateLayout(CameraSession active, int width, int height) {
        // 上方留出文字和间距空间
        int topMargin = Math.max(28, height / 10);
        int bottomMargin = Math.max(28, height / 10);
        int sideMargin = Math.max(12, width / 32);

        int availWidth = width - sideMargin * 2;
        int availHeight = height - topMargin - bottomMargin;

        // 16:9 横屏比例取景框，适配玩家屏幕
        float targetRatio = 16f / 9f;
        int vpWidth, vpHeight;
        if (availWidth / (float) availHeight > targetRatio) {
            vpHeight = availHeight;
            vpWidth = Math.round(vpHeight * targetRatio);
        } else {
            vpWidth = availWidth;
            vpHeight = Math.round(vpWidth / targetRatio);
        }

        int vpX = (width - vpWidth) / 2;
        int vpY = topMargin + (availHeight - vpHeight) / 2;
        Rect viewport = new Rect(vpX, vpY, vpWidth, vpHeight);
        active.layout = new Layout(width, height, viewport);
    }

    private static void drawOverlay(GuiGraphics graphics, Font font, CameraSession active) {
        Layout layout = active.layout;
        drawViewfinder(graphics, layout.viewport);
        drawCompactStatus(graphics, font, active, layout);
        drawStatus(graphics, font, active, layout);
    }

    private static void drawViewfinder(GuiGraphics graphics, Rect viewport) {
        int centerX = viewport.x + viewport.width / 2;
        int centerY = viewport.y + viewport.height / 2;
        int corner = Math.max(14, Math.min(viewport.width, viewport.height) / 9);
        drawCorner(graphics, viewport.x, viewport.y, corner, 1, 1);
        drawCorner(graphics, viewport.x + viewport.width, viewport.y, corner, -1, 1);
        drawCorner(graphics, viewport.x, viewport.y + viewport.height, corner, 1, -1);
        drawCorner(graphics, viewport.x + viewport.width, viewport.y + viewport.height, corner, -1, -1);
        graphics.fill(centerX - 1, centerY - 8, centerX + 1, centerY + 8, ACCENT);
        graphics.fill(centerX - 8, centerY - 1, centerX + 8, centerY + 1, ACCENT);
    }

    private static void drawCorner(GuiGraphics graphics, int x, int y, int length, int xDir, int yDir) {
        int xEnd = x + length * xDir;
        int yEnd = y + length * yDir;
        graphics.fill(Math.min(x, xEnd), y - (yDir < 0 ? 1 : 0), Math.max(x, xEnd) + 1, y + (yDir > 0 ? 1 : 0), LINE);
        graphics.fill(x - (xDir < 0 ? 1 : 0), Math.min(y, yEnd), x + (xDir > 0 ? 1 : 0), Math.max(y, yEnd) + 1, LINE);
    }

    private static void drawCompactStatus(GuiGraphics graphics, Font font, CameraSession active, Layout layout) {
        Rect viewport = layout.viewport;
        int textY = viewport.y - 16;
        graphics.drawString(font, Component.translatable("smartPhone.ui.app.camera"), viewport.x, textY, TEXT_MUTED, false);
        graphics.drawString(font, "%.1fx".formatted(currentZoom(active)), viewport.x + viewport.width - 28, textY, TEXT_MUTED, false);
    }

    private static void drawStatus(GuiGraphics graphics, Font font, CameraSession active, Layout layout) {
        Rect viewport = layout.viewport;
        Component text = active.statusTicks > 0 ? active.status : Component.translatable("smartPhone.ui.app.camera.previewHint");
        int color = active.statusTicks > 0 ? 0xCCFFFFFF : 0x77FFFFFF;
        graphics.drawCenteredString(font, text, viewport.x + viewport.width / 2, viewport.y + viewport.height + 10, color);
    }

    public record CaptureViewport(int x, int y, int width, int height, int screenWidth, int screenHeight) {
    }

    private static class CaptureRequest {
        private final CaptureViewport viewport;
        private int framesUntilCapture;

        private CaptureRequest(CaptureViewport viewport, int framesUntilCapture) {
            this.viewport = viewport;
            this.framesUntilCapture = framesUntilCapture;
        }
    }

    private static class CameraSession {
        private PhoneInfo phoneInfo;
        private int zoomIndex;
        private final int originalFov;
        private final boolean originalHideGui;
        private final CameraType originalCameraType;
        private Component status = Component.empty();
        private int statusTicks;
        private Layout layout = Layout.EMPTY;

        private CameraSession(PhoneInfo phoneInfo, int zoomIndex, int originalFov, boolean originalHideGui, CameraType originalCameraType) {
            this.phoneInfo = phoneInfo;
            this.zoomIndex = zoomIndex;
            this.originalFov = originalFov;
            this.originalHideGui = originalHideGui;
            this.originalCameraType = originalCameraType;
        }

        private void showStatus(Component status) {
            this.status = status;
            this.statusTicks = status.getString().isEmpty() ? 0 : 45;
        }

        private void tickStatus() {
            if (statusTicks > 0) statusTicks--;
        }
    }

    private record Layout(int screenWidth, int screenHeight, Rect viewport) {
        private static final Layout EMPTY = new Layout(0, 0, Rect.EMPTY);
    }

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);
    }
}
