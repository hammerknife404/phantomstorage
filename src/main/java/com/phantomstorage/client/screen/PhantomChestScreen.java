package com.phantomstorage.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phantomstorage.inventory.PhantomChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI screen for the Phantom Chest (6×9 = 54 slots).
 * Uses vanilla generic_54 texture. imageHeight = 114 + 6*18 = 222.
 */
public class PhantomChestScreen extends AbstractContainerScreen<PhantomChestMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");

    public PhantomChestScreen(PhantomChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222; // 114 + 6 rows * 18px — matches generic_54.png exactly
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gfx.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Draw only the chest title — suppress the vanilla "Inventory" label
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
}
