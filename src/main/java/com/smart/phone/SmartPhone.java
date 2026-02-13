package com.smart.phone;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.logging.LogUtils;
import com.smart.phone.command.ICommand;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.data.PhoneSavedData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(SmartPhone.MOD_ID)
public class SmartPhone {
    public static final String MOD_ID = "smart_phone";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter
    @Getter
    private static PhoneSavedData phoneSavedData;

    public SmartPhone(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        AccessorRegistries.setPriority(0);
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(PhoneInfo.class)
                .codec(PhoneInfo.CODEC)
                .streamCodec(PhoneInfo.STREAM_CODEC)
                .codecMark()
                .build()
        );
        if (dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, "%s_config.toml".formatted(MOD_ID));
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    //注册指令
    private void onRegisterCommands(RegisterCommandsEvent event) {
        for (AutoRegistry.Holder<LDLRegister, ICommand, Supplier<ICommand>> command : SmartPhoneRegistries.COMMANDS) {
            command.value().get().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }

    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}