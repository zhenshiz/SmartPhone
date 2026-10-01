package com.smart.phone.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.smart.phone.SmartPhone;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

/** 解析真实玩家和已注册虚拟角色，共用稳定的手机资料标识。 */
public final class PhoneOwnerResolver {
    private PhoneOwnerResolver() {}

    // 保留旧 UUID 组件的存储/网络形式，同时允许地图作者在物品命令中直接填写角色名。
    public static final Codec<UUID> COMPONENT_CODEC = Codec.either(UUIDUtil.CODEC, Codec.STRING).comapFlatMap(
            value -> value.map(DataResult::success, PhoneOwnerResolver::parseComponent), Either::left);

    private static DataResult<UUID> parseComponent(String value) {
        try { return DataResult.success(UUID.fromString(value)); }
        catch (IllegalArgumentException ignored) {
            return validCharacterName(value) ? DataResult.success(characterId(value))
                    : DataResult.error(() -> "Expected a UUID or a character name of 1–64 characters");
        }
    }

    public static boolean validCharacterName(String name) {
        if (name == null || name.isBlank() || !name.equals(name.strip()) || name.length() > 64
                || name.chars().anyMatch(Character::isISOControl)) return false;
        try { UUID.fromString(name); return false; }
        catch (IllegalArgumentException ignored) { return true; }
    }

    public static UUID characterId(String name) {
        return UUID.nameUUIDFromBytes(("smart_phone:character:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static UUID resolve(MinecraftServer server, String input) {
        UUID character = SmartPhone.getPhoneSavedData().getCharacters().find(input);
        if (character != null) return character;
        try {
            UUID uuid = UUID.fromString(input);
            return SmartPhoneServerUtil.isKnownPlayer(server, uuid) ? uuid : null;
        } catch (IllegalArgumentException ignored) {
            var online = server.getPlayerList().getPlayerByName(input);
            if (online != null) return online.getUUID();
            return server.getProfileCache().get(input).map(profile -> profile.getId())
                    .filter(uuid -> SmartPhoneServerUtil.isKnownPlayer(server, uuid)).orElse(null);
        }
    }

    public static String name(MinecraftServer server, UUID owner) {
        String character = SmartPhone.getPhoneSavedData().getCharacters().name(owner);
        if (character != null) return character;
        var online = server.getPlayerList().getPlayer(owner);
        return online != null ? online.getGameProfile().getName()
                : server.getProfileCache().get(owner).map(profile -> profile.getName()).orElse(null);
    }

    public static Stream<String> suggestions(MinecraftServer server) {
        return Stream.concat(Stream.of(server.getPlayerNames()), SmartPhone.getPhoneSavedData().getCharacters().names().stream())
                .map(com.mojang.brigadier.arguments.StringArgumentType::escapeIfRequired);
    }
}
