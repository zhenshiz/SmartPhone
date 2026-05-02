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

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ChatRoomSnapshot implements IPersistedSerializable {
    public static final Codec<ChatRoomSnapshot> CODEC = PersistedParser.createCodec(ChatRoomSnapshot::new);
    public static final StreamCodec<ByteBuf, ChatRoomSnapshot> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private String roomId = "";
    @Persisted
    private String displayNameKey = "";
    @Persisted
    private String displayName = "";
    @Persisted
    private List<ChatRoomMessage> messages = new ArrayList<>();

    public ChatRoomSnapshot(ChatRoom room) {
        this.roomId = room.getRoomId();
        this.displayNameKey = room.getDisplayNameKey();
        this.messages = new ArrayList<>(room.getMessages());
    }

    public ChatRoomSnapshot(ChatRoom room, String displayName) {
        this(room);
        this.displayName = displayName == null ? "" : displayName;
    }
}
