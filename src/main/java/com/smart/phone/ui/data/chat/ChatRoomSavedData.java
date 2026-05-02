package com.smart.phone.ui.data.chat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ChatRoomSavedData extends SavedData {
    public static final String DATA_NAME = "smart_phone_chat_rooms";
    public static final String DEFAULT_ROOM_ID = "global";
    public static final String DEFAULT_ROOM_NAME_KEY = "smartPhone.ui.app.chatRoom.publicRoom";
    public static final String DIRECT_ROOM_PREFIX = "dm:";

    private final List<ChatRoom> rooms = new ArrayList<>();

    public static SavedData.Factory<ChatRoomSavedData> factory() {
        return new SavedData.Factory<>(
                ChatRoomSavedData::new,
                ChatRoomSavedData::fromNbt
        );
    }

    public void ensureDefaultRooms() {
        getOrCreateRoom(DEFAULT_ROOM_ID);
    }

    public ChatRoom getOrCreateRoom(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        Optional<ChatRoom> existing = rooms.stream().filter(room -> normalizedRoomId.equals(room.getRoomId())).findFirst();
        if (existing.isPresent()) return existing.get();
        ChatRoom created = new ChatRoom(normalizedRoomId, DEFAULT_ROOM_NAME_KEY);
        rooms.add(created);
        rooms.sort(Comparator.comparing(ChatRoom::getRoomId));
        setDirty();
        return created;
    }

    public ChatRoomListSnapshot createListSnapshot() {
        ensureDefaultRooms();
        return new ChatRoomListSnapshot(rooms.stream()
                .filter(room -> !isDirectRoom(room.getRoomId()))
                .map(ChatRoomSummary::new)
                .toList());
    }

    public ChatRoomSnapshot createRoomSnapshot(String roomId) {
        return new ChatRoomSnapshot(getOrCreateRoom(roomId));
    }

    public ChatRoomMessage addPlayerMessage(String roomId, java.util.UUID senderUuid, String senderName, String body) {
        ChatRoom room = getOrCreateRoom(roomId);
        ChatRoomMessage message = new ChatRoomMessage(room.getRoomId(), senderUuid, senderName, body);
        room.addMessage(message);
        setDirty();
        return message;
    }

    public ChatRoomMessage addPlayerImageMessage(String roomId, java.util.UUID senderUuid, String senderName, byte[] imageData) {
        ChatRoom room = getOrCreateRoom(roomId);
        ChatRoomMessage message = new ChatRoomMessage(room.getRoomId(), senderUuid, senderName, "[image]", imageData);
        room.addMessage(message);
        setDirty();
        return message;
    }

    public static String directRoomId(java.util.UUID first, java.util.UUID second) {
        if (first == null || second == null) return DEFAULT_ROOM_ID;
        String firstId = first.toString();
        String secondId = second.toString();
        return firstId.compareTo(secondId) <= 0
                ? DIRECT_ROOM_PREFIX + firstId + ":" + secondId
                : DIRECT_ROOM_PREFIX + secondId + ":" + firstId;
    }

    public static boolean isDirectRoom(String roomId) {
        return roomId != null && roomId.startsWith(DIRECT_ROOM_PREFIX);
    }

    public static boolean isDirectParticipant(String roomId, java.util.UUID playerUuid) {
        return playerUuid != null && roomId != null && roomId.contains(playerUuid.toString());
    }

    public static ChatRoomSavedData fromNbt(CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        ChatRoomSavedData data = new ChatRoomSavedData();
        ListTag roomTags = nbt.getList("rooms", Tag.TAG_COMPOUND);
        for (Tag tag : roomTags) {
            if (!(tag instanceof CompoundTag roomTag)) continue;
            ChatRoom room = new ChatRoom();
            room.deserializeNBT(provider, roomTag);
            data.rooms.add(room);
        }
        data.ensureDefaultRooms();
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag roomTags = new ListTag();
        for (ChatRoom room : rooms) {
            roomTags.add(room.serializeNBT(provider));
        }
        compoundTag.put("rooms", roomTags);
        return compoundTag;
    }

    private String normalizeRoomId(String roomId) {
        return roomId == null || roomId.isBlank() ? DEFAULT_ROOM_ID : roomId;
    }
}
