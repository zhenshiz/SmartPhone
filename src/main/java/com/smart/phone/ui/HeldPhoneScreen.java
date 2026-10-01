package com.smart.phone.ui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class HeldPhoneScreen extends ModularUIScreen {
    private static final long RAISE_DURATION_NANOS = 240_000_000L;
    private final PhoneUI phoneUI;
    private final long raiseStartedAt = System.nanoTime();
    private boolean synced = true;

    public HeldPhoneScreen(ModularUI modularUI, PhoneUI phoneUI) {
        super(modularUI, Component.empty());
        this.phoneUI = phoneUI;
    }

    public boolean isRaised() {
        return System.nanoTime() - raiseStartedAt >= RAISE_DURATION_NANOS;
    }

    public float raiseProgress() {
        return Math.min(1f, (System.nanoTime() - raiseStartedAt) / (float) RAISE_DURATION_NANOS);
    }

    public PhoneUI getPhoneUI() {
        return phoneUI;
    }

    public boolean isSynced() {
        return synced;
    }

    public void markSynced() {
        synced = true;
    }

    public void awaitSync() {
        synced = false;
    }

    @Override
    public void init() {
        super.init();
        phoneUI.updateHeldPose(width, height, 0f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isRaised() || !synced) return false;
        modularUI.refreshHoveredElementAtScreen((float) mouseX, (float) mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        modularUI.refreshHoveredElementAtScreen((float) mouseX, (float) mouseY);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        modularUI.refreshHoveredElementAtScreen((float) mouseX, (float) mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        phoneUI.updateHeldPose(width, height, raiseProgress());
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
