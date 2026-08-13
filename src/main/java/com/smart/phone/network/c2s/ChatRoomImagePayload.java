package com.smart.phone.network.c2s;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天图片的二进制 RPC 负载。顶层对象由已注册的 direct accessor 传输，避免把图片编码为受限长度的字符串。
 */
@Data
@NoArgsConstructor
public class ChatRoomImagePayload implements IPersistedSerializable {
    @Persisted
    private String roomId = "";
    @Persisted
    private byte[] imageData = new byte[0];

    public ChatRoomImagePayload(String roomId, byte[] imageData) {
        this.roomId = roomId == null ? "" : roomId;
        this.imageData = imageData == null ? new byte[0] : imageData;
    }
}
