package com.smart.phone;

import com.smart.phone.util.SmartPhoneServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class PhoneItem extends Item {

    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            SmartPhoneServerUtil.openPhone(serverPlayer, player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    public static UUID ownerOf(ItemStack stack, UUID fallback) {
        UUID owner = stack.get(SmartPhoneRegistries.PHONE_OWNER.get());
        return owner == null ? fallback : owner;
    }

    public static UUID bindIfUnbound(ItemStack stack, ServerPlayer holder) {
        UUID owner = stack.get(SmartPhoneRegistries.PHONE_OWNER.get());
        if (owner == null) {
            owner = holder.getUUID();
            stack.set(SmartPhoneRegistries.PHONE_OWNER.get(), owner);
        }
        updateOwnerName(stack, holder.getServer());
        return owner;
    }

    public static void updateOwnerName(ItemStack stack, MinecraftServer server) {
        UUID owner = stack.get(SmartPhoneRegistries.PHONE_OWNER.get());
        if (owner == null || server == null) return;
        String name = com.smart.phone.util.PhoneOwnerResolver.name(server, owner);
        if (name != null && !name.equals(stack.get(SmartPhoneRegistries.PHONE_OWNER_NAME.get()))) {
            stack.set(SmartPhoneRegistries.PHONE_OWNER_NAME.get(), name);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof ServerPlayer player && stack.has(SmartPhoneRegistries.PHONE_OWNER.get())) {
            updateOwnerName(stack, player.getServer());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UUID owner = stack.get(SmartPhoneRegistries.PHONE_OWNER.get());
        String ownerName = stack.get(SmartPhoneRegistries.PHONE_OWNER_NAME.get());
        tooltip.add(owner == null
                ? Component.translatable("smartPhone.item.phone.unbound")
                : Component.translatable("smartPhone.item.phone.owner",
                        ownerName == null ? Component.translatable("smartPhone.item.phone.unknownOwner") : ownerName));
    }
}
