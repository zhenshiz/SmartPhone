package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.mojang.authlib.GameProfile;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.network.c2s.C2SPayload;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@LDLRegisterClient(
        name = "phone_owner",
        group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY
)
public final class PhoneOwnerScenario implements UIScenario {
    private static final UUID OWNER = UUID.nameUUIDFromBytes("ldtest-phone-owner".getBytes(StandardCharsets.UTF_8));
    private static final String OWNER_NAME = "TestOwner";
    private static final UUID UNBOUND_TARGET = UUID.nameUUIDFromBytes("ldtest-unbound-target".getBytes(StandardCharsets.UTF_8));
    private static final ResourceLocation DUMMY_WALLPAPER = SmartPhone.id("textures/ui/banner.png");
    private static final ResourceLocation SAVED_WALLPAPER = SmartPhone.id("textures/ui/unlock.png");

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("ui", "phone", "persistence");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        ItemStack invalidPhone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        invalidPhone.set(SmartPhoneRegistries.PHONE_OWNER.get(), UNBOUND_TARGET);
        ItemStack legacyPhone = SmartPhoneRegistries.PHONE.get().getDefaultInstance();
        legacyPhone.set(SmartPhoneRegistries.PHONE_OWNER.get(), OWNER);
        scenario
                .server("create another player's phone data", context -> {
                    PhoneInfo info = new PhoneInfo();
                    info.setPhoneWallpaper(DUMMY_WALLPAPER);
                    SmartPhone.getPhoneSavedData().setPhoneInfo(OWNER, info);
                    context.server().getProfileCache().add(new GameProfile(OWNER, OWNER_NAME));
                })
                .setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                .awaitScreen(HeldPhoneScreen.class)
                .waitUntil("new phone binds to its first holder", context ->
                        context.screen() instanceof HeldPhoneScreen screen && screen.isSynced())
                .checkServer("unbound phone is assigned the holder UUID", context ->
                        context.player().getUUID().equals(context.player().getMainHandItem()
                                .get(SmartPhoneRegistries.PHONE_OWNER.get())))
                .check("first holder's player ID appears in the tooltip", context ->
                        context.mc().player.getMainHandItem()
                                .getTooltipLines(Item.TooltipContext.of(context.mc().level),
                                        context.mc().player, TooltipFlag.NORMAL)
                                .stream().anyMatch(line -> line.getString().contains(context.mc().player.getGameProfile().getName())))
                .server("bind the held phone to another player's saved UUID", context ->
                        context.server().getCommands().performPrefixedCommand(
                                context.player().createCommandSourceStack().withPermission(2),
                                "smart_phone bind " + OWNER))
                .waitUntilServer("item component points to the target UUID", context ->
                        OWNER.equals(context.player().getMainHandItem()
                                .get(SmartPhoneRegistries.PHONE_OWNER.get())))
                .waitUntil("bound phone data arrives", context ->
                        context.screen() instanceof HeldPhoneScreen screen
                                && OWNER.equals(screen.getPhoneUI().getOwnerUuid()))
                .check("bound phone tooltip renders the player ID instead of UUID", context ->
                        context.mc().player.getMainHandItem()
                                .getTooltipLines(Item.TooltipContext.of(context.mc().level),
                                        context.mc().player, TooltipFlag.NORMAL)
                                .stream().anyMatch(line -> line.getString().contains(OWNER_NAME)
                                        && !line.getString().contains(OWNER.toString())))
                .check("bound phone opens the target UUID's data", context -> {
                    if (!(context.screen() instanceof HeldPhoneScreen screen)) return false;
                    return OWNER.equals(screen.getPhoneUI().getOwnerUuid())
                            && DUMMY_WALLPAPER.equals(screen.getPhoneUI().phoneInfo.getPhoneWallpaper());
                })
                .step("save an edit through the normal phone RPC", context -> {
                    HeldPhoneScreen screen = (HeldPhoneScreen) context.screen();
                    screen.getPhoneUI().phoneInfo.setPhoneWallpaper(SAVED_WALLPAPER);
                    SmartPhoneClientUtil.setPhoneInfoByPlayer(screen.getPhoneUI().phoneInfo);
                })
                .waitUntilServer("target UUID receives the saved edit", context ->
                        SAVED_WALLPAPER.equals(SmartPhone.getPhoneSavedData()
                                .getPhoneInfo(OWNER).getPhoneWallpaper()))
                .checkServer("holder's own phone data was not overwritten", context ->
                        !SAVED_WALLPAPER.equals(SmartPhone.getPhoneSavedData()
                                .getPhoneInfo(context.player()).getPhoneWallpaper()))
                .step("try to save to a UUID with no matching phone", context ->
                        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_PHONE_INFO,
                                UNBOUND_TARGET, new PhoneInfo()))
                .serverTicks(2)
                .checkServer("unbound UUID was not created", context ->
                        !SmartPhone.getPhoneSavedData().hasPhoneInfo(UNBOUND_TARGET))
                .setHeldItem(ItemStack.EMPTY)
                .waitUntil("previous phone closes", context -> context.screen() == null)
                .setHeldItem(legacyPhone)
                .waitUntilServer("existing UUID-only phone receives its player ID", context ->
                        OWNER_NAME.equals(context.player().getMainHandItem()
                                .get(SmartPhoneRegistries.PHONE_OWNER_NAME.get())))
                .check("existing phone keeps its UUID data reference", context ->
                        OWNER.equals(context.mc().player.getMainHandItem()
                                .get(SmartPhoneRegistries.PHONE_OWNER.get())))
                .setHeldItem(ItemStack.EMPTY)
                .waitUntil("existing phone closes", context -> context.screen() == null)
                .setHeldItem(invalidPhone)
                .serverTicks(2)
                .waitUntil("unknown owner's held screen is rejected", context ->
                        context.screen() == null)
                .checkServer("invalid owner did not create phone data", context ->
                        !SmartPhone.getPhoneSavedData().hasPhoneInfo(UNBOUND_TARGET))
                .teardown("close phone", context -> context.mc().setScreen(null))
                .teardownServer("clear held phone", context ->
                        context.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }
}
