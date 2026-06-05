package com.phantomstorage.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phantomstorage.inventory.PhantomChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PhantomChestScreen extends AbstractContainerScreen<PhantomChestMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");

    // Color of the generic_54 background panel — used to cover inactive chest slot rows
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BORDER  = 0xFF373737;
    private static final int SLOT_INNER   = 0xFFA0A0A0;
    private static final int ACTIVE_TAB_HIGHLIGHT = 0x4400AAFF;

    public PhantomChestScreen(PhantomChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();

        int bw = 48, bh = 18;
        int by = topPos - 22;

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.chest"),
                b -> switchTab(PhantomChestMenu.TAB_CHEST))
                .bounds(leftPos + 7, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.crafting"),
                b -> switchTab(PhantomChestMenu.TAB_CRAFT))
                .bounds(leftPos + 63, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.filter"),
                b -> switchTab(PhantomChestMenu.TAB_FILTER))
                .bounds(leftPos + 119, by, bw, bh).build());
    }

    private void switchTab(int tab) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tab);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int tab = menu.getActiveTab();
        if (tab == PhantomChestMenu.TAB_CHEST) return;

        // Cover the 6×9 chest slot squares drawn by the texture
        gfx.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 125, PANEL_COLOR);

        if (tab == PhantomChestMenu.TAB_CRAFT) {
            renderCraftingTab(gfx);
        } else {
            renderFilterTab(gfx);
        }
    }

    private void renderCraftingTab(GuiGraphics gfx) {
        // 3×3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBg(gfx,
                        leftPos + PhantomChestMenu.CRAFT_GRID_X - 1 + col * 18,
                        topPos  + PhantomChestMenu.CRAFT_GRID_Y - 1 + row * 18);
            }
        }
        // Arrow
        gfx.drawString(font, "→",
                leftPos + PhantomChestMenu.CRAFT_GRID_X + 58,
                topPos  + PhantomChestMenu.CRAFT_GRID_Y + 23,
                0xFF404040, false);
        // Result slot
        drawSlotBg(gfx,
                leftPos + PhantomChestMenu.CRAFT_RESULT_X - 1,
                topPos  + PhantomChestMenu.CRAFT_RESULT_Y - 1);
    }

    private void renderFilterTab(GuiGraphics gfx) {
        gfx.drawString(font,
                Component.translatable("container.phantomstorage.tab.filter"),
                leftPos + 61, topPos + 20, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBg(gfx,
                        leftPos + PhantomChestMenu.FILTER_X - 1 + col * 18,
                        topPos  + PhantomChestMenu.FILTER_Y - 1 + row * 18);
            }
        }
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x,     y,     x + 18, y + 18, SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_INNER);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }
}
