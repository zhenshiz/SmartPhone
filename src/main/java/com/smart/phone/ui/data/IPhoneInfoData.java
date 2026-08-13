package com.smart.phone.ui.data;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Supplier;
public abstract class IPhoneInfoData implements ILDLRegister<IPhoneInfoData, Supplier<IPhoneInfoData>>, IPersistedSerializable {
    public static final String ID = SmartPhone.MOD_ID + ":phone_info_data";

    public static final Codec<IPhoneInfoData> CODEC = SmartPhoneRegistries.PHONE_INFO_DATA.optionalCodec().dispatch(
            ILDLRegister::getRegistryHolderOptional,
            optional -> optional
                    .map(holder -> PersistedParser.createMapCodec(holder.value()))
                    .orElseGet(LDLibExtraCodecs::errorDecoder)
    );

    public static final StreamCodec<ByteBuf, IPhoneInfoData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /**
     * 是否在新的 PhoneInfo 创建时自动加入。
     * 对已有存档是否补全由调用方自行决定（建议在需要时 getOrCreate）。
     */
    public boolean isDefaultCreated() {
        return false;
    }
}
