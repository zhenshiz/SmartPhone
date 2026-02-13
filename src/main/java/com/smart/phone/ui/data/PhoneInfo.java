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

@Data
public class PhoneInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, PhoneInfo> STREAM_CODEC;
    public static final Codec<PhoneInfo> CODEC;

    //setting
    @Configurable(name = "smartPhone.data.phoneInfo.phoneWallpaper")
    private ResourceLocation phoneWallpaper = SmartPhone.id("textures/ui/default_wallpaper.png");
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writePhoneTimeSource", deserializeMethod = "readPhoneTimeSource")
    private IPhoneTimeSource iPhoneTimeSource = new RealTimeSource();
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeInstalledApps", deserializeMethod = "readInstalledApps")
    private List<IApp> installedApps = new ArrayList<>();
    //ui state
    @Persisted
    private String[] notepadText = new String[]{""};

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        Configurator iPhoneTimeSourceConfigurator = iPhoneTimeSource.createConfigurator(this);
        father.addConfigurator(iPhoneTimeSourceConfigurator);
    }

    private CompoundTag writePhoneTimeSource(IPhoneTimeSource value) {
        return (CompoundTag) IPhoneTimeSource.CODEC.encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), value).result().orElse(new CompoundTag());
    }

    private IPhoneTimeSource readPhoneTimeSource(CompoundTag tag) {
        return IPhoneTimeSource.CODEC.decode(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow().getFirst();
    }

    private CompoundTag writeInstalledApps(List<IApp> value) {
        CompoundTag rootTag = new CompoundTag();
        Codec<List<IApp>> listCodec = IApp.CODEC.listOf();
        listCodec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(SmartPhone.LOGGER::error)
                .ifPresent(tag -> {
                    rootTag.put("apps", tag);
                });
        return rootTag;
    }

    private List<IApp> readInstalledApps(CompoundTag tag) {
        if (tag == null || !tag.contains("apps")) {
            return new ArrayList<>();
        }

        Codec<List<IApp>> listCodec = IApp.CODEC.listOf();

        List<IApp> immutableList = listCodec.parse(NbtOps.INSTANCE, tag.get("apps"))
                .resultOrPartial(SmartPhone.LOGGER::error)
                .orElseGet(ArrayList::new);

        return new ArrayList<>(immutableList);
    }

    {
        SmartPhoneRegistries.APPS.forEach(iApp -> {
            IApp app = iApp.value().get();
            if (app.isDefaultInstalled()) {
                installedApps.add(app);
            }
        });
        NeoForge.EVENT_BUS.post(new PhoneInfoInitEvent(this));
    }

    static {
        CODEC = PersistedParser.createCodec(PhoneInfo::new);
        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }
}
