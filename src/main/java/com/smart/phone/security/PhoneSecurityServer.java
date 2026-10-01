package com.smart.phone.security;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.PhoneItem;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.s2c.S2CPayload;
import com.smart.phone.ui.data.PhoneInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 服务端手机解锁会话；换手机、收起或断线后撤销授权。 */
public final class PhoneSecurityServer {
    private static final Map<UUID, Access> ACCESS = new HashMap<>();
    private static final Map<AttemptKey, Attempts> ATTEMPTS = new HashMap<>();
    private PhoneSecurityServer() {}

    private static final class Access {
        final UUID owner;
        final UUID token = UUID.randomUUID();
        final ItemStack source;
        boolean unlocked;
        Access(UUID owner, ItemStack source, boolean unlocked) {
            this.owner = owner;
            this.source = source;
            this.unlocked = unlocked;
        }
    }
    private record AttemptKey(UUID viewer, UUID owner) {}
    private static final class Attempts { int failures; long blockedUntil; }

    public static boolean enabled(UUID owner) {
        return SmartPhone.getPhoneSavedData().getPasscode(owner) != null;
    }

    public static boolean blocked(UUID owner) {
        var info = SmartPhone.getPhoneSavedData().phoneInfoMap.get(owner);
        return info != null && info.isBlocked();
    }

    public static boolean possesses(ServerPlayer player, UUID owner) {
        return player != null && owner != null && (owner.equals(player.getUUID()) || heldSource(player, owner) != null);
    }

    private static ItemStack heldSource(ServerPlayer player, UUID owner) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.is(SmartPhoneRegistries.PHONE.get()) && owner.equals(PhoneItem.ownerOf(stack, player.getUUID()))) return stack;
        }
        return null;
    }

    private static boolean validAccess(ServerPlayer player, Access access) {
        return player != null && player.isAlive() && access != null && (access.source == null
                ? access.owner.equals(player.getUUID()) : heldSource(player, access.owner) == access.source);
    }

    public static boolean canAccess(ServerPlayer player, UUID owner) {
        if (!possesses(player, owner) || blocked(owner)) return false;
        if (!enabled(owner)) return true;
        var access = ACCESS.get(player.getUUID());
        return validAccess(player, access) && access.owner.equals(owner) && access.unlocked;
    }

    /** 旧的聊天/好友/通话 RPC 没有 owner 参数，也必须检查当前受保护手机。 */
    public static boolean canUsePhone(ServerPlayer player) {
        var access = ACCESS.get(player.getUUID());
        if (access != null) return canAccess(player, access.owner);
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.is(SmartPhoneRegistries.PHONE.get())) {
                UUID owner = PhoneItem.ownerOf(stack, player.getUUID());
                if (enabled(owner) || blocked(owner)) return false;
            }
        }
        return !enabled(player.getUUID()) && !blocked(player.getUUID());
    }

    public static UUID activeOwner(ServerPlayer player) {
        var access = ACCESS.get(player.getUUID());
        if (validAccess(player, access)) return access.owner;
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.is(SmartPhoneRegistries.PHONE.get())) return PhoneItem.ownerOf(stack, player.getUUID());
        }
        return player.getUUID();
    }

    public static void open(ServerPlayer player, UUID owner, boolean held) {
        if (!possesses(player, owner)) return;
        var access = new Access(owner, heldSource(player, owner), !enabled(owner) && !blocked(owner));
        ACCESS.put(player.getUUID(), access);
        RPCPacketDistributor.rpcToPlayer(player, held ? S2CPayload.OPEN_HELD_PHONE : S2CPayload.OPEN_PHONE,
                owner, snapshot(player, owner), access.token, enabled(owner), !access.unlocked,
                java.util.Objects.requireNonNullElse(com.smart.phone.util.PhoneOwnerResolver.name(player.getServer(), owner), ""));
    }

    public static PhoneInfo snapshot(ServerPlayer viewer, UUID owner) {
        var info = SmartPhone.getPhoneSavedData().getPhoneInfo(owner);
        if (canAccess(viewer, owner)) return info;
        var locked = new PhoneInfo();
        locked.setPhoneWallpaper(info.getPhoneWallpaper());
        locked.setIPhoneTimeSource(info.getIPhoneTimeSource());
        locked.setBlocked(info.isBlocked());
        locked.setHideDate(info.isHideDate());
        locked.setHideStatusIcons(info.isHideStatusIcons());
        locked.setHideOwnerName(info.isHideOwnerName());
        locked.setHideLockIcon(info.isHideLockIcon());
        locked.getInstalledApps().clear();
        locked.getExtensionData().clear();
        return locked;
    }

    public static void unlock(ServerPlayer player, UUID token, String pin) {
        var access = access(player, token);
        if (access == null) return;
        String error = verify(player, access.owner, pin);
        if (error == null) access.unlocked = true;
        result(player, access, "unlock", error);
    }

    public static void change(ServerPlayer player, UUID token, String currentPin, String newPin) {
        var access = access(player, token);
        if (access == null || !access.unlocked) return;
        String error = verify(player, access.owner, currentPin);
        if (error == null && (newPin == null || !newPin.isEmpty() && !PhonePasscode.valid(newPin))) error = "invalid";
        if (error == null) {
            SmartPhone.getPhoneSavedData().setPasscode(access.owner, newPin.isEmpty() ? null : PhonePasscode.create(newPin));
            // 修改密码会撤销其他持有者已获得的授权。
            for (var entry : ACCESS.entrySet()) {
                if (entry.getValue() != access && entry.getValue().owner.equals(access.owner)) {
                    entry.getValue().unlocked = !enabled(access.owner) && !blocked(access.owner);
                    var other = player.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (other != null) result(other, entry.getValue(), "lock", null);
                }
            }
        }
        result(player, access, "change", error);
    }

    private static String verify(ServerPlayer player, UUID owner, String pin) {
        if (blocked(owner)) return "blocked";
        var credential = SmartPhone.getPhoneSavedData().getPasscode(owner);
        if (credential == null) return null;
        var attempts = ATTEMPTS.computeIfAbsent(new AttemptKey(player.getUUID(), owner), key -> new Attempts());
        long now = System.currentTimeMillis();
        if (now < attempts.blockedUntil) return "wait";
        if (credential.matches(pin)) {
            ATTEMPTS.remove(new AttemptKey(player.getUUID(), owner));
            return null;
        }
        if (++attempts.failures >= 5) {
            attempts.failures = 0;
            attempts.blockedUntil = now + 30_000;
            return "wait";
        }
        return "wrong";
    }

    /**
     * 在管理员更新密码或拦截状态后撤销该手机的解锁授权，并同步当前持有者。
     *
     * @param server 执行更新的服务器
     * @param owner 已通过管理员权限检查并写入新密码的手机主人 UUID
     */
    public static void refreshAfterAdminReset(MinecraftServer server, UUID owner) {
        ATTEMPTS.keySet().removeIf(key -> key.owner().equals(owner));
        for (var entry : ACCESS.entrySet()) {
            var access = entry.getValue();
            if (!access.owner.equals(owner)) continue;
            access.unlocked = !enabled(owner) && !blocked(owner);
            var viewer = server.getPlayerList().getPlayer(entry.getKey());
            if (viewer != null) result(viewer, access, "lock", null);
        }
    }

    private static Access access(ServerPlayer player, UUID token) {
        var access = ACCESS.get(player.getUUID());
        return validAccess(player, access) && access.token.equals(token) ? access : null;
    }

    private static void result(ServerPlayer player, Access access, String action, String error) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.PHONE_SECURITY_RESULT,
                access.token, action, error == null, enabled(access.owner), snapshot(player, access.owner),
                error == null ? "" : "smartPhone.security." + error);
    }

    public static void close(ServerPlayer player, UUID token) {
        var access = ACCESS.get(player.getUUID());
        if (access != null && access.token.equals(token)) ACCESS.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        ACCESS.entrySet().removeIf(entry -> !validAccess(server.getPlayerList().getPlayer(entry.getKey()), entry.getValue()));
    }

    public static void clear() { ACCESS.clear(); ATTEMPTS.clear(); }
}
