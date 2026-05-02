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
public class ChatRoomListSnapshot implements IPersistedSerializable {
    public static final Codec<ChatRoomListSnapshot> CODEC = PersistedParser.createCodec(ChatRoomListSnapshot::new);
    public static final StreamCodec<ByteBuf, ChatRoomListSnapshot> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private List<ChatRoomSummary> rooms = new ArrayList<>();

    public ChatRoomListSnapshot(List<ChatRoomSummary> rooms) {
        this.rooms = rooms == null ? new ArrayList<>() : new ArrayList<>(rooms);
    }
}
