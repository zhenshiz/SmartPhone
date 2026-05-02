package com.smart.phone.client.chat;

import com.smart.phone.ui.data.chat.ChatRoomListSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomMessage;
import com.smart.phone.ui.data.chat.ChatRoomSnapshot;
import com.smart.phone.ui.data.chat.ChatRoomSummary;
import com.smart.phone.ui.data.social.FriendEntry;
import com.smart.phone.ui.data.social.FriendListSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PhoneChatClientState {
    private static ChatRoomListSnapshot roomList = new ChatRoomListSnapshot();
    private static FriendListSnapshot friendList = new FriendListSnapshot();
    private static final Map<String, ChatRoomSnapshot> roomSnapshots = new HashMap<>();
    private static int version;

    public static int getVersion() {
        return version;
    }

    public static List<ChatRoomSummary> getRooms() {
        return roomList.getRooms();
    }

    public static List<FriendEntry> getFriends() {
        return friendList.getEntries();
    }

    public static Optional<ChatRoomSnapshot> getRoom(String roomId) {
        return Optional.ofNullable(roomSnapshots.get(roomId));
    }

    public static void receiveRoomList(ChatRoomListSnapshot snapshot) {
        roomList = snapshot == null ? new ChatRoomListSnapshot() : snapshot;
        version++;
    }

    public static void receiveFriendList(FriendListSnapshot snapshot) {
        friendList = snapshot == null ? new FriendListSnapshot() : snapshot;
        version++;
    }

    public static void receiveRoom(ChatRoomSnapshot snapshot) {
        if (snapshot == null || snapshot.getRoomId() == null) return;
        roomSnapshots.put(snapshot.getRoomId(), snapshot);
        upsertSummary(snapshot);
        version++;
    }

    public static void receiveMessage(ChatRoomMessage message) {
        if (message == null || message.getRoomId() == null || message.getMessageId() == null) return;
        ChatRoomSnapshot snapshot = roomSnapshots.get(message.getRoomId());
        if (snapshot != null && snapshot.getMessages().stream().noneMatch(existing -> message.getMessageId().equals(existing.getMessageId()))) {
            snapshot.getMessages().add(message);
            snapshot.getMessages().sort(Comparator.comparingLong(ChatRoomMessage::getCreatedAtMillis));
        }
        updateSummary(message);
        version++;
    }

    private static void upsertSummary(ChatRoomSnapshot snapshot) {
        List<ChatRoomSummary> summaries = new ArrayList<>(roomList.getRooms());
        summaries.removeIf(summary -> snapshot.getRoomId().equals(summary.getRoomId()));
        ChatRoomSummary summary = new ChatRoomSummary();
        summary.setRoomId(snapshot.getRoomId());
        summary.setDisplayNameKey(snapshot.getDisplayNameKey());
        summary.setDisplayName(snapshot.getDisplayName());
        summary.setMessageCount(snapshot.getMessages().size());
        if (!snapshot.getMessages().isEmpty()) {
            ChatRoomMessage latest = snapshot.getMessages().getLast();
            summary.setLatestPreview(latest.getBody());
            summary.setLatestAtMillis(latest.getCreatedAtMillis());
        }
        summaries.add(summary);
        summaries.sort(Comparator.comparing(ChatRoomSummary::getRoomId));
        roomList = new ChatRoomListSnapshot(summaries);
    }

    private static void updateSummary(ChatRoomMessage message) {
        for (ChatRoomSummary summary : roomList.getRooms()) {
            if (!message.getRoomId().equals(summary.getRoomId())) continue;
            summary.setLatestPreview(message.getBody());
            summary.setLatestAtMillis(message.getCreatedAtMillis());
            summary.setMessageCount(summary.getMessageCount() + 1);
            return;
        }
    }
}
