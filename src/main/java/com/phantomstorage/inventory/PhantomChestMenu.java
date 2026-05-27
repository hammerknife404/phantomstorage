package com.phantomstorage.inventory;

import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 54-slot chest menu arranged as 6 rows × 9 columns.
 * Uses vanilla generic_54 texture. imageHeight = 114 + 6*18 = 222.
 * Player inventory at y=140, hotbar at y=198.
 */
public class PhantomChestMenu extends AbstractContainerMenu {

    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int CONTAINER_SIZE = ROWS * COLS; // 54

    private static final int CHEST_X = 8;
    private static final int CHEST_Y = 18;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 140; // CHEST_Y + ROWS*18 + 14 = 18 + 108 + 14
    private static final int HOTBAR_Y = 198;      // PLAYER_INV_Y + 3*18 + 4

    private final Container container;
    @Nullable private final PhantomChestEntity entity;

    /** Server-side constructor (opened from the entity). */
    public PhantomChestMenu(int id, Inventory playerInventory, Container container, PhantomChestEntity entity) {
        super(ModMenuTypes.PHANTOM_CHEST_MENU.get(), id);
        this.container = container;
        this.entity = entity;
        checkContainerSize(container, CONTAINER_SIZE);

        // Chest slots — 6 rows × 9 cols
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlot(new Slot(container, row * COLS + col,
                        CHEST_X + col * 18, CHEST_Y + row * 18));
            }
        }

        // Player inventory (rows 1–3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor (from network). */
    public PhantomChestMenu(int id, Inventory playerInventory, FriendlyByteBuf ignored) {
        this(id, playerInventory, new SimpleContainer(CONTAINER_SIZE), null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < CONTAINER_SIZE) {
            // Chest → player
            if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player → chest
            if (!this.moveItemStackTo(stack, 0, CONTAINER_SIZE, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (entity != null) {
            // Persist inventory to player data here — this is the single authoritative save point.
            // Doing it in a SimpleContainer listener would serialise all 54 slots on every item move.
            entity.saveInventoryTo(player);
            entity.onMenuClosed();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
