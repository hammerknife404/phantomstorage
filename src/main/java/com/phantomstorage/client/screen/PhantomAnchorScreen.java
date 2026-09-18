package com.phantomstorage.client.screen;

import com.phantomstorage.inventory.PhantomAnchorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PhantomAnchorScreen extends AbstractContainerScreen<PhantomAnchorMenu> {

    private Button radiusDownBtn;
    private Button radiusUpBtn;
    private Button ejectBtn;

    public PhantomAnchorScreen(PhantomAnchorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 96;
        this.inventoryLabelY = 10000; // hide vanilla inventory label — this GUI has no slots
    }

    @Override
    protected void init() {
        super.init();

        int midY = topPos + 46;

        radiusDownBtn = Button.builder(Component.literal("-"),
                b -> click(PhantomAnchorMenu.BTN_RADIUS_DOWN))
                .bounds(leftPos + 40, midY - 10, 20, 20).build();
        addRenderableWidget(radiusDownBtn);

        radiusUpBtn = Button.builder(Component.literal("+"),
                b -> click(PhantomAnchorMenu.BTN_RADIUS_UP))
                .bounds(leftPos + 116, midY - 10, 20, 20).build();
        addRenderableWidget(radiusUpBtn);

        ejectBtn = Button.builder(Component.translatable("container.phantomstorage.anchor.eject"),
                b -> click(PhantomAnchorMenu.BTN_EJECT))
                .bounds(leftPos + 48, topPos + 72, 80, 18).build();
        addRenderableWidget(ejectBtn);
    }

    private void click(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        boolean owner = menu.isOwner();
        radiusDownBtn.active = owner;
        radiusUpBtn.active = owner;
        ejectBtn.visible = menu.isDocked();
        ejectBtn.active = owner && menu.isDocked();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos,     topPos,     leftPos + imageWidth,     topPos + imageHeight,     0xFF636363);
        gfx.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        String status = !menu.isOwner()
                ? "Not the owner"
                : menu.isDocked()
                    ? Component.translatable("container.phantomstorage.anchor.docked").getString()
                    : Component.translatable("container.phantomstorage.anchor.not_docked").getString();
        int statusColor = !menu.isOwner() ? 0xFFCC4444 : menu.isDocked() ? 0xFF2E8B57 : 0xFF808080;
        gfx.drawString(font, status,
                leftPos + imageWidth / 2 - font.width(status) / 2,
                topPos + 22, statusColor, false);

        String radiusLabel = Component.translatable(
                "container.phantomstorage.anchor.radius", menu.getRadius()).getString();
        gfx.drawString(font, radiusLabel,
                leftPos + imageWidth / 2 - font.width(radiusLabel) / 2,
                topPos + 38, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }
}
