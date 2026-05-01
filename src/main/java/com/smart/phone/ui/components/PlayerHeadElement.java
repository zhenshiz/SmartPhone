package com.smart.phone.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerHeadElement extends UIElement {
    private final UUID playerUuid;

    public PlayerHeadElement(float size) {
        this(null, size);
    }

    public PlayerHeadElement(UUID playerUuid, float size) {
        super();
        this.playerUuid = playerUuid;
        layout(layout -> {
            layout.width(size);
            layout.height(size);
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

                guiContext.graphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
            }
        });
        RenderSystem.depthMask(true);
    }

    private ResourceLocation getSkin(Minecraft minecraft) {
        if (playerUuid != null && minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerUuid);
            if (playerInfo != null) {
                return playerInfo.getSkin().texture();
            }
        }
        LocalPlayer player = minecraft.player;
        return player == null ? null : player.getSkin().texture();
    }
}
