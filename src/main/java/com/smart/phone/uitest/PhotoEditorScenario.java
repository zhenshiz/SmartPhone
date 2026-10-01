package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.SmartPhone;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.ui.app.PhotoAlbumApp;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.editor.PhotoDoodleCanvas;
import com.smart.phone.ui.editor.PhotoEditorScreen;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

@LDLRegisterClient(name = "photo_editor", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class PhotoEditorScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10000).tags("phone", "photo", "editor");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.setHeldItem(ItemStack.EMPTY)
                .step("create a landscape photo and open the real album", ctx -> {
                    ctx.put("guiScale", ctx.mc().options.guiScale().get());
                    try (NativeImage image = new NativeImage(320, 180, false)) {
                        for (int y = 0; y < 180; y++) for (int x = 0; x < 320; x++) {
                            image.setPixelRGBA(x, y, y < 110 ? 0xFFDAAF72 : ((x / 20 + y / 20) % 2 == 0 ? 0xFF52884C : 0xFF659A5D));
                        }
                        PhonePhoto original = PhonePhotoAlbum.saveScreenshot(image);
                        ctx.put("original", original);
                        ctx.put("originalBytes", Files.readAllBytes(original.path()));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                    SmartPhoneClientUtil.openPhoneApp(new PhoneInfo(), new PhotoAlbumApp());
                    ctx.put("parent", ctx.screen());
                }).awaitElement(".phone_photo_tile")
                .screenshot("album_without_grid_background")
                .step("press this photo in the album", ctx -> {
                    PhonePhoto photo = ctx.get("original");
                    var bounds = ctx.el("#phone_photo_" + photo.fileName().replaceAll("[^a-zA-Z0-9_-]", "_")).bounds();
                    ctx.put("tileX", bounds.centerX());
                    ctx.put("tileY", bounds.centerY());
                    ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
                }).step("release photo tile", ctx -> ctx.input().mouseUp(ctx.get("tileX"), ctx.get("tileY"), 0))
                .awaitElement("#phone_photo_edit").click("#phone_photo_edit")
                .awaitScreen(PhotoEditorScreen.class).awaitElement("#photo_edit_canvas")
                .check("save and undo start disabled", ctx -> !element(ctx, "photo_edit_save").isActive()
                        && !element(ctx, "photo_edit_undo").isActive());
        s.click("#photo_edit_size_0");
        stroke(s, .5f, .5f, .5f, .5f);
        s.check("the fine brush can paint a single click", ctx -> pixel(ctx, 160, 90) == 0xFF505BF0)
                .click("#photo_edit_undo").click("#photo_edit_size_1");
        stroke(s, .25f, .35f, .75f, .35f);
        s.check("drag paints a continuous red stroke at photo coordinates", ctx -> {
                    try (NativeImage image = canvas(ctx).copyImage()) {
                        for (int x = 81; x < 239; x++) if (image.getPixelRGBA(x, 63) != 0xFF505BF0) return false;
                        return true;
                    }
                }).click("#photo_edit_color_blue");
        stroke(s, .3f, .7f, .7f, .7f);
        s.check("palette uses blue rather than swapping red and blue channels", ctx -> pixel(ctx, 160, 126) == 0xFFF28A57)
                .click("#photo_edit_undo")
                .check("undo removes only the last complete stroke", ctx -> pixel(ctx, 160, 63) == 0xFF505BF0
                        && pixel(ctx, 160, 126) != 0xFFF28A57)
                .key(GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL)
                .check("keyboard undo restores every original pixel", ctx -> {
                    PhonePhoto photo = ctx.get("original");
                    try (var input = Files.newInputStream(photo.path()); NativeImage original = NativeImage.read(input);
                         NativeImage edited = canvas(ctx).copyImage()) {
                        return samePixels(original, edited) && !element(ctx, "photo_edit_save").isActive();
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                }).click("#photo_edit_color_red");
        stroke(s, .2f, .3f, .8f, .65f);
        s.step("resize with a draft at GUI scale four", ctx -> {
                    ctx.mc().options.guiScale().set(4);
                    ctx.mc().resizeDisplay();
                }).ticks(2)
                .check("draft and controls survive GUI scaling", ctx -> canvas(ctx).isDirty()
                        && ctx.el("#photo_edit_save").bounds().isCenterOnScreen(ctx.screen().width, ctx.screen().height))
                .screenshot("photo_doodle_landscape")
                .click("#photo_edit_save")
                .waitUntil("save returns to original album screen", ctx -> ctx.screen() == ctx.get("parent"))
                .step("find and validate the saved copy", ctx -> {
                    PhonePhoto original = ctx.get("original");
                    String stem = original.fileName().replace(".png", "_edited");
                    PhonePhoto copy = PhonePhotoAlbum.listPhotos().stream().filter(p -> p.fileName().startsWith(stem)).findFirst().orElseThrow();
                    ctx.put("copy", copy);
                    try (var input = Files.newInputStream(copy.path()); NativeImage saved = NativeImage.read(input)) {
                        ctx.require("original file bytes remain intact", Arrays.equals(ctx.get("originalBytes"), Files.readAllBytes(original.path())));
                        ctx.require("saved photo keeps its original resolution", saved.getWidth() == 320 && saved.getHeight() == 180);
                        ctx.require("the saved PNG contains the red stroke", saved.getPixelRGBA(160, 85) == 0xFF505BF0);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                }).awaitElement("#phone_photo_edit").click("#phone_photo_edit")
                .awaitElement("#photo_edit_canvas").click("#photo_edit_color_black");
        stroke(s, .3f, .2f, .7f, .2f);
        s.key(GLFW.GLFW_KEY_ESCAPE)
                .check("Esc cancels without creating another photo", ctx -> {
                    PhonePhoto copy = ctx.get("copy");
                    return ctx.screen() == ctx.get("parent") && PhonePhotoAlbum.listPhotos().stream()
                            .noneMatch(p -> p.fileName().startsWith(copy.fileName().replace(".png", "_edited")));
                })
                .step("open a portrait image", ctx -> {
                    try (NativeImage image = new NativeImage(96, 192, false)) {
                        image.fillRect(0, 0, 96, 192, 0xFFBD9563);
                        PhonePhoto portrait = PhonePhotoAlbum.saveScreenshot(image);
                        ctx.put("portrait", portrait);
                        PhotoEditorScreen.open(portrait, saved -> ctx.put("portraitCopy", saved));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                }).awaitElement("#photo_edit_canvas")
                .step("click outside the portrait in the letterbox", ctx -> {
                    var b = ctx.el("#photo_edit_canvas").bounds();
                    ctx.input().mouseDown(b.x() + 2, b.centerY(), 0);
                }).step("release outside the portrait", ctx -> {
                    var b = ctx.el("#photo_edit_canvas").bounds();
                    ctx.input().mouseUp(b.x() + 2, b.centerY(), 0);
                }).check("letterbox click does not paint the photo", ctx -> !canvas(ctx).isDirty());
        stroke(s, .2f, .5f, .8f, .5f);
        s.check("portrait coordinates follow its fitted aspect ratio", ctx -> pixel(ctx, 48, 96) == 0xFF505BF0)
                .screenshot("photo_doodle_portrait")
                .click("#photo_edit_cancel")
                .check("cancel returns without saving portrait", ctx -> ctx.screen() == ctx.get("parent") && ctx.get("portraitCopy") == null)
                .teardown("close editor and remove only fixture photos", ctx -> {
                    ctx.mc().setScreen(null);
                    Integer scale = ctx.get("guiScale");
                    if (scale != null) {
                        ctx.mc().options.guiScale().set(scale);
                        ctx.mc().resizeDisplay();
                    }
                    for (String name : new String[]{"original", "copy", "portrait", "portraitCopy"}) {
                        PhonePhoto photo = ctx.get(name);
                        if (photo != null) PhonePhotoAlbum.delete(photo);
                    }
                });
    }

    private static com.lowdragmc.lowdraglib2.gui.ui.UIElement element(TestContext ctx, String id) {
        return ctx.requireUI().ui.rootElement.selectId(id).findFirst().orElseThrow();
    }

    private static PhotoDoodleCanvas canvas(TestContext ctx) {
        return (PhotoDoodleCanvas) element(ctx, "photo_edit_canvas");
    }

    private static int pixel(TestContext ctx, int x, int y) {
        try (NativeImage image = canvas(ctx).copyImage()) {
            return image.getPixelRGBA(x, y);
        }
    }

    private static boolean samePixels(NativeImage left, NativeImage right) {
        for (int y = 0; y < left.getHeight(); y++) for (int x = 0; x < left.getWidth(); x++) {
            if (left.getPixelRGBA(x, y) != right.getPixelRGBA(x, y)) return false;
        }
        return true;
    }

    private static float[] point(TestContext ctx, float u, float v) {
        var canvas = canvas(ctx);
        try (NativeImage image = canvas(ctx).copyImage()) {
            float scale = Math.min(canvas.getContentWidth() / image.getWidth(), canvas.getContentHeight() / image.getHeight());
            var world = canvas.getWorldMouse(canvas.getContentX() + canvas.getContentWidth() / 2 + (u - .5f) * image.getWidth() * scale,
                    canvas.getContentY() + canvas.getContentHeight() / 2 + (v - .5f) * image.getHeight() * scale);
            return new float[]{world.x, world.y};
        }
    }

    private static void stroke(ScenarioBuilder s, float u1, float v1, float u2, float v2) {
        s.step("position a photo stroke", ctx -> {
                    ctx.put("strokeStart", point(ctx, u1, v1));
                    ctx.put("strokeEnd", point(ctx, u2, v2));
                }).step("press photo", ctx -> {
                    float[] p = ctx.get("strokeStart");
                    ctx.input().mouseDown(p[0], p[1], 0);
                }).step("draw across photo", ctx -> {
                    float[] p = ctx.get("strokeEnd");
                    ctx.input().dragTo(p[0], p[1], 0);
                }).step("release photo", ctx -> {
                    float[] p = ctx.get("strokeEnd");
                    ctx.input().mouseUp(p[0], p[1], 0);
                });
    }
}
