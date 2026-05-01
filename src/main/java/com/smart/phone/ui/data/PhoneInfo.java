package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.event.neoforge.PhoneInfoInitEvent;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.time.IPhoneTimeSource;
import com.smart.phone.ui.time.RealTimeSource;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class PhoneInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, PhoneInfo> STREAM_CODEC;
    public static final Codec<PhoneInfo> CODEC;

    //setting
    @Configurable(name = "smartPhone.data.phoneInfo.phoneWallpaper")
    private ResourceLocation phoneWallpaper = SmartPhone.id("textures/ui/default_wallpaper.png");
    @Persisted
    private IPhoneTimeSource iPhoneTimeSource = new RealTimeSource();
    //ui state
    @Persisted
    private List<IApp> installedApps = new ArrayList<>();
    @Persisted
    private List<IPhoneInfoData> extensionData = new ArrayList<>();

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        Configurator iPhoneTimeSourceConfigurator = iPhoneTimeSource.createConfigurator(this);
        father.addConfigurator(iPhoneTimeSourceConfigurator);
    }

    public <T extends IPhoneInfoData> Optional<T> findExtensionData(Class<T> type) {
        for (IPhoneInfoData data : extensionData) {
            if (type.isInstance(data)) {
                return Optional.of(type.cast(data));
            }
        }
        return Optional.empty();
    }

    public <T extends IPhoneInfoData> T getOrCreateExtensionData(Class<T> type) {
        Optional<T> existing = findExtensionData(type);
        if (existing.isPresent()) {
            return existing.get();
        }

        IPhoneInfoData created = createExtensionDataInstance(type);
        if (created == null) {
            throw new IllegalArgumentException("Unregistered phone info data type: " + type.getName());
        }
        if (!type.isInstance(created)) {
            throw new IllegalArgumentException("Phone info data type mismatch: " + created.getClass().getName() + " != " + type.getName());
        }

        extensionData.add(created);
        return type.cast(created);
    }

    private IPhoneInfoData createExtensionDataInstance(Class<?> type) {
        for (var holder : SmartPhoneRegistries.PHONE_INFO_DATA) {
            IPhoneInfoData data = holder.value().get();
            if (type.isInstance(data)) {
                return data;
            }
        }
        return null;
    }

    public void ensureDefaultContent() {
        SmartPhoneRegistries.APPS.forEach(iApp -> {
            IApp app = iApp.value().get();
            if (app.isDefaultInstalled() && installedApps.stream().noneMatch(installedApp -> installedApp.name().equals(app.name()))) {
                installedApps.add(app);
            }
        });
        SmartPhoneRegistries.PHONE_INFO_DATA.forEach(holder -> {
            IPhoneInfoData data = holder.value().get();
            if (data.isDefaultCreated() && findExtensionData(data.getClass()).isEmpty()) {
                extensionData.add(data);
            }
        });
    }

    {
        ensureDefaultContent();
        NeoForge.EVENT_BUS.post(new PhoneInfoInitEvent(this));
    }

    static {
        CODEC = PersistedParser.createCodec(PhoneInfo::new);
        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }
}
