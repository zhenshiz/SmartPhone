package com.smart.phone;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.smart.phone.command.ICommand;
import com.smart.phone.ui.app.IApp;
import com.smart.phone.ui.time.IPhoneTimeSource;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class SmartPhoneRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;

    public static AutoRegistry.LDLibRegister<IApp, Supplier<IApp>> APPS;

    public static AutoRegistry.LDLibRegister<IPhoneTimeSource, Supplier<IPhoneTimeSource>> PHONE_TIME_SOURCE;

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
