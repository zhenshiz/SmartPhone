package com.smart.phone;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.data.IPhoneInfoData;
import com.smart.phone.ui.time.IPhoneTimeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class SmartPhoneRegistries {
    public static AutoRegistry.LDLibRegister<IApp, Supplier<IApp>> APPS;

    public static AutoRegistry.LDLibRegister<IPhoneInfoData, Supplier<IPhoneInfoData>> PHONE_INFO_DATA;

    public static AutoRegistry.LDLibRegister<IPhoneTimeSource, Supplier<IPhoneTimeSource>> PHONE_TIME_SOURCE;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartPhone.MOD_ID);

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SmartPhone.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PHONE_OWNER =
            DATA_COMPONENTS.registerComponentType("phone_owner", builder ->
                    builder.persistent(com.smart.phone.util.PhoneOwnerResolver.COMPONENT_CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PHONE_OWNER_NAME =
            DATA_COMPONENTS.registerComponentType("phone_owner_name", builder ->
                    builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static DeferredItem<Item> PHONE = ITEMS.register("phone", () -> new PhoneItem(new Item.Properties().stacksTo(1)));

    public static DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SmartPhone.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SMART_PHONE_TAB = CREATIVE_TABS.register("smart_phone", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.smartPhone"))
            .icon(() -> PHONE.get().getDefaultInstance())
            .displayItems((itemDisplayParameters, output) -> {
                output.accept(PHONE.get());
            }).build());

    public static Set<IApp> filterApp(Function<IApp, Boolean> screeningCondition) {
        Set<IApp> filterApps = new HashSet<>();
        APPS.forEach(iApp -> {
            IApp app = iApp.value().get();
            if (screeningCondition.apply(app)) {
                filterApps.add(app);
            }
        });
        return filterApps;
    }

    static {
        APPS = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IApp.ID), IApp.class, AutoRegistry::noArgsCreator);
        PHONE_INFO_DATA = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IPhoneInfoData.ID), IPhoneInfoData.class, AutoRegistry::noArgsCreator);
        PHONE_TIME_SOURCE = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IPhoneTimeSource.ID), IPhoneTimeSource.class, AutoRegistry::noArgsCreator);
    }
}
