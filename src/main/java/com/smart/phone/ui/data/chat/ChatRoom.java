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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class ChatRoom implements IPersistedSerializable {
    public static final Codec<ChatRoom> CODEC = PersistedParser.createCodec(ChatRoom::new);
    public static final StreamCodec<ByteBuf, ChatRoom> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public static final int MAX_MESSAGES = 100;

    @Persisted
    private String roomId = "";
    @Persisted
    private String displayNameKey = "";
    @Persisted
    private List<ChatRoomMessage> messages = new ArrayList<>();

    public ChatRoom(String roomId, String displayNameKey) {
        this.roomId = roomId == null ? "" : roomId;
        this.displayNameKey = displayNameKey == null ? "" : displayNameKey;
    }

    public void addMessage(ChatRoomMessage message) {
        if (message == null || message.getMessageId() == null) return;
        if (messages.stream().anyMatch(existing -> message.getMessageId().equals(existing.getMessageId()))) return;
        messages.add(message);
        messages.sort(Comparator.comparingLong(ChatRoomMessage::getCreatedAtMillis));
        while (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }
    }

    public Optional<ChatRoomMessage> latestMessage() {
        if (messages.isEmpty()) return Optional.empty();
        return Optional.of(messages.getLast());
    }
}
