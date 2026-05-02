package com.smart.phone.client.camera;

import java.nio.file.Path;

public record PhonePhoto(Path path, long lastModifiedMillis, long fileSize) {
    public String fileName() {
        return path.getFileName().toString();
    }
}
