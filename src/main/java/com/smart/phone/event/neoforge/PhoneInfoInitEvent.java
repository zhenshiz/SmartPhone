package com.smart.phone.event.neoforge;

import com.smart.phone.ui.data.PhoneInfo;
import lombok.Getter;
import net.neoforged.bus.api.Event;

@Getter
public class PhoneInfoInitEvent extends Event {
    private final PhoneInfo phoneInfo;

    public PhoneInfoInitEvent(PhoneInfo phoneInfo) {
        this.phoneInfo = phoneInfo;
    }
}
