package com.smart.phone.event;

import com.smart.phone.SmartPhone;
import com.smart.phone.ui.data.PhoneSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = SmartPhone.MOD_ID)
public class PhoneServerEvent {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            SmartPhone.setPhoneSavedData(world.getDataStorage().computeIfAbsent(PhoneSavedData.factory(), "phone_info"));
        }
    }
}
