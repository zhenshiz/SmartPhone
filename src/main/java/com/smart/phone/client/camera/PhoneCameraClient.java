package com.smart.phone.client.camera;

import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.ui.app.PhotoAlbumApp;
import com.smart.phone.ui.PhoneUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

public class PhoneCameraClient {
    private static final float[] ZOOM_LEVELS = {1.0f, 1.5f, 2.0f, 3.0f};
    private static final int CAPTURE_DELAY_FRAMES = 2;
    private static CameraSession session;
    private static CaptureRequest pendingCapture;
    private static CameraSession queuedReturn;

    public static boolean openPreview(PhoneInfo phoneInfo) {
        return openPreview(phoneInfo, 0);
    }

    public static boolean openPreview(PhoneInfo phoneInfo, int zoomIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (phoneInfo == null || phoneInfo.isBlocked() || minecraft.player == null || minecraft.level == null) return false;

        if (session == null) {
            session = new CameraSession(
                    phoneInfo,
                    clampZoomIndex(zoomIndex),
                    minecraft.options.hideGui,
                    minecraft.options.getCameraType(),
                    minecraft.screen instanceof ModularUIScreen screen
                            && screen.getModularUI().ui.rootElement instanceof PhoneUI ? screen : null
            );
        } else {
            session.phoneInfo = phoneInfo;
            session.zoomIndex = clampZoomIndex(zoomIndex);
        }

        queuedReturn = null;
        pendingCapture = null;
        minecraft.setScreen(null);
        minecraft.mouseHandler.grabMouse();
        applyCameraOptions();
        return true;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (queuedReturn != null) {
            CameraSession returning = queuedReturn;
            queuedReturn = null;
            if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
                if (returning.phoneScreen != null) {
                    PhoneUI phone = (PhoneUI) returning.phoneScreen.getModularUI().ui.rootElement;
                    phone.homeScreen.closeApp();
                    minecraft.setScreen(returning.phoneScreen);
                } else {
                    SmartPhoneClientUtil.openUnlockedPhone(returning.phoneInfo);
                }
                minecraft.getSoundManager().resume();
            }
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
        applyCameraOptions();
    }

    /** Esc first opens the vanilla pause screen, before InputEvent.Key is dispatched. */
    public static void handleScreenOpening(ScreenEvent.Opening event) {
        if (session != null && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            closeToPhone();
        }
    }

    public static boolean isCameraActive() {
        return session != null;
    }

    public static boolean receiveSecurity(UUID token, String action, boolean success, boolean enabled, PhoneInfo info, String error) {
        CameraSession active = session != null ? session : queuedReturn;
        if (active == null || active.phoneScreen == null) return false;
        var phone = (PhoneUI) active.phoneScreen.getModularUI().ui.rootElement;
        if (!token.equals(phone.getAccessToken())) return false;
        phone.securityResult(action, success, enabled, info, error);
        active.phoneInfo = phone.phoneInfo;
        if (phone.isAccessLocked()) returnToLockedPhone(active);
        return true;
    }

    public static boolean refreshPhoneInfo(UUID owner, PhoneInfo info) {
        CameraSession active = session != null ? session : queuedReturn;
        if (active == null || active.phoneScreen == null) return false;
        var phone = (PhoneUI) active.phoneScreen.getModularUI().ui.rootElement;
        if (!owner.equals(phone.getOwnerUuid())) return false;
        active.phoneInfo = phone.phoneInfo = info;
        if (info.isBlocked()) {
            phone.configureAccess(phone.getAccessToken(), phone.isPasscodeEnabled(), true);
            returnToLockedPhone(active);
        }
        return true;
    }

    private static void returnToLockedPhone(CameraSession active) {
        closeSession();
        queuedReturn = null;
        Minecraft.getInstance().setScreen(active.phoneScreen);
    }

    public static boolean isOverlayVisible() {
        return canHandleWorldInput();
    }

    public static float zoom() {
        return session == null ? 1f : currentZoom(session);
    }

    public static Component statusText() {
        return session != null && session.statusTicks > 0 ? session.status
                : Component.translatable("smartPhone.ui.app.camera.controls");
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
                applyCameraOptions();
            }
        }
    }

    private static boolean requestCaptureFromCameraMode() {
        CameraSession active = session;
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || pendingCapture != null || minecraft.player == null || minecraft.level == null) return false;

        var layout = PhoneCameraHud.layoutFor(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        var viewport = layout.viewport();
        pendingCapture = new CaptureRequest(
                new CaptureViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height(), layout.screenWidth(), layout.screenHeight()),
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
        closeSession();
        if (active.phoneScreen != null) {
            PhoneUI phone = (PhoneUI) active.phoneScreen.getModularUI().ui.rootElement;
            phone.homeScreen.openApp(new PhotoAlbumApp());
            Minecraft.getInstance().setScreen(active.phoneScreen);
        } else {
            SmartPhoneClientUtil.openPhoneApp(active.phoneInfo, new PhotoAlbumApp());
        }
    }

    private static void closeToPhone() {
        if (session == null) return;
        CameraSession returning = session;
        closeSession();
        queuedReturn = returning;
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
        PhoneCameraViewfinder.release();
    }

    private static boolean canHandleWorldInput() {
        Minecraft minecraft = Minecraft.getInstance();
        return session != null && minecraft.player != null && minecraft.level != null && minecraft.screen == null;
    }

    static boolean isCaptureFrame() {
        return pendingCapture != null && pendingCapture.framesUntilCapture <= 0;
    }

    private static void setZoomIndex(int index) {
        CameraSession active = session;
        if (active == null) return;
        active.zoomIndex = clampZoomIndex(index);
        applyCameraOptions();
    }

    private static int clampZoomIndex(int index) {
        return Math.max(0, Math.min(ZOOM_LEVELS.length - 1, index));
    }

    private static float currentZoom(CameraSession active) {
        return ZOOM_LEVELS[active.zoomIndex];
    }

    private static void applyCameraOptions() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = false;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void restoreCameraOptions(CameraSession active) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.hideGui = active.originalHideGui;
        minecraft.options.setCameraType(active.originalCameraType);
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
        private final boolean originalHideGui;
        private final CameraType originalCameraType;
        private Component status = Component.empty();
        private int statusTicks;
        private final ModularUIScreen phoneScreen;

        private CameraSession(PhoneInfo phoneInfo, int zoomIndex, boolean originalHideGui, CameraType originalCameraType, ModularUIScreen phoneScreen) {
            this.phoneInfo = phoneInfo;
            this.phoneScreen = phoneScreen;
            this.zoomIndex = zoomIndex;
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

}
