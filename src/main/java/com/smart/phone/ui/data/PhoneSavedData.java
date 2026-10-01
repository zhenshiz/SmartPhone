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
    private final Map<UUID, com.smart.phone.security.PhonePasscode> passcodes = new HashMap<>();
    private final PhoneCharacters characters = new PhoneCharacters();

    public PhoneCharacters getCharacters() { return characters; }

    public UUID registerCharacter(String name) {
        if (!com.smart.phone.util.PhoneOwnerResolver.validCharacterName(name)) throw new IllegalArgumentException("Invalid character name");
        UUID id = characters.register(name);
        getPhoneInfo(id);
        setDirty();
        return id;
    }

    public com.smart.phone.security.PhonePasscode getPasscode(UUID owner) { return passcodes.get(owner); }

    public void setPasscode(UUID owner, com.smart.phone.security.PhonePasscode passcode) {
        if (passcode == null) passcodes.remove(owner);
        else passcodes.put(owner, passcode);
        setDirty();
    }

    public static SavedData.Factory<PhoneSavedData> factory() {
        return new SavedData.Factory<>(
                PhoneSavedData::new,
                PhoneSavedData::fromNbt
        );
    }

    public PhoneInfo getPhoneInfo(ServerPlayer player) {
        return getPhoneInfo(player.getUUID());
    }

    public PhoneInfo getPhoneInfo(UUID owner) {
        setDirty();
        PhoneInfo phoneInfo = phoneInfoMap.get(owner);
        if (phoneInfo == null) {
            phoneInfo = new PhoneInfo();
            setPhoneInfo(owner, phoneInfo);
        } else {
            phoneInfo.ensureDefaultContent();
        }
        return phoneInfo;
    }

    public void setPhoneInfo(ServerPlayer player, PhoneInfo phoneInfo) {
        setPhoneInfo(player.getUUID(), phoneInfo);
    }

    public void setPhoneInfo(UUID owner, PhoneInfo phoneInfo) {
        phoneInfo.ensureDefaultContent();
        phoneInfoMap.put(owner, phoneInfo);
        setDirty();
    }

    public boolean hasPhoneInfo(UUID owner) {
        return phoneInfoMap.containsKey(owner);
    }

    public void resetPhoneInfo(ServerPlayer player) {
        phoneInfoMap.remove(player.getUUID());
        passcodes.remove(player.getUUID());
        setDirty();
    }

    public static PhoneSavedData fromNbt(CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        PhoneSavedData phoneSavedData = new PhoneSavedData();
        if (nbt.contains("_characters")) phoneSavedData.characters.deserializeNBT(provider, nbt.getCompound("_characters"));
        for (String player : nbt.getAllKeys()) {
            if (player.equals("_characters")) continue;
            UUID uuid = UUID.fromString(player);
            PhoneInfo phoneInfo = new PhoneInfo();
            phoneInfo.deserializeNBT(provider, nbt.getCompound(player));
            phoneInfo.ensureDefaultContent();
            phoneSavedData.phoneInfoMap.put(uuid, phoneInfo);
            if (nbt.getCompound(player).contains("_passcode")) {
                var passcode = new com.smart.phone.security.PhonePasscode();
                passcode.deserializeNBT(provider, nbt.getCompound(player).getCompound("_passcode"));
                phoneSavedData.passcodes.put(uuid, passcode);
            }
        }
        return phoneSavedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        compoundTag.put("_characters", characters.serializeNBT(provider));
        for (Map.Entry<UUID, PhoneInfo> entry : phoneInfoMap.entrySet()) {
            var tag = entry.getValue().serializeNBT(provider);
            var passcode = passcodes.get(entry.getKey());
            if (passcode != null) tag.put("_passcode", passcode.serializeNBT(provider));
            compoundTag.put(entry.getKey().toString(), tag);
        }
        return compoundTag;
    }
}
