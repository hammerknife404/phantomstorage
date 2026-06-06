package com.phantomstorage.block;

import com.phantomstorage.ModBlockEntities;
import com.phantomstorage.ModBlocks;
import com.phantomstorage.inventory.PhantomLinkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhantomLinkBlockEntity extends BlockEntity implements MenuProvider {

    // 80 credits/tick × 20 ticks/sec = 1600 max; 100 per item → 16 items/sec
    private static final int CREDITS_PER_TICK = 80;
    private static final int CREDITS_PER_ITEM = 100;
    private static final int MAX_CREDITS      = 1600;

    // ownerUUID → channelId → set of GlobalPos for all links in that channel
    private static final Map<UUID, Map<Integer, Set<GlobalPos>>> REGISTRY =
            new ConcurrentHashMap<>();

    @Nullable private UUID ownerUUID;
    private int channel = -1; // -1 = not configured
    private int credits  = 0;

    public PhantomLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHANTOM_LINK.get(), pos, state);
    }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.literal("Phantom Link");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new PhantomLinkMenu(id, playerInv, this);
    }

    // ── Registry ──────────────────────────────────────────────────────────────

    private void registerSelf() {
        if (ownerUUID == null || channel < 0 || level == null) return;
        GlobalPos gpos = GlobalPos.of(level.dimension(), worldPosition);
        REGISTRY.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
                .add(gpos);
    }

    private void deregisterSelf() {
        if (ownerUUID == null || channel < 0 || level == null) return;
        GlobalPos gpos = GlobalPos.of(level.dimension(), worldPosition);
        Map<Integer, Set<GlobalPos>> ownerMap = REGISTRY.get(ownerUUID);
        if (ownerMap == null) return;
        Set<GlobalPos> positions = ownerMap.get(channel);
        if (positions != null) {
            positions.remove(gpos);
            if (positions.isEmpty()) ownerMap.remove(channel);
        }
        if (ownerMap.isEmpty()) REGISTRY.remove(ownerUUID);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        REGISTRY.clear();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) registerSelf();
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) deregisterSelf();
        super.setRemoved();
    }

    // ── Setters / getters ─────────────────────────────────────────────────────

    public void setOwnerUUID(@Nullable UUID uuid) {
        if (level != null && !level.isClientSide()) deregisterSelf();
        this.ownerUUID = uuid;
        if (level != null && !level.isClientSide()) registerSelf();
        setChanged();
    }

    public void setChannel(int ch) {
        if (level != null && !level.isClientSide()) deregisterSelf();
        this.channel = ch;
        if (level != null && !level.isClientSide()) registerSelf();
        setChanged();
    }

    public int  getChannel()   { return channel; }
    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }
    public BlockPos getBlockPos()  { return worldPosition; }

    // ── Transfer ──────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PhantomLinkBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.credits = Math.min(be.credits + CREDITS_PER_TICK, MAX_CREDITS);
        if (be.credits < CREDITS_PER_ITEM) return;
        if (be.channel < 0 || be.ownerUUID == null) return;
        be.doTransfer(serverLevel, state);
    }

    private void doTransfer(ServerLevel serverLevel, BlockState state) {
        Direction facing = state.getValue(PhantomLinkBlock.FACING);

        // Pull from any adjacent face except the output (front) face
        IItemHandler chosenHandler = null;
        int chosenSlot = -1;
        ItemStack toSend = ItemStack.EMPTY;

        for (Direction dir : Direction.values()) {
            if (dir == facing) continue; // skip the output face
            BlockPos checkPos = worldPosition.relative(dir);
            IItemHandler handler = serverLevel.getCapability(
                    Capabilities.ItemHandler.BLOCK, checkPos, null);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack sim = handler.extractItem(slot, 1, true);
                if (!sim.isEmpty()) {
                    toSend = sim.copy();
                    chosenHandler = handler;
                    chosenSlot = slot;
                    break;
                }
            }
            if (chosenHandler != null) break;
        }
        if (toSend.isEmpty()) return;

        // Find a paired link (same owner + channel)
        Map<Integer, Set<GlobalPos>> ownerMap = REGISTRY.get(ownerUUID);
        if (ownerMap == null) return;
        Set<GlobalPos> endpoints = ownerMap.get(channel);
        if (endpoints == null) return;

        for (GlobalPos endpoint : endpoints) {
            if (endpoint.pos().equals(worldPosition)
                    && endpoint.dimension().equals(serverLevel.dimension())) continue;

            ServerLevel remoteLevel = serverLevel.getServer().getLevel(endpoint.dimension());
            if (remoteLevel == null) continue;

            BlockEntity be = remoteLevel.getBlockEntity(endpoint.pos());
            if (!(be instanceof PhantomLinkBlockEntity remote)) continue;

            if (remote.canAccept(toSend, remoteLevel)) {
                ItemStack actual = chosenHandler.extractItem(chosenSlot, 1, false);
                if (!actual.isEmpty()) {
                    remote.acceptItem(actual, remoteLevel);
                    credits -= CREDITS_PER_ITEM;
                }
                return;
            }
        }
    }

    boolean canAccept(ItemStack stack, ServerLevel level) {
        Direction facing = getBlockState().getValue(PhantomLinkBlock.FACING);
        BlockPos outputPos = worldPosition.relative(facing);
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, outputPos, null);
        if (handler == null) return false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack, true).isEmpty()) return true;
        }
        return false;
    }

    void acceptItem(ItemStack stack, ServerLevel level) {
        Direction facing = getBlockState().getValue(PhantomLinkBlock.FACING);
        BlockPos outputPos = worldPosition.relative(facing);
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, outputPos, null);
        if (handler == null) { spawnDrop(stack, level, outputPos); return; }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            stack = handler.insertItem(slot, stack, false);
            if (stack.isEmpty()) return;
        }
        if (!stack.isEmpty()) spawnDrop(stack, level, outputPos);
    }

    private static void spawnDrop(ItemStack stack, ServerLevel level, BlockPos at) {
        net.minecraft.world.entity.item.ItemEntity ie =
                new net.minecraft.world.entity.item.ItemEntity(
                        level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack);
        ie.setDefaultPickUpDelay();
        level.addFreshEntity(ie);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (channel >= 0) tag.putInt("Channel", channel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        channel = tag.contains("Channel") ? tag.getInt("Channel") : -1;
    }
}
