package com.smart.phone.client.camera;

import com.mojang.blaze3d.platform.NativeImage;
import com.smart.phone.SmartPhone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class PhonePhotoAlbum {
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").withZone(ZoneId.systemDefault());
    private static final Map<Path, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    // 聊天图片消息缩略图纹理缓存，key 为 messageId
    private static final Map<UUID, ResourceLocation> MESSAGE_IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger MESSAGE_TEXTURE_COUNTER = new AtomicInteger(0);

    public static Path photoDirectory() {
        Minecraft minecraft = Minecraft.getInstance();
        String owner = minecraft.player == null ? "local" : minecraft.player.getUUID().toString();
        return minecraft.gameDirectory.toPath().resolve("smart_phone").resolve("photos").resolve(owner);
    }

    public static List<PhonePhoto> listPhotos() {
        Path directory = photoDirectory();
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .map(PhonePhotoAlbum::createPhoto)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparingLong(PhonePhoto::lastModifiedMillis).reversed())
                    .toList();
        } catch (IOException exception) {
            SmartPhone.LOGGER.warn("Failed to list smart phone photos", exception);
            return List.of();
        }
    }

    public static Optional<PhonePhoto> latestPhoto() {
        List<PhonePhoto> photos = listPhotos();
        return photos.isEmpty() ? Optional.empty() : Optional.of(photos.getFirst());
    }

    public static PhonePhoto saveScreenshot(NativeImage image) throws IOException {
        Path directory = photoDirectory();
        Files.createDirectories(directory);
        Path target = createUniquePhotoPath(directory);
        image.writeToFile(target);
        return createPhoto(target).orElseGet(() -> new PhonePhoto(target, System.currentTimeMillis(), 0));
    }

    public static PhonePhoto saveScreenshot(NativeImage image, int x, int y, int width, int height) throws IOException {
        int cropX = Math.max(0, Math.min(image.getWidth() - 1, x));
        int cropY = Math.max(0, Math.min(image.getHeight() - 1, y));
        int cropWidth = Math.max(1, Math.min(width, image.getWidth() - cropX));
        int cropHeight = Math.max(1, Math.min(height, image.getHeight() - cropY));
        try (NativeImage cropped = new NativeImage(cropWidth, cropHeight, false)) {
            image.copyRect(cropped, cropX, cropY, 0, 0, cropWidth, cropHeight, false, false);
            return saveScreenshot(cropped);
        }
    }

    public static Optional<ResourceLocation> textureFor(PhonePhoto photo) {
        if (photo == null) return Optional.empty();
        Path normalized = photo.path().toAbsolutePath().normalize();
        ResourceLocation cached = TEXTURE_CACHE.get(normalized);
        if (cached != null) return Optional.of(cached);
        if (!Files.isRegularFile(normalized)) return Optional.empty();

        try (InputStream inputStream = Files.newInputStream(normalized)) {
            NativeImage image = NativeImage.read(inputStream);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register("smart_phone_photo", new DynamicTexture(image));
            TEXTURE_CACHE.put(normalized, location);
            return Optional.of(location);
        } catch (Exception exception) {
            SmartPhone.LOGGER.warn("Failed to load smart phone photo texture {}", normalized, exception);
            return Optional.empty();
        }
    }

    public static boolean delete(PhonePhoto photo) {
        if (photo == null) return false;
        Path normalized = photo.path().toAbsolutePath().normalize();
        releaseTexture(normalized);
        try {
            return Files.deleteIfExists(normalized);
        } catch (IOException exception) {
            SmartPhone.LOGGER.warn("Failed to delete smart phone photo {}", normalized, exception);
            return false;
        }
    }

    private static void releaseTexture(Path normalized) {
        ResourceLocation location = TEXTURE_CACHE.remove(normalized);
        if (location != null) {
            Minecraft.getInstance().getTextureManager().release(location);
        }
    }

    private static Optional<PhonePhoto> createPhoto(Path path) {
        try {
            return Optional.of(new PhonePhoto(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path)));
        } catch (IOException exception) {
            SmartPhone.LOGGER.warn("Failed to read smart phone photo metadata {}", path, exception);
            return Optional.empty();
        }
    }

    /**
     * 将本地照片缩放为 160×90 PNG 缩略图 byte[]，用于聊天图片消息发送。
     */
    public static Optional<byte[]> createThumbnailBytes(PhonePhoto photo) {
        if (photo == null) return Optional.empty();
        Path normalized = photo.path().toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) return Optional.empty();
        try (InputStream inputStream = Files.newInputStream(normalized)) {
            NativeImage image = NativeImage.read(inputStream);
            return Optional.of(downscaleToPngBytes(image, 160, 90));
        } catch (Exception exception) {
            SmartPhone.LOGGER.warn("Failed to create thumbnail for {}", normalized, exception);
            return Optional.empty();
        }
    }

    /**
     * 面积平均缩放 NativeImage 到指定尺寸（逐像素采样，不使用 copyRect）。
     */
    public static NativeImage downscale(NativeImage source, int targetWidth, int targetHeight) {
        NativeImage result = new NativeImage(targetWidth, targetHeight, false);
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        float xRatio = srcWidth / (float) targetWidth;
        float yRatio = srcHeight / (float) targetHeight;
        for (int ty = 0; ty < targetHeight; ty++) {
            for (int tx = 0; tx < targetWidth; tx++) {
                int srcX = (int) (tx * xRatio);
                int srcY = (int) (ty * yRatio);
                int srcX2 = Math.min((int) ((tx + 1) * xRatio), srcWidth);
                int srcY2 = Math.min((int) ((ty + 1) * yRatio), srcHeight);
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int sy = srcY; sy < srcY2; sy++) {
                    for (int sx = srcX; sx < srcX2; sx++) {
                        int color = source.getPixelRGBA(sx, sy);
                        a += (color >> 24) & 0xFF;
                        r += (color >> 16) & 0xFF;
                        g += (color >> 8) & 0xFF;
                        b += color & 0xFF;
                        count++;
                    }
                }
                if (count > 0) {
                    result.setPixelRGBA(tx, ty, ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count));
                }
            }
        }
        return result;
    }

    /**
     * 缩放 NativeImage 并写出为 PNG byte[]。
     */
    public static byte[] downscaleToPngBytes(NativeImage source, int targetWidth, int targetHeight) throws IOException {
        NativeImage downscaled = downscale(source, targetWidth, targetHeight);
        try {
            return downscaled.asByteArray();
        } finally {
            downscaled.close();
        }
    }

    /**
     * 从聊天消息的 byte[] PNG 数据创建纹理，用 messageId 做缓存 key。
     */
    public static Optional<ResourceLocation> textureForMessageData(UUID messageId, byte[] pngBytes) {
        if (messageId == null || pngBytes == null || pngBytes.length == 0) return Optional.empty();
        ResourceLocation cached = MESSAGE_IMAGE_CACHE.get(messageId);
        if (cached != null) return Optional.of(cached);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pngBytes)) {
            NativeImage image = NativeImage.read(inputStream);
            // 递增 id 避免纹理名冲突
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .register("smart_phone_msg_" + MESSAGE_TEXTURE_COUNTER.getAndIncrement(), new DynamicTexture(image));
            MESSAGE_IMAGE_CACHE.put(messageId, location);
            return Optional.of(location);
        } catch (Exception exception) {
            SmartPhone.LOGGER.warn("Failed to create texture from chat image message {}", messageId, exception);
            return Optional.empty();
        }
    }

    private static Path createUniquePhotoPath(Path directory) {
        String baseName = FILE_NAME_FORMATTER.format(LocalDateTime.now());
        Path target = directory.resolve(baseName + ".png");
        int index = 1;
        while (Files.exists(target)) {
            target = directory.resolve(baseName + "_" + index + ".png");
            index++;
        }
        return target;
    }
}
