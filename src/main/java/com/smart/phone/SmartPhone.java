package com.smart.phone;

import com.mojang.logging.LogUtils;
import com.smart.phone.ui.data.PhoneSavedData;
import com.smart.phone.ui.data.chat.ChatRoomSavedData;
import com.smart.phone.ui.data.social.PhoneSocialSavedData;
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
import org.slf4j.Logger;

@Mod(SmartPhone.MOD_ID)
public class SmartPhone {
    public static final String MOD_ID = "smart_phone";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter
    @Getter
    private static PhoneSavedData phoneSavedData;
    @Setter
    @Getter
    private static ChatRoomSavedData chatRoomSavedData;
    @Setter
    @Getter
    private static PhoneSocialSavedData phoneSocialSavedData;

    public SmartPhone(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        SmartPhoneRegistries.ITEMS.register(modEventBus);
        SmartPhoneRegistries.CREATIVE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, "%s_config.toml".formatted(MOD_ID));
        if (dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
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
