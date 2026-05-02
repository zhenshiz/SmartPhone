package com.smart.phone.ui.data.social;

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
public class FriendListSnapshot implements IPersistedSerializable {
    public static final Codec<FriendListSnapshot> CODEC = PersistedParser.createCodec(FriendListSnapshot::new);
    public static final StreamCodec<ByteBuf, FriendListSnapshot> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private List<FriendEntry> entries = new ArrayList<>();

    public FriendListSnapshot(List<FriendEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }
}
