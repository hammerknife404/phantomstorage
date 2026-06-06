package com.phantomstorage.client.screen;

import com.phantomstorage.inventory.PhantomLinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PhantomLinkScreen extends AbstractContainerScreen<PhantomLinkMenu> {

    // Approximate Minecraft dye colors for channels 0-15
    private static final int[] CHANNEL_COLORS = {
        0xF9FFFE, // 0  White
        0xF9801D, // 1  Orange
        0xC74EBD, // 2  Magenta
        0x3AB3DA, // 3  Light Blue
        0xFED83D, // 4  Yellow
        0x80C71F, // 5  Lime
        0xF38BAA, // 6  Pink
        0x474F52, // 7  Gray
        0x9D9D97, // 8  Light Gray
        0x169C9C, // 9  Cyan
        0x8932B8, // 10 Purple
        0x3C44AA, // 11 Blue
        0x835432, // 12 Brown
        0x5E7C16, // 13 Green
        0xB02E26, // 14 Red
        0x1D1D21, // 15 Black
    };

    private static final int COLS    = 4;
    private static final int ROWS    = 4;
    private static final int BTN_W   = 26;
    private static final int BTN_H   = 20;
    private static final int GAP     = 3;
    private static final int PAD     = 8;
    private static final int GRID_W  = COLS * BTN_W + (COLS - 1) * GAP;
    private static final int GRID_H  = ROWS * BTN_H + (ROWS - 1) * GAP;

    public PhantomLinkScreen(PhantomLinkMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth       = GRID_W + PAD * 2;
        this.imageHeight      = GRID_H + PAD * 2 + 14 + 20; // 14 = title bar, 20 = status label
        this.titleLabelX      = PAD;
        this.titleLabelY      = 5;
        this.inventoryLabelY  = 10000; // hide vanilla inventory label
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        // Panel background
        gfx.fill(leftPos,     topPos,     leftPos + imageWidth,     topPos + imageHeight,     0xFF636363);
        gfx.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        int currentCh = menu.getChannel();
        boolean isOwner = menu.isOwner();
        int startX = leftPos + PAD;
        int startY = topPos  + PAD + 14;

        for (int i = 0; i < 16; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int bx  = startX + col * (BTN_W + GAP);
            int by  = startY + row * (BTN_H + GAP);

            boolean selected  = (i == currentCh);
            boolean hovered   = isOwner
                    && mouseX >= bx && mouseX < bx + BTN_W
                    && mouseY >= by && mouseY < by + BTN_H;

            // Border: gold if selected, white if hovered, dark otherwise
            int borderColor = selected ? 0xFFFFD700
                            : hovered  ? 0xFFFFFFFF
                                       : 0xFF333333;
            gfx.fill(bx - 1, by - 1, bx + BTN_W + 1, by + BTN_H + 1, borderColor);

            // Button face
            int faceColor = 0xFF000000 | CHANNEL_COLORS[i];
            gfx.fill(bx, by, bx + BTN_W, by + BTN_H, faceColor);

            // Channel number — pick black or white based on luminance
            int r = (CHANNEL_COLORS[i] >> 16) & 0xFF;
            int g = (CHANNEL_COLORS[i] >> 8)  & 0xFF;
            int b =  CHANNEL_COLORS[i]         & 0xFF;
            int lum = (r * 299 + g * 587 + b * 114) / 1000;
            int textColor = lum > 128 ? 0x000000 : 0xFFFFFF;

            String label = String.valueOf(i);
            gfx.drawString(font, label,
                    bx + BTN_W / 2 - font.width(label) / 2,
                    by + BTN_H / 2 - 4,
                    textColor, false);
        }

        // Dim entire grid if not owner
        if (!isOwner) {
            gfx.fill(startX - 1, startY - 1,
                     startX + GRID_W + 1, startY + GRID_H + 1,
                     0x88000000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        // Status label — positioned just below the button grid
        int labelY = PAD + 14 + GRID_H + 6;
        if (!menu.isOwner()) {
            String msg = "Not the owner";
            gfx.drawString(font, msg,
                    imageWidth / 2 - font.width(msg) / 2,
                    labelY, 0xFF4444, false);
        } else {
            int ch = menu.getChannel();
            String msg = ch < 0 ? "No channel set" : "Channel: " + ch;
            gfx.drawString(font, msg,
                    imageWidth / 2 - font.width(msg) / 2,
                    labelY, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.isOwner()) return super.mouseClicked(mouseX, mouseY, button);

        int startX = leftPos + PAD;
        int startY = topPos  + PAD + 14;

        for (int i = 0; i < 16; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int bx  = startX + col * (BTN_W + GAP);
            int by  = startY + row * (BTN_H + GAP);

            if (mouseX >= bx && mouseX < bx + BTN_W
                    && mouseY >= by && mouseY < by + BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
