package com.smart.phone.ui.time;

import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.smart.phone.SmartPhone;
import lombok.Getter;
import net.minecraft.nbt.IntTag;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = CustomTimeSource.CUSTOM_TIME_SOURCE, registry = IPhoneTimeSource.ID)
public class CustomTimeSource extends IPhoneTimeSource {
    public static final String CUSTOM_TIME_SOURCE = SmartPhone.MOD_ID + ":custom_time_source";

    // 存储所有的自定义时间片段
    @Getter
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeTimeSegments", deserializeMethod = "readTimeSegments")
    private final List<TimeSegment> segments = new ArrayList<>();

    // 当前运行状态
    private int currentSegmentIndex = 0;
    private int currentTickInSegment = 0;

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        ArrayConfiguratorGroup<TimeSegment> segmentArrayConfiguratorGroup = new ArrayConfiguratorGroup<>("smartPhone.data.phoneTimeSource.customTimeSource.segments", true,
                () -> new ArrayList<>(this.getSegments()),
                (getter, setter) -> {
                    TimeSegment instance = getter.get();
                    return instance != null ? instance.createDirectConfigurator() : new Configurator();
                }, true);
        segmentArrayConfiguratorGroup.setAddDefault(TimeSegment::new);
        segmentArrayConfiguratorGroup.setOnUpdate(list -> {
            List<TimeSegment> origin = this.getSegments();
            origin.clear();
            origin.addAll(list);
        });
        father.addConfigurator(segmentArrayConfiguratorGroup);
    }

    public void addSegment(TimeSegment segment) {
        this.segments.add(segment);
    }

    public void clearSegments() {
        this.segments.clear();
        this.currentSegmentIndex = 0;
        this.currentTickInSegment = 0;
    }

    @Override
    public String getDisplayName() {
        return "smartPhone.data.phoneTimeSource.type.customTime";
    }

    @Override
    public void tick() {
        if (segments.isEmpty()) return;

        TimeSegment current = segments.get(currentSegmentIndex);

        // 如果是冻结时间，且 durationTicks 设置为 -1 (代表无限冻结)，就不增加计数
        if (current.freezeTime && current.durationTicks < 0) {
            updateCache(current.startHour, current.startMin);
            return;
        }

        currentTickInSegment++;

        // 检查当前片段是否结束
        if (currentTickInSegment >= current.durationTicks) {
            // 切换到下一阶段
            nextSegment();
        } else {
            // 还没结束，计算当前时间
            calculateCurrentTime(current);
        }
    }

    private void nextSegment() {
        currentTickInSegment = 0;
        currentSegmentIndex++;

        // 如果跑完了所有片段
        if (currentSegmentIndex >= segments.size()) {
            currentSegmentIndex = 0;
        }

        // 切换瞬间更新一下时间
        if (!segments.isEmpty()) {
            TimeSegment next = segments.get(currentSegmentIndex);
            updateCache(next.startHour, next.startMin);
        }
    }

    private void calculateCurrentTime(TimeSegment seg) {
        if (seg.freezeTime) {
            updateCache(seg.startHour, seg.startMin);
            return;
        }

        int startTotal = seg.getStartTotalMinutes();
        int endTotal = seg.getEndTotalMinutes();

        if (endTotal < startTotal) {
            endTotal += 24 * 60;
        }

        int durationMinutes = endTotal - startTotal;

        double progress = (double) currentTickInSegment / (double) seg.durationTicks;

        int currentVirtualTotal = (int) (startTotal + (durationMinutes * progress));

        currentVirtualTotal = currentVirtualTotal % (24 * 60);

        int h = currentVirtualTotal / 60;
        int m = currentVirtualTotal % 60;

        updateCache(h, m);
    }

    private void updateCache(int h, int m) {
        this.hour = h;
        this.minute = m;
    }

    private IntTag writeTimeSegments(List<TimeSegment> value) {
        return IntTag.valueOf(value.size());
    }

    private List<TimeSegment> readTimeSegments(IntTag tag) {
        var groups = new ArrayList<TimeSegment>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            groups.add(new TimeSegment());
        }
        return groups;
    }
}
