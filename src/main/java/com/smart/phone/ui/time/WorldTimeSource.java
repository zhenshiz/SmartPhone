package com.smart.phone.ui.time;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.smart.phone.SmartPhone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@LDLRegister(name = WorldTimeSource.WORLD_TIME_SOURCE_ID, registry = IPhoneTimeSource.ID)
public class WorldTimeSource extends IPhoneTimeSource {
    public static final String WORLD_TIME_SOURCE_ID = SmartPhone.MOD_ID + ":world_time_source";

    @Override
    public String getDisplayName() {
        return "smartPhone.data.phoneTimeSource.type.worldTime";
    }

    @Override
    public void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        long dayTime = level.getDayTime();
        long totalTicks = (dayTime + 6000) % 24000;

        hour = (int) (totalTicks / 1000);
        minute = (int) (totalTicks % 1000 * 60 / 1000);
    }
}
