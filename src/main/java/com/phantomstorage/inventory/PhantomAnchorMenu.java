package com.phantomstorage.inventory;

import com.phantomstorage.ModBlocks;
import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.block.PhantomAnchorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class PhantomAnchorMenu extends AbstractContainerMenu {

    public static final int BTN_RADIUS_DOWN     = 0;
    public static final int BTN_RADIUS_UP       = 1;
    public static final int BTN_EJECT           = 2;
    public static final int BTN_INPUT_MODE      = 3;
    public static final int BTN_COMPARATOR_MODE = 4;

    // [0]=docked(0/1), [1]=radius, [2]=isOwner(0/1), [3]=inputMode ordinal, [4]=comparatorMode ordinal
    private final SimpleContainerData data = new SimpleContainerData(5);
    @Nullable private final PhantomAnchorBlockEntity blockEntity;

    /** Server-side constructor. */
    public PhantomAnchorMenu(int id, Inventory playerInv, PhantomAnchorBlockEntity be) {
        super(ModMenuTypes.PHANTOM_ANCHOR_MENU.get(), id);
        this.blockEntity = be;
        data.set(0, be.isDocked() ? 1 : 0);
        data.set(1, be.getRadius());
        data.set(2, playerInv.player.getUUID().equals(be.getOwnerUUID()) ? 1 : 0);
        data.set(3, be.getInputMode().ordinal());
        data.set(4, be.getComparatorMode().ordinal());
        addDataSlots(data);
    }

    /** Client-side constructor (from network). */
    public PhantomAnchorMenu(int id, Inventory playerInv, FriendlyByteBuf ignored) {
        super(ModMenuTypes.PHANTOM_ANCHOR_MENU.get(), id);
        this.blockEntity = null;
        addDataSlots(data);
    }

    public boolean isDocked()          { return data.get(0) == 1; }
    public int     getRadius()         { return data.get(1); }
    public boolean isOwner()           { return data.get(2) == 1; }
    public int     getInputModeOrdinal()      { return data.get(3); }
    public int     getComparatorModeOrdinal() { return data.get(4); }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) {
            data.set(0, blockEntity.isDocked() ? 1 : 0);
            data.set(1, blockEntity.getRadius());
            data.set(3, blockEntity.getInputMode().ordinal());
            data.set(4, blockEntity.getComparatorMode().ordinal());
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        if (!player.getUUID().equals(blockEntity.getOwnerUUID())) return false;

        switch (id) {
            case BTN_RADIUS_DOWN     -> blockEntity.setRadius(blockEntity.getRadius() - 1);
            case BTN_RADIUS_UP       -> blockEntity.setRadius(blockEntity.getRadius() + 1);
            case BTN_INPUT_MODE      -> blockEntity.cycleInputMode();
            case BTN_COMPARATOR_MODE -> blockEntity.cycleComparatorMode();
            case BTN_EJECT -> {
                if (player instanceof ServerPlayer sp) blockEntity.toggleDock(sp);
            }
            default -> { return false; }
        }
        data.set(0, blockEntity.isDocked() ? 1 : 0);
        data.set(1, blockEntity.getRadius());
        data.set(3, blockEntity.getInputMode().ordinal());
        data.set(4, blockEntity.getComparatorMode().ordinal());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                ModBlocks.PHANTOM_ANCHOR.get());
    }
}
