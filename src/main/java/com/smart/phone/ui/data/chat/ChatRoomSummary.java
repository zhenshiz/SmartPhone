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

@Data
@NoArgsConstructor
public class ChatRoomSummary implements IPersistedSerializable {
    public static final Codec<ChatRoomSummary> CODEC = PersistedParser.createCodec(ChatRoomSummary::new);
    public static final StreamCodec<ByteBuf, ChatRoomSummary> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private String roomId = "";
    @Persisted
    private String displayNameKey = "";
    @Persisted
    private String displayName = "";
    @Persisted
    private String latestPreview = "";
    @Persisted
    private long latestAtMillis;
    @Persisted
    private int messageCount;

    public ChatRoomSummary(ChatRoom room) {
        this.roomId = room.getRoomId();
        this.displayNameKey = room.getDisplayNameKey();
        this.messageCount = room.getMessages().size();
        room.latestMessage().ifPresent(message -> {
            this.latestPreview = message.getBody();
            this.latestAtMillis = message.getCreatedAtMillis();
        });
    }
}
