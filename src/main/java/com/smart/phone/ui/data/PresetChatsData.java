package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.chat.ChatRoom;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Authored conversations belonging to one phone, independent of public chat history. */
@Data
@EqualsAndHashCode(callSuper = true)
@LDLRegister(name = SmartPhone.MOD_ID + ":preset_chats", registry = IPhoneInfoData.ID)
public class PresetChatsData extends IPhoneInfoData {
    @Persisted
    private List<ChatRoom> rooms = new ArrayList<>();

    public Optional<ChatRoom> findRoom(String id) {
        return rooms.stream().filter(room -> room.getRoomId().equals(id)).findFirst();
    }
}
