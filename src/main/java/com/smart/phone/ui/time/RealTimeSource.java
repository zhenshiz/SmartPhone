package com.smart.phone.ui.time;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;

import java.time.LocalDateTime;

@LDLRegister(name = RealTimeSource.REAL_TIME_SOURCE_ID, registry = IPhoneTimeSource.ID)
public class RealTimeSource extends IPhoneTimeSource {
    public static final String REAL_TIME_SOURCE_ID = SmartPhone.MOD_ID + ":real_time_source";

    @Override
    public String getDisplayName() {
        return "smartPhone.data.phoneTimeSource.type.realTime";
    }

    @Override
    public void tick() {
        super.hour = LocalDateTime.now().getHour();
        super.minute = LocalDateTime.now().getMinute();
    }
}
