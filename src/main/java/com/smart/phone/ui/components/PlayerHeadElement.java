package com.smart.phone.ui.components;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayerHeadElement extends UIElement {
    private static final Cache<String, CompletableFuture<ResourceLocation>> AVATAR_SKINS = CacheBuilder.newBuilder()
            .maximumSize(256).expireAfterWrite(Duration.ofMinutes(5)).build();
    private final UUID playerUuid;
    private final String playerName;
    private final String avatarPlayerName;
    private final long createdAtNanos = System.nanoTime();
    private CompletableFuture<ResourceLocation> avatarSkin;

    public PlayerHeadElement(float size) {
        this(null, size);
    }

    public PlayerHeadElement(UUID playerUuid, float size) {
        this(playerUuid, null, size);
    }

    public PlayerHeadElement(UUID playerUuid, String playerName, float size) {
        this(playerUuid, playerName, "", size);
    }

    public PlayerHeadElement(UUID playerUuid, String playerName, String avatarPlayerName, float size) {
        super();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.avatarPlayerName = avatarPlayerName == null ? "" : avatarPlayerName.trim();
        layout(layout -> {
            layout.width(size);
            layout.height(size);
            layout.flexShrink(0);
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        RenderSystem.depthMask(false);
        guiContext.graphics.drawManaged(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ResourceLocation skin = getSkin(minecraft);

            if (skin != null) {
                var x = (int) getPositionX();
                var y = (int) getPositionY();
                var size = (int) getSizeWidth();

                PlayerFaceRenderer.draw(guiContext.graphics, skin, x, y, size);
            }
        });
        RenderSystem.depthMask(true);
    }

    /**
     * 返回当前可用头像纹理；指定头像尚未加载或加载失败时沿用发送者头像。
     *
     * @param minecraft 当前客户端，调用须在客户端主线程执行
     * @return 可用的皮肤纹理；未指定人物且没有本地玩家时为 {@code null}
     */
    public ResourceLocation getSkin(Minecraft minecraft) {
        // 等输入稳定后再查询；皮肤下载和账号查询均不阻塞渲染线程。
        if (avatarPlayerName.matches("[A-Za-z0-9_]{1,16}")
                && System.nanoTime() - createdAtNanos >= TimeUnit.MILLISECONDS.toNanos(500)) {
            if (avatarSkin == null) {
                avatarSkin = AVATAR_SKINS.asMap().computeIfAbsent(avatarPlayerName.toLowerCase(Locale.ROOT), name ->
                        SkullBlockEntity.fetchGameProfile(name).thenComposeAsync(profile ->
                                profile.map(value -> minecraft.getSkinManager().getOrLoad(value)
                                                .thenApply(skin -> skin.texture()))
                                        .orElseGet(() -> CompletableFuture.completedFuture(null)), minecraft)
                                .completeOnTimeout(null, 15, TimeUnit.SECONDS).exceptionally(error -> null));
            }
            ResourceLocation skin = avatarSkin.getNow(null);
            if (skin != null) return skin;
        }
        if (playerUuid != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerUuid);
            if (playerInfo != null) {
                return playerInfo.getSkin().texture();
            }
        }
        if (playerName != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerName);
            if (playerInfo != null) return playerInfo.getSkin().texture();
        }
        // Fictional/offline names have a stable default avatar, never the viewer's skin.
        if (playerUuid != null || playerName != null) {
            UUID fallback = playerUuid != null && !playerUuid.equals(new UUID(0, 0)) ? playerUuid
                    : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
            return DefaultPlayerSkin.get(fallback).texture();
        }
        LocalPlayer player = minecraft.player;
        return player == null ? null : player.getSkin().texture();
    }
}
