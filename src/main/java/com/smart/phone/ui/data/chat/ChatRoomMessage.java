package com.smart.phone.ui.data.chat;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ChatRoomMessage implements IPersistedSerializable {
    public static final Codec<ChatRoomMessage> CODEC = PersistedParser.createCodec(ChatRoomMessage::new);
    public static final StreamCodec<ByteBuf, ChatRoomMessage> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private UUID messageId = UUID.randomUUID();
    @Persisted
    private String roomId = "";
    @Persisted
    private long createdAtMillis = System.currentTimeMillis();
    @Persisted
    private UUID senderUuid = new UUID(0L, 0L);
    @Persisted
    private String senderName = "";
    @Persisted
    private String body = "";
    // 图片消息的缩略图 PNG 数据，文本消息为 null
    @Persisted
    private byte[] imageData = null;

    public ChatRoomMessage(String roomId, UUID senderUuid, String senderName, String body) {
        this.messageId = UUID.randomUUID();
        this.roomId = roomId == null ? "" : roomId;
        this.createdAtMillis = System.currentTimeMillis();
        this.senderUuid = senderUuid == null ? new UUID(0L, 0L) : senderUuid;
        this.senderName = senderName == null ? "" : senderName;
        this.body = body == null ? "" : body;
    }

    public ChatRoomMessage(String roomId, UUID senderUuid, String senderName, String body, byte[] imageData) {
        this(roomId, senderUuid, senderName, body);
        this.imageData = imageData;
    }
}
