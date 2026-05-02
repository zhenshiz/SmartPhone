package com.smart.phone.ui.data.social;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PhoneSocialSavedData extends SavedData {
    public static final String DATA_NAME = "smart_phone_social";

    private final List<FriendRecord> records = new ArrayList<>();

    public static SavedData.Factory<PhoneSocialSavedData> factory() {
        return new SavedData.Factory<>(
                PhoneSocialSavedData::new,
                PhoneSocialSavedData::fromNbt
        );
    }

    public FriendListSnapshot createSnapshot(ServerPlayer player) {
        if (player == null || player.getServer() == null) return new FriendListSnapshot();
        UUID ownerUuid = player.getUUID();
        List<FriendEntry> entries = new ArrayList<>();
        for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
            if (ownerUuid.equals(onlinePlayer.getUUID())) continue;
            FriendRecord record = findRecord(ownerUuid, onlinePlayer.getUUID()).orElse(null);
            entries.add(new FriendEntry(
                    onlinePlayer.getUUID(),
                    onlinePlayer.getGameProfile().getName(),
                    record == null ? FriendStatus.NONE : record.getStatus(),
                    true
            ));
        }
        for (FriendRecord record : findRecords(ownerUuid)) {
            boolean alreadyVisible = entries.stream().anyMatch(entry -> record.getTargetUuid().equals(entry.getTargetUuid()));
            if (!alreadyVisible && !FriendStatus.NONE.equals(record.getStatus())) {
                entries.add(new FriendEntry(record.getTargetUuid(), record.getTargetName(), record.getStatus(), false));
            }
        }
        entries.sort(Comparator
                .comparingInt((FriendEntry entry) -> statusOrder(entry.getStatus()))
                .thenComparing(FriendEntry::getTargetName, String.CASE_INSENSITIVE_ORDER));
        return new FriendListSnapshot(entries);
    }

    public boolean requestFriend(ServerPlayer owner, ServerPlayer target) {
        if (owner == null || target == null || owner.getUUID().equals(target.getUUID())) return false;
        FriendRecord ownerRecord = getOrCreateRecord(owner.getUUID(), target.getUUID(), target.getGameProfile().getName());
        FriendRecord targetRecord = getOrCreateRecord(target.getUUID(), owner.getUUID(), owner.getGameProfile().getName());
        if (FriendStatus.ACCEPTED.equals(ownerRecord.getStatus())) return false;
        if (FriendStatus.PENDING_RECEIVED.equals(ownerRecord.getStatus())) {
            acceptFriend(owner, target);
            return true;
        }
        updateRecord(ownerRecord, target.getGameProfile().getName(), FriendStatus.PENDING_SENT);
        updateRecord(targetRecord, owner.getGameProfile().getName(), FriendStatus.PENDING_RECEIVED);
        setDirty();
        return true;
    }

    public boolean acceptFriend(ServerPlayer owner, ServerPlayer target) {
        if (owner == null || target == null) return false;
        FriendRecord ownerRecord = findRecord(owner.getUUID(), target.getUUID()).orElse(null);
        FriendRecord targetRecord = findRecord(target.getUUID(), owner.getUUID()).orElse(null);
        if (ownerRecord == null || targetRecord == null) return false;
        updateRecord(ownerRecord, target.getGameProfile().getName(), FriendStatus.ACCEPTED);
        updateRecord(targetRecord, owner.getGameProfile().getName(), FriendStatus.ACCEPTED);
        setDirty();
        return true;
    }

    public boolean acceptFriend(ServerPlayer owner, UUID targetUuid) {
        if (owner == null || targetUuid == null) return false;
        FriendRecord ownerRecord = findRecord(owner.getUUID(), targetUuid).orElse(null);
        FriendRecord targetRecord = findRecord(targetUuid, owner.getUUID()).orElse(null);
        if (ownerRecord == null || targetRecord == null) return false;
        updateRecord(ownerRecord, ownerRecord.getTargetName(), FriendStatus.ACCEPTED);
        updateRecord(targetRecord, owner.getGameProfile().getName(), FriendStatus.ACCEPTED);
        setDirty();
        return true;
    }

    public boolean removeFriend(ServerPlayer owner, UUID targetUuid) {
        if (owner == null || targetUuid == null) return false;
        boolean removed = records.removeIf(record ->
                (owner.getUUID().equals(record.getOwnerUuid()) && targetUuid.equals(record.getTargetUuid()))
                        || (targetUuid.equals(record.getOwnerUuid()) && owner.getUUID().equals(record.getTargetUuid()))
        );
        if (removed) setDirty();
        return removed;
    }

    public boolean areFriends(UUID first, UUID second) {
        return findRecord(first, second)
                .filter(record -> FriendStatus.ACCEPTED.equals(record.getStatus()))
                .isPresent();
    }

    public Optional<FriendRecord> findRecord(UUID ownerUuid, UUID targetUuid) {
        if (ownerUuid == null || targetUuid == null) return Optional.empty();
        return records.stream()
                .filter(record -> ownerUuid.equals(record.getOwnerUuid()) && targetUuid.equals(record.getTargetUuid()))
                .findFirst();
    }

    public static PhoneSocialSavedData fromNbt(CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        PhoneSocialSavedData data = new PhoneSocialSavedData();
        ListTag recordTags = nbt.getList("friends", Tag.TAG_COMPOUND);
        for (Tag tag : recordTags) {
            if (!(tag instanceof CompoundTag recordTag)) continue;
            FriendRecord record = new FriendRecord();
            record.deserializeNBT(provider, recordTag);
            data.records.add(record);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag recordTags = new ListTag();
        for (FriendRecord record : records) {
            recordTags.add(record.serializeNBT(provider));
        }
        compoundTag.put("friends", recordTags);
        return compoundTag;
    }

    private List<FriendRecord> findRecords(UUID ownerUuid) {
        return records.stream().filter(record -> ownerUuid.equals(record.getOwnerUuid())).toList();
    }

    private FriendRecord getOrCreateRecord(UUID ownerUuid, UUID targetUuid, String targetName) {
        return findRecord(ownerUuid, targetUuid).orElseGet(() -> {
            FriendRecord created = new FriendRecord(ownerUuid, targetUuid, targetName, FriendStatus.NONE);
            records.add(created);
            return created;
        });
    }

    private void updateRecord(FriendRecord record, String targetName, String status) {
        record.setTargetName(targetName == null ? "" : targetName);
        record.setStatus(status == null ? FriendStatus.NONE : status);
        record.setUpdatedAtMillis(System.currentTimeMillis());
    }

    private int statusOrder(String status) {
        return switch (status) {
            case FriendStatus.PENDING_RECEIVED -> 0;
            case FriendStatus.ACCEPTED -> 1;
            case FriendStatus.PENDING_SENT -> 2;
            default -> 3;
        };
    }
}
