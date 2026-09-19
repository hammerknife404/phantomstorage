package com.phantomstorage.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phantomstorage.DesignationMode;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.inventory.PhantomChestMenu;
import com.phantomstorage.network.LinkedStorageSyncPayload;
import com.phantomstorage.network.WrenchHighlightData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PhantomChestScreen extends AbstractContainerScreen<PhantomChestMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");

    // Color of the generic_54 background panel — used to cover inactive chest slot rows
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BORDER  = 0xFF373737;
    private static final int SLOT_INNER   = 0xFFA0A0A0;
    private static final int ACTIVE_TAB_HIGHLIGHT = 0x4400AAFF;

    // ── Logistics tab layout ─────────────────────────────────────────────────
    private static final int LOGI_X      = 10;
    private static final int LOGI_HEADER_Y = 20;
    private static final int LOGI_ROWS_Y = 32;
    private static final int LOGI_COL_W  = 79;
    private static final int LOGI_ROW_H  = 12;
    private static final int LOGI_ROWS   = 8;

    // Vanilla generic_54.png's chest-slot area ends here; everything below (player inventory +
    // hotbar) is blitted separately, shifted down, to make room for the crafting row that's now
    // permanently docked under the main inventory instead of living on its own tab.
    private static final int CHEST_AREA_SRC_BOTTOM = 125;
    private static final int VANILLA_SRC_HEIGHT     = 222;
    // Must match the +55 baked into PhantomChestMenu's PLAYER_INV_Y (195 = 140 + 55) and
    // HOTBAR_Y (253 = 198 + 55) — leaves exactly enough room for the 3-row crafting grid
    // (CRAFT_GRID_Y=133, 54px tall) plus an 8px margin on each side.
    private static final int PLAYER_SECTION_SHIFT   = 55;

    public PhantomChestScreen(PhantomChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = VANILLA_SRC_HEIGHT + PLAYER_SECTION_SHIFT;
    }

    private Button filterTabBtn;

    @Override
    protected void init() {
        super.init();

        int bw = 30, bh = 18, step = 32;
        int by = topPos - 22;

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.chest"),
                b -> switchTab(PhantomChestMenu.TAB_CHEST))
                .bounds(leftPos + 7, by, bw, bh).build());

        filterTabBtn = Button.builder(
                Component.translatable("container.phantomstorage.tab.filter"),
                b -> switchTab(PhantomChestMenu.TAB_FILTER))
                .bounds(leftPos + 7 + step, by, bw, bh).build();
        addRenderableWidget(filterTabBtn);

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.logistics"),
                b -> switchTab(PhantomChestMenu.TAB_LOGISTICS))
                .bounds(leftPos + 7 + step * 2, by, bw, bh).build());

        addRenderableWidget(Button.builder(
                Component.translatable("container.phantomstorage.tab.refill"),
                b -> switchTab(PhantomChestMenu.TAB_REFILL))
                .bounds(leftPos + 7 + step * 3, by, bw, bh).build());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int tier = menu.getEntityTier();
        if (filterTabBtn != null) filterTabBtn.active = tier >= 2;
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
        // Top: title border + chest inventory rows, unchanged from the vanilla source.
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, CHEST_AREA_SRC_BOTTOM);
        // Bottom: player inventory + hotbar, shifted down to make room for the crafting row.
        gfx.blit(TEXTURE, leftPos, topPos + CHEST_AREA_SRC_BOTTOM + PLAYER_SECTION_SHIFT,
                0, CHEST_AREA_SRC_BOTTOM, imageWidth, VANILLA_SRC_HEIGHT - CHEST_AREA_SRC_BOTTOM);
        // Flat panel fill for the gap opened up between them.
        gfx.fill(leftPos, topPos + CHEST_AREA_SRC_BOTTOM,
                leftPos + imageWidth, topPos + CHEST_AREA_SRC_BOTTOM + PLAYER_SECTION_SHIFT, PANEL_COLOR);

        int tab = menu.getActiveTab();
        if (tab == PhantomChestMenu.TAB_CHEST) {
            // Crafting is always shown docked under the main inventory now, tier permitting.
            if (menu.getEntityTier() >= 1) renderCraftingTab(gfx);
            return;
        }

        // Cover the 6×9 chest slot squares drawn by the texture
        gfx.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 125, PANEL_COLOR);

        if (tab == PhantomChestMenu.TAB_FILTER) {
            renderFilterTab(gfx);
        } else if (tab == PhantomChestMenu.TAB_REFILL) {
            renderRefillTab(gfx);
        } else {
            renderLogisticsTab(gfx, mouseX, mouseY);
        }
    }

    private void renderLogisticsTab(GuiGraphics gfx, int mouseX, int mouseY) {
        List<LinkedStorageSyncPayload.HighlightEntry> entries = WrenchHighlightData.get();
        int tier = WrenchHighlightData.getTier();
        int cap = PhantomChestEntity.linkCapForTier(tier);

        gfx.drawString(font,
                Component.translatable("container.phantomstorage.tab.logistics.header", entries.size(), cap),
                leftPos + LOGI_X, topPos + LOGI_HEADER_Y, 0xFF404040, false);

        if (entries.isEmpty()) {
            gfx.drawString(font,
                    Component.translatable("container.phantomstorage.tab.logistics.empty"),
                    leftPos + LOGI_X, topPos + LOGI_ROWS_Y, 0xFF808080, false);
            return;
        }

        double range = PhantomChestEntity.transferRangeForTier(tier);
        Vec3 playerPos = minecraft.player.position();

        for (int i = 0; i < entries.size(); i++) {
            LinkedStorageSyncPayload.HighlightEntry entry = entries.get(i);
            int[] pos = rowPos(i);
            int x = pos[0], y = pos[1];

            boolean inRange = Vec3.atCenterOf(entry.pos()).distanceToSqr(playerPos) <= range * range;
            int modeColor  = entry.mode() == DesignationMode.INPUT ? 0xFF3388FF : 0xFFFF8C1A;
            int rangeColor = inRange ? 0xFF55FF55 : 0xFFFF5555;

            gfx.fill(x, y, x + 6, y + 6, modeColor);
            String coords = entry.pos().getX() + "," + entry.pos().getY() + "," + entry.pos().getZ();
            gfx.drawString(font, coords, x + 9, y - 1, rangeColor, false);

            boolean hoverUnlink = mouseX >= x + LOGI_COL_W - 8 && mouseX < x + LOGI_COL_W
                    && mouseY >= y - 1 && mouseY < y + 7;
            gfx.drawString(font, "X", x + LOGI_COL_W - 8, y - 1, hoverUnlink ? 0xFFFFFFFF : 0xFFAA0000, false);
        }
    }

    /** {x, y} screen-space top-left for logistics row `i`, in a 2-column x LOGI_ROWS layout. */
    private int[] rowPos(int i) {
        int col = i / LOGI_ROWS;
        int row = i % LOGI_ROWS;
        return new int[] {
                leftPos + LOGI_X + col * LOGI_COL_W,
                topPos  + LOGI_ROWS_Y + row * LOGI_ROW_H
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.getActiveTab() == PhantomChestMenu.TAB_LOGISTICS) {
            List<LinkedStorageSyncPayload.HighlightEntry> entries = WrenchHighlightData.get();
            for (int i = 0; i < entries.size(); i++) {
                int[] pos = rowPos(i);
                int x = pos[0], y = pos[1];
                if (mouseX >= x && mouseX < x + LOGI_COL_W && mouseY >= y - 1 && mouseY < y + 7) {
                    boolean hitUnlink = mouseX >= x + LOGI_COL_W - 8;
                    int id = (hitUnlink ? PhantomChestMenu.LOGISTICS_UNLINK_BASE
                                        : PhantomChestMenu.LOGISTICS_CYCLE_BASE) + i;
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        // Word-wrapped and width-capped so it can never spill past the panel's edges.
        gfx.drawWordWrap(font,
                Component.translatable("container.phantomstorage.tab.filter.warning"),
                leftPos + 9, topPos + 96, 158, 0xFFCC4444);
    }

    private void renderRefillTab(GuiGraphics gfx) {
        gfx.drawString(font,
                Component.translatable("container.phantomstorage.tab.refill.header"),
                leftPos + 61, topPos + 20, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBg(gfx,
                        leftPos + PhantomChestMenu.REFILL_X - 1 + col * 18,
                        topPos  + PhantomChestMenu.REFILL_Y - 1 + row * 18);
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
