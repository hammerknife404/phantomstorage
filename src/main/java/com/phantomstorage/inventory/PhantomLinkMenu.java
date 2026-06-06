package com.phantomstorage.inventory;

import com.phantomstorage.ModBlocks;
import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.block.PhantomLinkBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class PhantomLinkMenu extends AbstractContainerMenu {

    // [0] = channel (-1 = unset), [1] = isOwner (0/1)
    private final SimpleContainerData data = new SimpleContainerData(2);
    @Nullable private final PhantomLinkBlockEntity blockEntity;

    /** Server-side constructor. */
    public PhantomLinkMenu(int id, Inventory playerInv, PhantomLinkBlockEntity be) {
        super(ModMenuTypes.PHANTOM_LINK_MENU.get(), id);
        this.blockEntity = be;
        data.set(0, be.getChannel());
        data.set(1, playerInv.player.getUUID().equals(be.getOwnerUUID()) ? 1 : 0);
        addDataSlots(data);
    }

    /** Client-side constructor (from network packet). */
    public PhantomLinkMenu(int id, Inventory playerInv, FriendlyByteBuf ignored) {
        super(ModMenuTypes.PHANTOM_LINK_MENU.get(), id);
        this.blockEntity = null;
        addDataSlots(data);
    }

    public int     getChannel() { return data.get(0); }
    public boolean isOwner()    { return data.get(1) == 1; }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null) data.set(0, blockEntity.getChannel());
        super.broadcastChanges();
    }

    /** id = 0-15 to set channel; called by the client button click. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        if (!player.getUUID().equals(blockEntity.getOwnerUUID())) return false;
        if (id < 0 || id > 15) return false;
        blockEntity.setChannel(id);
        data.set(0, id);
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
                ModBlocks.PHANTOM_LINK.get());
    }
}
