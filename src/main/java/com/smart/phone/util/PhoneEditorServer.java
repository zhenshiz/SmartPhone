package com.smart.phone.util;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.s2c.S2CPayload;
import com.smart.phone.security.PhonePasscode;
import com.smart.phone.security.PhoneSecurityServer;
import com.smart.phone.ui.data.PhoneInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Server-authoritative entry points for editing a known player's phone SavedData. */
public final class PhoneEditorServer {
    private PhoneEditorServer() {}

    public static boolean open(ServerPlayer editor, UUID owner) {
        if (!editor.hasPermissions(2) || !SmartPhoneServerUtil.isKnownPlayer(editor.getServer(), owner)) return false;
        String name = PhoneOwnerResolver.name(editor.getServer(), owner);
        if (name == null) name = Component.translatable("smartPhone.item.phone.unknownOwner").getString();
        RPCPacketDistributor.rpcToPlayer(editor, S2CPayload.OPEN_PHONE_EDITOR, owner, name,
                UUID.randomUUID(), SmartPhone.getPhoneSavedData().getPhoneInfo(owner), PhoneSecurityServer.enabled(owner));
        return true;
    }

    public static void save(ServerPlayer editor, UUID owner, UUID session, PhoneInfo draft,
                            boolean updatePasscode, String passcode) {
        if (!editor.hasPermissions(2) || !SmartPhoneServerUtil.isKnownPlayer(editor.getServer(), owner) || draft == null
                || updatePasscode && (passcode == null || !passcode.isEmpty() && !PhonePasscode.valid(passcode))) {
            RPCPacketDistributor.rpcToPlayer(editor, S2CPayload.PHONE_EDITOR_RESULT, session, false);
            return;
        }
        // 校验及摘要生成完成后才写入，避免无效密码导致其余草稿被部分保存。
        var credential = updatePasscode && !passcode.isEmpty() ? PhonePasscode.create(passcode) : null;
        boolean blockedChanged = PhoneSecurityServer.blocked(owner) != draft.isBlocked();
        SmartPhone.getPhoneSavedData().setPhoneInfo(owner, draft);
        if (updatePasscode) {
            SmartPhone.getPhoneSavedData().setPasscode(owner, credential);
        }
        syncChanges(editor.getServer(), owner, updatePasscode || blockedChanged);
        RPCPacketDistributor.rpcToPlayer(editor, S2CPayload.PHONE_EDITOR_RESULT, session, true);
    }

    /** 将已保存的管理员修改同步给手机主人和当前持有者。 */
    public static void syncChanges(MinecraftServer server, UUID owner, boolean resetAccess) {
        if (resetAccess) PhoneSecurityServer.refreshAfterAdminReset(server, owner);
        // Only send private data to its owner or players currently holding that phone.
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            boolean holdsPhone = isBoundPhone(viewer.getMainHandItem(), owner)
                    || isBoundPhone(viewer.getOffhandItem(), owner);
            if (owner.equals(viewer.getUUID()) || holdsPhone) {
                RPCPacketDistributor.rpcToPlayer(viewer, S2CPayload.PHONE_INFO_UPDATED, owner, com.smart.phone.security.PhoneSecurityServer.snapshot(viewer, owner));
            }
        }
    }

    private static boolean isBoundPhone(net.minecraft.world.item.ItemStack stack, UUID owner) {
        return stack.is(SmartPhoneRegistries.PHONE.get()) && owner.equals(stack.get(SmartPhoneRegistries.PHONE_OWNER.get()));
    }
}
