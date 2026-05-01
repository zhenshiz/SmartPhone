package com.smart.phone.compat.voicechat;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class CallSession {
    private final UUID sessionId;
    private final UUID callerUuid;
    private final UUID calleeUuid;
    private final String callerName;
    private final String calleeName;
    private final long createdAtMillis;
    @Setter
    private CallState state = CallState.CALLING;
    @Setter
    private UUID voiceGroupId;

    public CallSession(UUID sessionId, UUID callerUuid, UUID calleeUuid, String callerName, String calleeName) {
        this.sessionId = sessionId;
        this.callerUuid = callerUuid;
        this.calleeUuid = calleeUuid;
        this.callerName = callerName;
        this.calleeName = calleeName;
        this.createdAtMillis = System.currentTimeMillis();
    }

    public UUID getOtherPlayer(UUID playerUuid) {
        if (callerUuid.equals(playerUuid)) {
            return calleeUuid;
        }
        if (calleeUuid.equals(playerUuid)) {
            return callerUuid;
        }
        return null;
    }

    public String getOtherPlayerName(UUID playerUuid) {
        if (callerUuid.equals(playerUuid)) {
            return calleeName;
        }
        if (calleeUuid.equals(playerUuid)) {
            return callerName;
        }
        return "";
    }

    public boolean contains(UUID playerUuid) {
        return callerUuid.equals(playerUuid) || calleeUuid.equals(playerUuid);
    }
}
