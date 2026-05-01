package com.smart.phone.client.call;

import com.smart.phone.compat.voicechat.CallState;
import com.smart.phone.compat.voicechat.CallStatusKeys;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhoneCallClientState {
    @Getter
    private static UUID sessionId;
    @Getter
    private static UUID remoteUuid;
    @Getter
    private static String remoteName = "";
    @Getter
    private static CallState state = CallState.ENDED;
    @Getter
    private static String messageKey = "";
    @Getter
    private static int version;
    @Getter
    private static int statusVersion;
    private static final Map<UUID, String> playerStatuses = new HashMap<>();
    private static String encodedStatuses = "";

    public static void ringing(UUID newSessionId, UUID calleeUuid, String calleeName) {
        update(newSessionId, calleeUuid, calleeName, CallState.CALLING, "smartPhone.ui.app.phoneCall.status.calling");
    }

    public static void incoming(UUID newSessionId, UUID callerUuid, String callerName) {
        update(newSessionId, callerUuid, callerName, CallState.CALLING, "smartPhone.ui.app.phoneCall.status.incoming");
    }

    public static void connected(UUID newSessionId, UUID otherUuid, String otherName) {
        update(newSessionId, otherUuid, otherName, CallState.CONNECTED, "smartPhone.ui.app.phoneCall.status.connected");
    }

    public static void ended(UUID endedSessionId, String reasonKey) {
        if (sessionId == null || endedSessionId == null || sessionId.equals(endedSessionId)) {
            update(endedSessionId, remoteUuid, remoteName, CallState.ENDED, reasonKey);
        }
    }

    public static void error(String errorKey) {
        update(sessionId, remoteUuid, remoteName, state, errorKey);
    }

    public static void clear() {
        update(null, null, "", CallState.ENDED, "");
    }

    public static boolean hasActiveSession() {
        return sessionId != null && state != CallState.ENDED;
    }

    public static boolean hasIncomingCall() {
        return sessionId != null && state == CallState.CALLING && "smartPhone.ui.app.phoneCall.status.incoming".equals(messageKey);
    }

    public static Component message() {
        return messageKey == null || messageKey.isBlank() ? Component.empty() : Component.translatable(messageKey);
    }

    public static String getPlayerStatus(UUID playerUuid) {
        return playerStatuses.getOrDefault(playerUuid, CallStatusKeys.UNKNOWN);
    }

    public static Map<UUID, String> getPlayerStatuses() {
        return Collections.unmodifiableMap(playerStatuses);
    }

    public static void updatePlayerStatuses(String encoded) {
        String normalized = encoded == null ? "" : encoded;
        if (encodedStatuses.equals(normalized)) return;
        encodedStatuses = normalized;
        playerStatuses.clear();
        if (!normalized.isBlank()) {
            for (String entry : normalized.split(";")) {
                int separator = entry.indexOf('=');
                if (separator <= 0 || separator >= entry.length() - 1) continue;
                try {
                    playerStatuses.put(UUID.fromString(entry.substring(0, separator)), entry.substring(separator + 1));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        statusVersion++;
    }

    private static void update(UUID newSessionId, UUID newRemoteUuid, String newRemoteName, CallState newState, String newMessageKey) {
        sessionId = newSessionId;
        remoteUuid = newRemoteUuid;
        remoteName = newRemoteName == null ? "" : newRemoteName;
        state = newState == null ? CallState.ENDED : newState;
        messageKey = newMessageKey == null ? "" : newMessageKey;
        version++;
    }
}
