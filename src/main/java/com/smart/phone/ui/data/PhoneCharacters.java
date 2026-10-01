package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.smart.phone.util.PhoneOwnerResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 世界内已注册的虚拟手机主人，不占用真实玩家的档案或 UUID。 */
public class PhoneCharacters implements IPersistedSerializable {
    @Persisted
    private Map<UUID, String> names = new HashMap<>();

    public UUID find(String name) {
        UUID id = PhoneOwnerResolver.characterId(name);
        return name.equals(names.get(id)) ? id : null;
    }

    public String name(UUID id) { return names.get(id); }

    public Set<String> names() { return Set.copyOf(names.values()); }

    public UUID register(String name) {
        UUID id = PhoneOwnerResolver.characterId(name);
        names.putIfAbsent(id, name);
        return id;
    }
}
