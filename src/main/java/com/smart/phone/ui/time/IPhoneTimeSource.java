package com.smart.phone.ui.time;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.mojang.serialization.Codec;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.common.BeanUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.appliedenergistics.yoga.YogaDisplay;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public abstract class IPhoneTimeSource implements ILDLRegister<IPhoneTimeSource, Supplier<IPhoneTimeSource>>, IPersistedSerializable, IConfigurable {
    public static final Codec<IPhoneTimeSource> CODEC = SmartPhoneRegistries.PHONE_TIME_SOURCE.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, IPhoneTimeSource> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static final String ID = SmartPhone.MOD_ID + ":phone_time_source";

    public int hour = 0;

    public int minute = 0;

    public Configurator createConfigurator(PhoneInfo phoneInfo) {
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);

        Set<IPhoneTimeSource> types = new HashSet<>();
        SmartPhoneRegistries.PHONE_TIME_SOURCE.forEach(iPhoneTimeSource -> types.add(iPhoneTimeSource.value().get()));
        ConfiguratorGroup configuratorGroup = new ConfiguratorGroup();
        configuratorGroup.setCollapse(false);
        configuratorGroup.setCanCollapse(false);
        configuratorGroup.setCollapse(false);
        configuratorGroup.lineContainer.setDisplay(YogaDisplay.NONE);
        SearchComponentConfigurator<IPhoneTimeSource> typeConfigurator = new SearchComponentConfigurator<>("smartPhone.data.phoneTimeSource.customTimeSource.segments",
                phoneInfo::getIPhoneTimeSource,
                phoneInfo::setIPhoneTimeSource,
                BeanUtil.getValueOrDefault(phoneInfo.getIPhoneTimeSource(), new RealTimeSource()),
                false,
                (word, searchHandler) -> {
                    String lowerWord = word.toLowerCase();
                    for (var key : types) {
                        if (Thread.currentThread().isInterrupted()) return;
                        String displayName = Component.translatable(key.getDisplayName()).getString();
                        if (displayName.contains(lowerWord) || key.name().contains(lowerWord)) {
                            ((IResultHandler<IPhoneTimeSource>) searchHandler).acceptResult(key);
                        }
                    }
                },
                (value) -> Component.translatable(value.getDisplayName()).getString(),
                value -> new Label().setText(Component.translatable(value.getDisplayName())).textStyle(textStyle -> textStyle.fontSize(6).adaptiveHeight(true))
        );
        typeConfigurator.searchComponent.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            configuratorGroup.removeAllConfigurators();
            configuratorGroup.addConfigurators(phoneInfo.getIPhoneTimeSource().createDirectConfigurator());
        });
        group.addConfigurators(typeConfigurator, configuratorGroup);
        return group;
    }

    // 补全输入框中显示的文本
    abstract String getDisplayName();

    // 获取当前手机显示的小时 (0-23)
    public String getHour() {
        return String.format("%02d", hour);
    }

    // 获取当前手机显示的分钟 (0-59)
    public String getMinute() {
        return String.format("%02d", minute);
    }

    // 每tick更新逻辑
    abstract public void tick();
}
