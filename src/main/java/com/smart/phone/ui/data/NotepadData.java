package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.app.Notepad;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@LDLRegister(name = Notepad.NOTEPAD_ID + "_data", registry = IPhoneInfoData.ID)
public class NotepadData extends IPhoneInfoData {
    @Persisted
    private String[] text = new String[]{""};

    @Override
    public boolean isDefaultCreated() {
        return true;
    }
}