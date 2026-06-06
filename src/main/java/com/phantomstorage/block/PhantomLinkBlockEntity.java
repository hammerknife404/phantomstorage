package com.phantomstorage.block;

import com.phantomstorage.ModBlockEntities;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class PhantomLinkBlockEntity extends BlockEntity {

    // 80 credits/tick × 20 ticks = 1600; 1600 / 100 per item = 16 items/sec
    private static final int CREDITS_PER_TICK = 80;
    private static final int CREDITS_PER_ITEM = 100;
    private static final int MAX_CREDITS      = 1600;

    // How long (ticks) the flow indicator remains visible after the last transfer
    private static final int FLOW_DISPLAY_TICKS = 20;

    // Search range in blocks — covers any practical base-to-base distance
    private static final double SEARCH_RANGE = 2048.0;

    @Nullable private UUID ownerUUID;
    @Nullable private PhantomChestEntity cachedChest;

    private int credits        = 0;
    private int flowDecayTicks = 0;
    private int searchCooldown = 0;

    // Client-side mirror of (flowDecayTicks > 0), set via block entity sync packet
    public boolean isFlowingClient = false;

    // ── Item handler ──────────────────────────────────────────────────────────

    private final IItemHandler itemHandler = new IItemHandler() {

        @Override
        public int getSlots() {
            return 54;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            PhantomChestEntity chest = findChest();
            return chest != null ? chest.getInventory().getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            PhantomChestEntity chest = findChest();
            if (chest == null) return stack;
            // Rate-limit actual transfers; simulations always report true capacity
            if (!simulate && credits < CREDITS_PER_ITEM) return stack;

            ItemStack remaining = new InvWrapper(chest.getInventory()).insertItem(slot, stack, simulate);
            if (!simulate && remaining.getCount() < stack.getCount()) {
                credits -= CREDITS_PER_ITEM;
                markFlow(1, chest); // items flowing IN to phantom chest
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            PhantomChestEntity chest = findChest();
            if (chest == null) return ItemStack.EMPTY;
            if (!simulate && credits < CREDITS_PER_ITEM) return ItemStack.EMPTY;

            ItemStack extracted = new InvWrapper(chest.getInventory()).extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                credits -= CREDITS_PER_ITEM;
                markFlow(2, chest); // items flowing OUT of phantom chest
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    // ─────────────────────────────────────────────────────────────────────────

    public PhantomLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHANTOM_LINK.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
        this.cachedChest = null;
        this.searchCooldown = 0;
        setChanged();
    }

    // ── Entity lookup ─────────────────────────────────────────────────────────

    @Nullable
    private PhantomChestEntity findChest() {
        if (!(level instanceof ServerLevel) || ownerUUID == null) return null;

        // Cache hit
        if (cachedChest != null && cachedChest.isAlive()
                && ownerUUID.equals(cachedChest.getOwnerUUID())) {
            return cachedChest;
        }

        // Don't hammer entity search every tick while the chest is absent
        if (searchCooldown > 0) return null;
        searchCooldown = 40;

        cachedChest = null;
        List<PhantomChestEntity> candidates = level.getEntitiesOfClass(
                PhantomChestEntity.class,
                new AABB(worldPosition).inflate(SEARCH_RANGE),
                e -> ownerUUID.equals(e.getOwnerUUID()) && e.isAlive());
        if (!candidates.isEmpty()) {
            cachedChest = candidates.get(0);
        }
        return cachedChest;
    }

    // ── Flow state ────────────────────────────────────────────────────────────

    private void markFlow(int state, PhantomChestEntity chest) {
        boolean wasFlowing = flowDecayTicks > 0;
        flowDecayTicks = FLOW_DISPLAY_TICKS;
        chest.setFlowState(state);
        if (!wasFlowing) syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Flowing", flowDecayTicks > 0);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        isFlowingClient = tag.getBoolean("Flowing");
    }

    public void onRemoved() {
        PhantomChestEntity chest = findChest();
        if (chest != null) chest.setFlowState(0);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PhantomLinkBlockEntity be) {
        be.credits = Math.min(be.credits + CREDITS_PER_TICK, MAX_CREDITS);

        if (be.searchCooldown > 0) be.searchCooldown--;

        if (be.flowDecayTicks > 0 && --be.flowDecayTicks == 0) {
            PhantomChestEntity chest = be.findChest();
            if (chest != null) chest.setFlowState(0);
            be.syncToClient();
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putInt("Credits", credits);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        credits   = tag.getInt("Credits");
    }
}
