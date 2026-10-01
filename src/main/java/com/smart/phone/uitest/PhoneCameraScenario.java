package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.client.camera.*;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Exercises the screen-free camera through world input events, pause handling and screenshots. */
@LDLRegisterClient(name = "phone_camera", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhoneCameraScenario implements UIScenario {
    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("phone", "camera", "hud");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("remember camera settings and existing photos", ctx -> {
                    ctx.put("fov", ctx.mc().options.fov().get());
                    ctx.put("bobView", ctx.mc().options.bobView().get());
                    ctx.mc().options.bobView().set(false);
                    ctx.put("cameraType", ctx.mc().options.getCameraType());
                    ctx.put("hideGui", ctx.mc().options.hideGui);
                    Set<Path> photos = new HashSet<>();
                    PhonePhotoAlbum.listPhotos().forEach(p -> photos.add(p.path()));
                    ctx.put("photosBefore", photos);
                }).setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                .waitUntil("held phone ready", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced() && h.isRaised())
                .step("unlock phone for camera launch", ctx -> {
                    var phone = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    phone.screenContainer.removeChild(phone.lockScreen);
                    ctx.put("phoneScreen", ctx.screen());
                    ctx.put("owner", phone.getOwnerUuid());
                }).step("open camera using the desktop icon", ctx -> click(ctx, "#phone_app_smart_phone_camera"))
                .waitUntil("camera HUD active without a screen", ctx -> PhoneCameraClient.isOverlayVisible() && ctx.screen() == null)
                .check("camera leaves world input available without pausing", ctx -> ctx.screen() == null && !ctx.mc().isPaused())
                .check("landscape phone preserves body ratio and fits the window", ctx -> {
                    for (int[] size : new int[][]{{1280,720},{1024,768},{2560,1080}}) {
                        var layout = PhoneCameraHud.layoutFor(size[0],size[1]);
                        var body = layout.body();
                        if (Math.abs(body.width() / (float)body.height() - PhoneUI.SHELL_HEIGHT / PhoneUI.SHELL_WIDTH) > .02f
                                || body.x() < 0 || body.y() < 0 || body.right() > size[0] || body.bottom() > size[1]) return false;
                    }
                    return PhoneCameraHud.INSTANCE.getModularUI() != null;
                }).screenshot("landscape_camera")
                .step("walk while camera is open", ctx -> {
                    ctx.put("start", ctx.player().position());
                    ctx.mc().options.keyUp.setDown(true);
                }).ticks(12)
                .step("release movement key", ctx -> ctx.mc().options.keyUp.setDown(false))
                .check("player moved with the camera HUD open", ctx -> ctx.player().position().distanceTo((Vec3)ctx.get("start")) > .3
                        && ctx.screen() == null && PhoneCameraClient.isCameraActive())
                .server("place a coloured subject and stop in front of it", sc -> {
                    var origin = sc.player().blockPosition();
                    Map<BlockPos, BlockState> previous = new HashMap<>();
                    sc.put("sceneBlocks", previous);
                    var colours = new net.minecraft.world.level.block.Block[]{Blocks.RED_CONCRETE, Blocks.WHITE_CONCRETE,
                            Blocks.BLUE_CONCRETE, Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK};
                    for (int x = -2; x <= 2; x++) for (int y = 0; y < 4; y++) {
                        var pos = origin.offset(x, y, 8);
                        previous.put(pos, sc.level().getBlockState(pos));
                        sc.level().setBlockAndUpdate(pos, colours[x + 2].defaultBlockState());
                    }
                    sc.player().connection.teleport(origin.getX() + .5, sc.player().getY(), origin.getZ() + .5, 0, 0);
                }).ticks(30)
                .step("remember the 1x world image", ctx -> ctx.put("unzoomed", Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())))
                .screenshot("viewfinder_1x")
                .step("zoom using the world scroll event", ctx -> {
                    var scroll = new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0);
                    NeoForge.EVENT_BUS.post(scroll);
                    ctx.require("camera consumes scroll instead of changing slot", scroll.isCanceled());
                }).check("1.5x zoom leaves the game FOV unchanged", ctx -> PhoneCameraClient.zoom() == 1.5f
                        && ctx.mc().options.fov().get().equals(ctx.get("fov")))
                .ticks(4)
                .screenshot("landscape_camera_zoom")
                .step("zoom to 3x", ctx -> {
                    for (int i = 0; i < 2; i++) NeoForge.EVENT_BUS.post(new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0));
                }).ticks(4)
                .check("3x magnifies only the phone viewfinder", ctx -> {
                    try (NativeImage image = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
                        return PhoneCameraClient.zoom() == 3f && ctx.mc().options.fov().get().equals(ctx.get("fov"))
                                && outsideUnchanged(ctx, image) && matchesZoom(ctx, image, false, 3f);
                    }
                }).screenshot("viewfinder_3x")
                .step("press camera shutter", ctx -> { key(ctx, GLFW.GLFW_KEY_C, GLFW.GLFW_PRESS); key(ctx, GLFW.GLFW_KEY_C, GLFW.GLFW_RELEASE); })
                .waitUntil("photo saved to album", ctx -> newPhoto(ctx) != null)
                .check("saved photo matches 3x viewfinder without the HUD", ctx -> {
                    var photo = newPhoto(ctx);
                    ctx.put("captured", photo);
                    try (var input = java.nio.file.Files.newInputStream(photo.path()); NativeImage image = NativeImage.read(input)) {
                        return matchesZoom(ctx, image, true, 3f);
                    } catch (Exception ex) { throw new IllegalStateException(ex); }
                }).step("zoom back out to 1x", ctx -> {
                    for (int i = 0; i < 3; i++) NeoForge.EVENT_BUS.post(new InputEvent.MouseScrollingEvent(0, -1, false, false, false, 0, 0));
                }).ticks(4)
                .check("1x returns to the original scene", ctx -> {
                    try (NativeImage image = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
                        return PhoneCameraClient.zoom() == 1f && matchesZoom(ctx, image, false, 1f) && outsideUnchanged(ctx, image);
                    }
                }).step("open album from the camera", ctx -> key(ctx, GLFW.GLFW_KEY_G, GLFW.GLFW_PRESS))
                .waitUntil("album uses the same phone", ctx -> ctx.screen() == ctx.get("phoneScreen")
                        && ((HeldPhoneScreen)ctx.screen()).getPhoneUI().homeScreen.appUI instanceof com.smart.phone.ui.app.ui.PhotoAlbumUI)
                .check("album transition restores FOV and phone owner", ctx -> !PhoneCameraClient.isCameraActive()
                        && ctx.mc().options.fov().get().equals(ctx.get("fov"))
                        && ((HeldPhoneScreen)ctx.screen()).getPhoneUI().getOwnerUuid().equals(ctx.get("owner")))
                .step("reopen camera from phone", ctx -> {
                    var phone = ((HeldPhoneScreen)ctx.screen()).getPhoneUI();
                    phone.homeScreen.openApp(new com.smart.phone.ui.app.CameraApp());
                }).waitUntil("camera active again", ctx -> PhoneCameraClient.isOverlayVisible() && ctx.screen() == null)
                .step("zoom again after reopening camera", ctx -> {
                    for (int i = 0; i < 3; i++) NeoForge.EVENT_BUS.post(new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0));
                }).ticks(4)
                .check("reopened camera still magnifies the live scene", ctx -> {
                    try (NativeImage image = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
                        return PhoneCameraClient.zoom() == 3f && matchesZoom(ctx, image, false, 3f) && outsideUnchanged(ctx, image);
                    }
                }).step("Esc opens pause before the key event, as in KeyboardHandler", ctx -> {
                    ctx.mc().pauseGame(false);
                    key(ctx, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_PRESS);
                    key(ctx, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_RELEASE);
                    ctx.require("pause menu was suppressed", !(ctx.screen() instanceof PauseScreen));
                }).waitUntil("Esc returns to same unlocked phone desktop", ctx -> ctx.screen() == ctx.get("phoneScreen")
                        && ((HeldPhoneScreen)ctx.screen()).getPhoneUI().homeScreen.appUI == null)
                .check("closing camera restores settings and owner", ctx -> !PhoneCameraClient.isCameraActive()
                        && ctx.mc().options.fov().get().equals(ctx.get("fov"))
                        && ctx.mc().options.getCameraType().equals(ctx.get("cameraType"))
                        && ctx.mc().options.hideGui == (boolean)ctx.get("hideGui")
                        && ((HeldPhoneScreen)ctx.screen()).getPhoneUI().getOwnerUuid().equals(ctx.get("owner"))
                        && !ctx.mc().mouseHandler.isMouseGrabbed())
                .screenshot("camera_closed_to_desktop")
                .click("#phone_app_smart_phone_chat_room")
                .check("desktop remains interactive after closing camera", ctx -> ((HeldPhoneScreen)ctx.screen()).getPhoneUI().homeScreen.appUI
                        instanceof com.smart.phone.ui.app.ui.ChatRoomUI)
                .setHeldItem(ItemStack.EMPTY)
                .teardown("close camera and remove only this test photo", ctx -> {
                    ctx.mc().options.keyUp.setDown(false);
                    ctx.mc().options.bobView().set(ctx.get("bobView"));
                    NativeImage unzoomed = ctx.get("unzoomed");
                    if (unzoomed != null) unzoomed.close();
                    if (PhoneCameraClient.isCameraActive()) ctx.mc().pauseGame(false);
                    ctx.mc().setScreen(null);
                    PhoneCameraClient.tick();
                    ctx.mc().setScreen(null);
                    PhonePhoto photo = ctx.get("captured");
                    if (photo == null) photo = newPhoto(ctx);
                    if (photo != null) PhonePhotoAlbum.delete(photo);
                }).teardownServer("restore the photographed blocks", sc -> {
                    Map<BlockPos, BlockState> previous = sc.get("sceneBlocks");
                    if (previous != null) previous.forEach((pos, state) -> sc.level().setBlockAndUpdate(pos, state));
                });
    }

    private static PhoneCameraHud.Rect pixelViewport(TestContext ctx, NativeImage image) {
        var window = ctx.mc().getWindow();
        var rect = PhoneCameraHud.layoutFor(window.getGuiScaledWidth(), window.getGuiScaledHeight()).viewport();
        return new PhoneCameraHud.Rect(Math.round(rect.x() * image.getWidth() / (float) window.getGuiScaledWidth()),
                Math.round(rect.y() * image.getHeight() / (float) window.getGuiScaledHeight()),
                Math.round(rect.width() * image.getWidth() / (float) window.getGuiScaledWidth()),
                Math.round(rect.height() * image.getHeight() / (float) window.getGuiScaledHeight()));
    }

    private static boolean outsideUnchanged(TestContext ctx, NativeImage image) {
        NativeImage reference = ctx.get("unzoomed");
        int total = 0, matched = 0;
        // Sample the unobstructed margin, beyond both the phone and the hands.
        for (int y = 8; y < image.getHeight() * .8; y += 11) for (int x = 4; x < image.getWidth() * .05; x += 7) {
            total++;
            if (colourDifference(image.getPixelRGBA(x,y), reference.getPixelRGBA(x,y)) < 12) matched++;
        }
        ctx.log("Outside phone pixels unchanged: " + matched + "/" + total);
        return matched > total * .98;
    }

    private static boolean matchesZoom(TestContext ctx, NativeImage image, boolean photo, float zoom) {
        NativeImage reference = ctx.get("unzoomed");
        var viewport = pixelViewport(ctx, reference);
        if (photo && (image.getWidth() != viewport.width() || image.getHeight() != viewport.height())) return false;
        int matched = 0, changed = 0, total = 0;
        // Compare the rendered and saved images against a magnification of the original scene.
        // Avoid the small focus marks in the reference and in the on-screen HUD.
        for (int yi = 0; yi < 17; yi++) for (int xi = 0; xi < 23; xi++) {
            float u = (xi + .25f) / 23, v = (yi + .25f) / 17;
            float su = .5f + (u - .5f) / zoom, sv = .5f + (v - .5f) / zoom;
            if (focusMark(u,v) || focusMark(su,sv)) continue;
            int x = Math.round((viewport.width() - 1) * u), y = Math.round((viewport.height() - 1) * v);
            int expected = magnifiedPixel(reference, viewport, x, y, zoom);
            int actual = image.getPixelRGBA(x + (photo ? 0 : viewport.x()), y + (photo ? 0 : viewport.y()));
            total++;
            if (colourDifference(expected, actual) < 25) matched++;
            if (colourDifference(reference.getPixelRGBA(viewport.x() + x, viewport.y() + y), actual) > 40) changed++;
        }
        ctx.log((photo ? "Photo" : "Viewfinder") + " matches " + zoom + "x: " + matched + "/" + total + "; changed from 1x: " + changed);
        return matched > total * .90 && (zoom == 1f || changed > total * .15);
    }

    private static int magnifiedPixel(NativeImage image, PhoneCameraHud.Rect viewport, int x, int y, float zoom) {
        // The viewfinder uses linear filtering. Nearest-pixel sampling falsely rejects
        // textured subjects at low resolutions, especially along gold/emerald details.
        int width = Math.max(1, Math.round(viewport.width() / zoom));
        int height = Math.max(1, Math.round(viewport.height() / zoom));
        float sx = Math.clamp((x + .5f) * width / viewport.width() - .5f, 0, width - 1);
        float sy = Math.clamp((y + .5f) * height / viewport.height() - .5f, 0, height - 1);
        int x0 = (int) sx, y0 = (int) sy;
        int ox = viewport.x() + (viewport.width() - width) / 2;
        // The framebuffer crop is centred from the bottom, while NativeImage starts at the top.
        int oy = viewport.y() + viewport.height() - height - (viewport.height() - height) / 2;
        int a = image.getPixelRGBA(ox + x0, oy + y0);
        int b = image.getPixelRGBA(ox + Math.min(x0 + 1, width - 1), oy + y0);
        int c = image.getPixelRGBA(ox + x0, oy + Math.min(y0 + 1, height - 1));
        int d = image.getPixelRGBA(ox + Math.min(x0 + 1, width - 1), oy + Math.min(y0 + 1, height - 1));
        float fx = sx - x0, fy = sy - y0;
        int result = 0xff000000;
        for (int shift = 0; shift < 24; shift += 8) {
            float top = (a >>> shift & 255) * (1 - fx) + (b >>> shift & 255) * fx;
            float bottom = (c >>> shift & 255) * (1 - fx) + (d >>> shift & 255) * fx;
            result |= Math.round(top * (1 - fy) + bottom * fy) << shift;
        }
        return result;
    }

    private static boolean focusMark(float u, float v) {
        return (Math.abs(u - .5f) < .015f && Math.abs(v - .5f) < .025f)
                || (Math.abs(Math.abs(u - .5f) - .12f) < .025f && Math.abs(Math.abs(v - .5f) - .14f) < .07f);
    }

    private static int colourDifference(int a, int b) {
        return Math.max(Math.abs((a & 255) - (b & 255)), Math.max(Math.abs((a >>> 8 & 255) - (b >>> 8 & 255)),
                Math.abs((a >>> 16 & 255) - (b >>> 16 & 255))));
    }

    private static PhonePhoto newPhoto(TestContext ctx) {
        Set<Path> before = ctx.get("photosBefore");
        return before == null ? null : PhonePhotoAlbum.listPhotos().stream().filter(p -> !before.contains(p.path())).findFirst().orElse(null);
    }
    private static void click(TestContext ctx, String selector) {
        var bounds = ctx.el(selector).bounds();
        ctx.input().mouseDown(bounds.centerX(),bounds.centerY(),0);
        ctx.input().mouseUp(bounds.centerX(),bounds.centerY(),0);
    }
    private static void key(TestContext ctx, int key, int action) {
        // Synthetic mode deliberately blocks KeyboardHandler callbacks and physical pointer capture.
        NeoForge.EVENT_BUS.post(new InputEvent.Key(key, 0, action, 0));
    }
}
