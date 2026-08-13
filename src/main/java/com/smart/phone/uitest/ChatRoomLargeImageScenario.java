package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.SmartPhone;
import com.smart.phone.client.camera.PhonePhoto;
import com.smart.phone.client.camera.PhonePhotoAlbum;
import com.smart.phone.client.chat.PhoneChatClientState;
import com.smart.phone.ui.app.ChatRoomApp;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSavedData;
import com.smart.phone.util.SmartPhoneClientUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

/**
 * Guards the chat-image RPC boundary against Minecraft's 32,767-character string cap.
 */
@LDLRegisterClient(
        name = "chat_room_large_image",
        group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY
)
public final class ChatRoomLargeImageScenario implements UIScenario {
    private static final String FIXTURE_FILE = "ldtest_chat_image.png";
    private static final String PHOTO_SELECTOR = "#chat_room_photo_ldtest_chat_image_png";
    private static final int STRING_CAP = 32_767;
    private static final int SERVER_IMAGE_CAP = 100 * 1024;

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3)
                .defaultTimeoutMs(10_000)
                .tags("ui", "chat", "fast");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario
                .step("create high-entropy local photo fixture", context -> {
                    PhonePhoto photo;
                    try {
                        photo = createFixturePhoto();
                    } catch (IOException exception) {
                        throw new IllegalStateException("Could not create UI test image fixture", exception);
                    }
                    byte[] imageData = PhonePhotoAlbum.createThumbnailBytes(photo)
                            .orElseThrow(() -> new IllegalStateException("Could not create test image thumbnail"));
                    int encodedLength = Base64.getEncoder().encodeToString(imageData).length();
                    context.require("fixture exceeds the old string payload cap", encodedLength > STRING_CAP);
                    context.require("fixture stays within the server image limit", imageData.length <= SERVER_IMAGE_CAP);
                    context.attach("thumbnailBytes", Integer.toString(imageData.length));
                    context.attach("oldBase64Length", Integer.toString(encodedLength));
                    context.put("expectedImage", imageData);
                })
                .openScreen("open production chat room app", context -> {
                    SmartPhoneClientUtil.openPhoneApp(new PhoneInfo(), new ChatRoomApp());
                    return context.mc().screen;
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#chat_room_global")
                .click("#chat_room_global")
                .awaitElement("#chat_room_attach")
                .click("#chat_room_attach")
                .awaitElement(PHOTO_SELECTOR)
                .click(PHOTO_SELECTOR)
                .waitUntil("large image message returns from the server", context -> imageArrived(context.get("expectedImage")))
                .check("returned image bytes equal the selected thumbnail", context -> imageArrived(context.get("expectedImage")))
                .screenshot("large_image_sent")
                .teardown("close screen and remove local image fixture", context -> {
                    context.mc().setScreen(null);
                    PhonePhotoAlbum.delete(new PhonePhoto(fixturePath(), 0, 0));
                });
    }

    private static boolean imageArrived(byte[] expectedImage) {
        if (expectedImage == null) return false;
        return PhoneChatClientState.getRoom(ChatRoomSavedData.DEFAULT_ROOM_ID)
                .stream()
                .flatMap(snapshot -> snapshot.getMessages().stream())
                .map(ChatRoomMessage::getImageData)
                .anyMatch(actual -> Arrays.equals(expectedImage, actual));
    }

    private static PhonePhoto createFixturePhoto() throws IOException {
        Path path = fixturePath();
        if (Files.exists(path) && !PhonePhotoAlbum.delete(new PhonePhoto(path, 0, 0))) {
            throw new IOException("Could not replace stale UI test fixture " + path);
        }
        Files.createDirectories(path.getParent());
        try (NativeImage image = new NativeImage(160, 90, false)) {
            int state = 0x13579BDF;
            for (int y = 0; y < 90; y++) {
                for (int x = 0; x < 160; x++) {
                    state ^= state << 13;
                    state ^= state >>> 17;
                    state ^= state << 5;
                    image.setPixelRGBA(x, y, 0xFF000000 | (state & 0x00FFFFFF));
                }
            }
            image.writeToFile(path);
        }
        return new PhonePhoto(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path));
    }

    private static Path fixturePath() {
        return PhonePhotoAlbum.photoDirectory().resolve(FIXTURE_FILE);
    }
}
