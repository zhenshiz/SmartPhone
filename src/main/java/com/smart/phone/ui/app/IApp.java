package com.smart.phone.ui.app;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.view.HomeScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public abstract class IApp implements ILDLRegister<IApp, Supplier<IApp>>, IPersistedSerializable {
    public static final Codec<IApp> CODEC = SmartPhoneRegistries.APPS.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, IApp> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public static final String ID = SmartPhone.MOD_ID + ":app";

    // 唯一标识
    abstract public ResourceLocation getPhoneId();

    // 应用名
    abstract public Component getDisplayName();

    // 图标
    abstract public IGuiTexture getIcon();

    // 应用描述，显示在App Store里的 如果是系统应用就不需要了
    public Component getDescription() {
        return Component.empty();
    }

    // App的UI
    abstract public UIElement createAppUI(HomeScreen homeScreen);

    // 是否可以通过App Store下载
    public boolean isAppStoreInstall() {
        return true;
    }

    // 是否可以卸载
    public boolean isUninstall() {
        return true;
    }

    // 是否默认安装
    public boolean isDefaultInstalled() {
        return false;
    }

    // 在玩家点击图标准备打开 App 时调用，用来判断玩家是否允许打开该App
    public AppOpenResult canOpen() {
        return AppOpenResult.allow();
    }

    // 关闭应用时调用
    public void onClose() {
    }

    // 最小化时
    public void onMinimize() {
    }
}
