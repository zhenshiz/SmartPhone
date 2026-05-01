package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.smart.phone.ui.app.OfficialMessages;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@LDLRegister(name = OfficialMessages.OFFICIAL_MESSAGES_ID + "_data", registry = IPhoneInfoData.ID)
public class OfficialMessagesData extends IPhoneInfoData {
    @Persisted
    private List<OfficialMessage> messages = new ArrayList<>();

    public void addMessage(OfficialMessage message) {
        if (message == null || message.getMessageId() == null) return;
        if (findMessage(message.getMessageId()).isPresent()) return;
        messages.add(message);
        messages.sort(Comparator.comparingLong(OfficialMessage::getCreatedAtMillis).reversed());
    }

    public Optional<OfficialMessage> findMessage(UUID messageId) {
        if (messageId == null) return Optional.empty();
        return messages.stream().filter(message -> messageId.equals(message.getMessageId())).findFirst();
    }

    public void markRead(UUID messageId) {
        findMessage(messageId).ifPresent(message -> message.setRead(true));
    }

    public long unreadCount() {
        return messages.stream().filter(message -> !message.isRead()).count();
    }

    @Override
    public boolean isDefaultCreated() {
        return true;
    }
}
