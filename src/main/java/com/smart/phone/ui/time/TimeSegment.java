package com.smart.phone.ui.time;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimeSegment implements IPersistedSerializable, IConfigurable {
    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.startHour")
    public int startHour = 0;
    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.startMin")
    public int startMin = 0;

    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.endHour")
    public int endHour = 0;
    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.endMin")
    public int endMin = 0;

    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.durationTicks")
    public int durationTicks = 0;
    @Configurable(name = "smartPhone.data.phoneTimeSource.customTimeSource.timeSegment.freezeTime")
    public boolean freezeTime = false;

    public int getStartTotalMinutes() {
        return startHour * 60 + startMin;
    }

    public int getEndTotalMinutes() {
        return endHour * 60 + endMin;
    }
}
