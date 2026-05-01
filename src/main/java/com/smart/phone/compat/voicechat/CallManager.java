package com.smart.phone.compat.voicechat;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.smart.phone.SmartPhone;
import com.smart.phone.network.s2c.S2CPayload;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {
    private static final CallManager INSTANCE = new CallManager();
    private static final long RING_TIMEOUT_MILLIS = 60_000L;

    private final Map<UUID, CallSession> activeCalls = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToSession = new ConcurrentHashMap<>();
    private MinecraftServer server;
    @Getter
    private VoicechatServerApi voicechatApi;

    public static CallManager getInstance() {
        return INSTANCE;
    }

    public void setVoicechatApi(VoicechatServerApi voicechatApi) {
        this.voicechatApi = voicechatApi;
        broadcastStatus();
    }

    public void dial(ServerPlayer caller, UUID calleeUuid) {
        if (caller == null || calleeUuid == null) return;
        server = caller.server;
        ServerPlayer callee = caller.server.getPlayerList().getPlayer(calleeUuid);
        if (callee == null) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.playerOffline");
            return;
        }
        if (caller.getUUID().equals(callee.getUUID())) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.self");
            return;
        }
        if (isPlayerBusy(caller.getUUID())) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.youBusy");
            return;
        }
        if (isPlayerBusy(callee.getUUID())) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.targetBusy");
            return;
        }
        if (!hasUsableVoiceConnection(caller.getUUID())) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.yourVoiceUnavailable");
            return;
        }
        if (!hasUsableVoiceConnection(callee.getUUID())) {
            sendError(caller, "smartPhone.ui.app.phoneCall.error.targetVoiceUnavailable");
            return;
        }

        UUID sessionId = UUID.randomUUID();
        CallSession session = new CallSession(sessionId, caller.getUUID(), callee.getUUID(), caller.getGameProfile().getName(), callee.getGameProfile().getName());
        activeCalls.put(sessionId, session);
        playerToSession.put(caller.getUUID(), sessionId);
        playerToSession.put(callee.getUUID(), sessionId);

        RPCPacketDistributor.rpcToPlayer(caller, S2CPayload.CALL_RINGING, sessionId, callee.getUUID(), session.getCalleeName());
        RPCPacketDistributor.rpcToPlayer(callee, S2CPayload.CALL_INCOMING, SmartPhone.getPhoneSavedData().getPhoneInfo(callee), sessionId, caller.getUUID(), session.getCallerName());
        broadcastStatus();
    }

    public void answer(ServerPlayer callee, UUID sessionId) {
        CallSession session = getValidSession(callee, sessionId);
        if (session == null) return;
        if (!session.getCalleeUuid().equals(callee.getUUID())) {
            sendError(callee, "smartPhone.ui.app.phoneCall.error.notCallee");
            return;
        }
        if (session.getState() != CallState.CALLING) {
            sendError(callee, "smartPhone.ui.app.phoneCall.error.invalidState");
            return;
        }

        ServerPlayer caller = callee.server.getPlayerList().getPlayer(session.getCallerUuid());
        if (caller == null) {
            endSession(session, "smartPhone.ui.app.phoneCall.ended.callerOffline", true);
            return;
        }

        VoicechatConnection callerConnection = getUsableConnection(caller.getUUID());
        VoicechatConnection calleeConnection = getUsableConnection(callee.getUUID());
        if (callerConnection == null || calleeConnection == null) {
            endSession(session, "smartPhone.ui.app.phoneCall.ended.voiceUnavailable", true);
            return;
        }

        Group group;
        try {
            group = voicechatApi.groupBuilder()
                    .setId(session.getSessionId())
                    .setName("smart_phone_call_" + session.getSessionId())
                    .setHidden(true)
                    .setPersistent(false)
                    .setType(Group.Type.ISOLATED)
                    .build();
        } catch (Exception exception) {
            SmartPhone.LOGGER.error("Failed to create SmartPhone call group", exception);
            endSession(session, "smartPhone.ui.app.phoneCall.ended.groupFailed", true);
            return;
        }

        try {
            callerConnection.setGroup(group);
            calleeConnection.setGroup(group);
        } catch (Exception exception) {
            SmartPhone.LOGGER.error("Failed to join SmartPhone call group", exception);
            callerConnection.setGroup(null);
            calleeConnection.setGroup(null);
            endSession(session, "smartPhone.ui.app.phoneCall.ended.groupFailed", true);
            return;
        }

        session.setVoiceGroupId(group.getId());
        session.setState(CallState.CONNECTED);
        notifyConnected(caller, session);
        notifyConnected(callee, session);
        broadcastStatus();
    }

    public void reject(ServerPlayer callee, UUID sessionId) {
        CallSession session = getValidSession(callee, sessionId);
        if (session == null) return;
        if (!session.getCalleeUuid().equals(callee.getUUID())) {
            sendError(callee, "smartPhone.ui.app.phoneCall.error.notCallee");
            return;
        }
        endSession(session, "smartPhone.ui.app.phoneCall.ended.rejected", true);
    }

    public void hangup(ServerPlayer player) {
        if (player == null) return;
        UUID sessionId = playerToSession.get(player.getUUID());
        if (sessionId == null) {
            sendError(player, "smartPhone.ui.app.phoneCall.error.noActiveCall");
            return;
        }
        CallSession session = activeCalls.get(sessionId);
        if (session == null) {
            playerToSession.remove(player.getUUID());
            sendError(player, "smartPhone.ui.app.phoneCall.error.noActiveCall");
            return;
        }
        endSession(session, "smartPhone.ui.app.phoneCall.ended.hangup", true);
    }

    public void onPlayerDisconnected(ServerPlayer player) {
        if (player == null) return;
        UUID sessionId = playerToSession.get(player.getUUID());
        if (sessionId == null) return;
        CallSession session = activeCalls.get(sessionId);
        if (session != null) {
            endSession(session, "smartPhone.ui.app.phoneCall.ended.playerOffline", true);
        } else {
            playerToSession.remove(player.getUUID());
        }
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        long now = System.currentTimeMillis();
        for (CallSession session : activeCalls.values()) {
            if (session.getState() == CallState.CALLING && now - session.getCreatedAtMillis() > RING_TIMEOUT_MILLIS) {
                endSession(session, "smartPhone.ui.app.phoneCall.ended.timeout", true);
            } else if (session.getState() == CallState.CONNECTED && !isCallGroupIntact(session)) {
                endSession(session, "smartPhone.ui.app.phoneCall.ended.voiceGroupLost", true);
            }
        }
    }

    public void cleanupAll(String reasonKey, boolean notifyPlayers) {
        for (CallSession session : activeCalls.values()) {
            endSession(session, reasonKey, notifyPlayers);
        }
        activeCalls.clear();
        playerToSession.clear();
        broadcastStatus();
    }

    public void sendStatus(ServerPlayer viewer) {
        if (viewer == null) return;
        server = viewer.server;
        StringJoiner joiner = new StringJoiner(";");
        UUID viewerUuid = viewer.getUUID();
        for (ServerPlayer player : viewer.server.getPlayerList().getPlayers()) {
            UUID playerUuid = player.getUUID();
            if (viewerUuid.equals(playerUuid)) continue;
            joiner.add(playerUuid + "=" + statusKey(playerUuid));
        }
        RPCPacketDistributor.rpcToPlayer(viewer, S2CPayload.CALL_STATUS_UPDATE, joiner.toString());
    }

    private boolean isPlayerBusy(UUID playerUuid) {
        return playerToSession.containsKey(playerUuid);
    }

    private String statusKey(UUID playerUuid) {
        if (isPlayerBusy(playerUuid)) return CallStatusKeys.BUSY;
        if (!hasUsableVoiceConnection(playerUuid)) return CallStatusKeys.VOICE_UNAVAILABLE;
        return CallStatusKeys.AVAILABLE;
    }

    private void broadcastStatus() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendStatus(player);
        }
    }

    private CallSession getValidSession(ServerPlayer player, UUID sessionId) {
        if (player == null || sessionId == null) return null;
        CallSession session = activeCalls.get(sessionId);
        if (session == null || !session.contains(player.getUUID())) {
            sendError(player, "smartPhone.ui.app.phoneCall.error.sessionMissing");
            return null;
        }
        return session;
    }

    private boolean hasUsableVoiceConnection(UUID playerUuid) {
        return getUsableConnection(playerUuid) != null;
    }

    private VoicechatConnection getUsableConnection(UUID playerUuid) {
        if (voicechatApi == null) return null;
        VoicechatConnection connection = voicechatApi.getConnectionOf(playerUuid);
        if (connection == null) return null;
        if (!connection.isInstalled() || !connection.isConnected() || connection.isDisabled()) return null;
        return connection;
    }

    private boolean isCallGroupIntact(CallSession session) {
        UUID groupId = session.getVoiceGroupId();
        if (groupId == null || voicechatApi == null) return false;
        return isPlayerInGroup(session.getCallerUuid(), groupId) && isPlayerInGroup(session.getCalleeUuid(), groupId);
    }

    private boolean isPlayerInGroup(UUID playerUuid, UUID expectedGroupId) {
        VoicechatConnection connection = getUsableConnection(playerUuid);
        if (connection == null || !connection.isInGroup() || connection.getGroup() == null) return false;
        return expectedGroupId.equals(connection.getGroup().getId());
    }

    private void endSession(CallSession session, String reasonKey, boolean notifyPlayers) {
        if (session == null) return;
        if (!activeCalls.remove(session.getSessionId(), session)) return;
        session.setState(CallState.ENDED);
        playerToSession.remove(session.getCallerUuid(), session.getSessionId());
        playerToSession.remove(session.getCalleeUuid(), session.getSessionId());

        leaveGroup(session.getCallerUuid());
        leaveGroup(session.getCalleeUuid());
        if (voicechatApi != null && session.getVoiceGroupId() != null) {
            try {
                voicechatApi.removeGroup(session.getVoiceGroupId());
            } catch (Exception exception) {
                SmartPhone.LOGGER.debug("Failed to remove SmartPhone call group {}", session.getVoiceGroupId(), exception);
            }
        }

        if (notifyPlayers) {
            notifyEnded(session, reasonKey);
        }
        broadcastStatus();
    }

    private void leaveGroup(UUID playerUuid) {
        if (voicechatApi == null) return;
        VoicechatConnection connection = voicechatApi.getConnectionOf(playerUuid);
        if (connection != null) {
            connection.setGroup(null);
        }
    }

    private void notifyConnected(ServerPlayer player, CallSession session) {
        UUID other = session.getOtherPlayer(player.getUUID());
        String otherName = session.getOtherPlayerName(player.getUUID());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CALL_CONNECTED, session.getSessionId(), other, otherName);
    }

    private void notifyEnded(CallSession session, String reasonKey) {
        notifyEnded(session.getSessionId(), session.getCallerUuid(), reasonKey);
        notifyEnded(session.getSessionId(), session.getCalleeUuid(), reasonKey);
    }

    private void notifyEnded(UUID sessionId, UUID playerUuid, String reasonKey) {
        ServerPlayer player = findOnlinePlayer(playerUuid);
        if (player != null) {
            RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CALL_ENDED, sessionId, reasonKey);
        }
    }

    private ServerPlayer findOnlinePlayer(UUID playerUuid) {
        return server == null ? null : server.getPlayerList().getPlayer(playerUuid);
    }

    private void sendError(ServerPlayer player, String messageKey) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.CALL_ERROR, messageKey);
    }
}
