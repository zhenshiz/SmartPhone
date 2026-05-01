package com.smart.phone.client.message;

import com.smart.phone.ui.data.OfficialMessage;
import lombok.Getter;

public class PhoneMessageClientState {
    private static final long NOTIFICATION_DURATION_MILLIS = 5_000L;

    @Getter
    private static OfficialMessage notification;
    private static long notificationUntilMillis;

    public static void receive(OfficialMessage message) {
        notification = message;
        notificationUntilMillis = System.currentTimeMillis() + NOTIFICATION_DURATION_MILLIS;
    }

    public static boolean hasVisibleNotification() {
        return notification != null && System.currentTimeMillis() <= notificationUntilMillis;
    }
}
