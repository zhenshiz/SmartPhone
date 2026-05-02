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

import java.util.UUID;

@Data
@NoArgsConstructor
public class FriendRecord implements IPersistedSerializable {
    public static final Codec<FriendRecord> CODEC = PersistedParser.createCodec(FriendRecord::new);
    public static final StreamCodec<ByteBuf, FriendRecord> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Persisted
    private UUID ownerUuid = new UUID(0L, 0L);
    @Persisted
    private UUID targetUuid = new UUID(0L, 0L);
    @Persisted
    private String targetName = "";
    @Persisted
    private String status = FriendStatus.NONE;
    @Persisted
    private long updatedAtMillis = System.currentTimeMillis();

    public FriendRecord(UUID ownerUuid, UUID targetUuid, String targetName, String status) {
        this.ownerUuid = ownerUuid == null ? new UUID(0L, 0L) : ownerUuid;
        this.targetUuid = targetUuid == null ? new UUID(0L, 0L) : targetUuid;
        this.targetName = targetName == null ? "" : targetName;
        this.status = status == null ? FriendStatus.NONE : status;
        this.updatedAtMillis = System.currentTimeMillis();
    }
}
