package com.phantomstorage.inventory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps the 54-slot chest inventory. Any item matching a slot in the
 * filterSlots container is silently voided instead of stored.
 * Uses loadItem() to bypass the filter during NBT deserialization.
 */
public class VoidFilterContainer extends SimpleContainer {

    private final SimpleContainer filter;

    public VoidFilterContainer(int size, SimpleContainer filter) {
        super(size);
        this.filter = filter;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && matchesFilter(stack)) {
            return;
        }
        super.setItem(slot, stack);
    }

    /** Bypasses void check — used when loading from NBT. */
    public void loadItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
    }

    private boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < filter.getContainerSize(); i++) {
            ItemStack f = filter.getItem(i);
            if (!f.isEmpty() && ItemStack.isSameItem(f, stack)) return true;
        }
        return false;
    }
}
