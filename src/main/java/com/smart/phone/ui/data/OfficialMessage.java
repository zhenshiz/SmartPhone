package com.smart.phone.ui.data;

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
public class OfficialMessage implements IPersistedSerializable {
    public static final Codec<OfficialMessage> CODEC = PersistedParser.createCodec(OfficialMessage::new);
    public static final StreamCodec<ByteBuf, OfficialMessage> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private UUID messageId = UUID.randomUUID();
    @Persisted
    private long createdAtMillis = System.currentTimeMillis();
    @Persisted
    private String sender = "";
    @Persisted
    private String title = "";
    @Persisted
    private String body = "";
    @Persisted
    private boolean read;

    public OfficialMessage(String sender, String title, String body) {
        this.messageId = UUID.randomUUID();
        this.createdAtMillis = System.currentTimeMillis();
        this.sender = sender == null ? "" : sender;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
    }
}
