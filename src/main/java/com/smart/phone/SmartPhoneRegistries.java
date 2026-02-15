package com.smart.phone;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.smart.phone.command.ICommand;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.time.IPhoneTimeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class SmartPhoneRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;

    public static AutoRegistry.LDLibRegister<IApp, Supplier<IApp>> APPS;

    public static AutoRegistry.LDLibRegister<IPhoneTimeSource, Supplier<IPhoneTimeSource>> PHONE_TIME_SOURCE;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartPhone.MOD_ID);

    public static DeferredItem<Item> PHONE = ITEMS.register("phone", () -> new PhoneItem(new Item.Properties()));

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
        COMMANDS = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(ICommand.COMMAND_ID), ICommand.class, AutoRegistry::noArgsCreator);
        APPS = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IApp.ID), IApp.class, AutoRegistry::noArgsCreator);
        PHONE_TIME_SOURCE = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IPhoneTimeSource.ID), IPhoneTimeSource.class, AutoRegistry::noArgsCreator);
    }
}
