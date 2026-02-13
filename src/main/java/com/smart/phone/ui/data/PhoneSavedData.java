package com.smart.phone.ui.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhoneSavedData extends SavedData {
    public final Map<UUID, PhoneInfo> phoneInfoMap = new HashMap<>();

    public static SavedData.Factory<PhoneSavedData> factory() {
        return new SavedData.Factory<>(
                PhoneSavedData::new,
                PhoneSavedData::fromNbt
        );
    }

    public PhoneInfo getPhoneInfo(ServerPlayer player) {
        setDirty();
        PhoneInfo phoneInfo = phoneInfoMap.get(player.getUUID());
        if (phoneInfo == null) {
            phoneInfo = new PhoneInfo();
            setPhoneInfo(player, phoneInfo);
        }
        return phoneInfo;
    }

    public void setPhoneInfo(ServerPlayer player, PhoneInfo phoneInfo) {
        phoneInfoMap.put(player.getUUID(), phoneInfo);
        setDirty();
    }

    public void resetPhoneInfo(ServerPlayer player) {
        phoneInfoMap.remove(player.getUUID());
        setDirty();
    }

    public static PhoneSavedData fromNbt(CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        PhoneSavedData phoneSavedData = new PhoneSavedData();
        for (String player : nbt.getAllKeys()) {
            UUID uuid = UUID.fromString(player);
            PhoneInfo phoneInfo = new PhoneInfo();
            phoneInfo.deserializeNBT(provider, nbt.getCompound(player));
            phoneSavedData.phoneInfoMap.put(uuid, phoneInfo);
        }
        return phoneSavedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        for (Map.Entry<UUID, PhoneInfo> entry : phoneInfoMap.entrySet()) {
            compoundTag.put(entry.getKey().toString(), entry.getValue().serializeNBT(provider));
        }
        return compoundTag;
    }
}
