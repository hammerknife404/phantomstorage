package com.phantomstorage.block;

import com.phantomstorage.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhantomLinkBlockEntity extends BlockEntity {

    // 80 credits/tick × 20 ticks/sec = 1600 max; 100 per item → 16 items/sec
    private static final int CREDITS_PER_TICK = 80;
    private static final int CREDITS_PER_ITEM = 100;
    private static final int MAX_CREDITS      = 1600;

    // ownerUUID → channel → set of GlobalPos for all links in that channel
    private static final Map<UUID, Map<DyeColor, Set<GlobalPos>>> REGISTRY =
            new ConcurrentHashMap<>();

    @Nullable private UUID ownerUUID;
    @Nullable private DyeColor channel;
    private int credits = 0;

    public PhantomLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHANTOM_LINK.get(), pos, state);
    }

    // ── Registry ──────────────────────────────────────────────────────────────

    private void registerSelf() {
        if (ownerUUID == null || channel == null || level == null) return;
        GlobalPos gpos = GlobalPos.of(level.dimension(), worldPosition);
        REGISTRY.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
                .add(gpos);
    }

    private void deregisterSelf() {
        if (ownerUUID == null || channel == null || level == null) return;
        GlobalPos gpos = GlobalPos.of(level.dimension(), worldPosition);
        Map<DyeColor, Set<GlobalPos>> ownerMap = REGISTRY.get(ownerUUID);
        if (ownerMap == null) return;
        Set<GlobalPos> positions = ownerMap.get(channel);
        if (positions != null) {
            positions.remove(gpos);
            if (positions.isEmpty()) ownerMap.remove(channel);
        }
        if (ownerMap.isEmpty()) REGISTRY.remove(ownerUUID);
    }

    /** Called on server stop to clear stale entries between dev-env sessions. */
    public static void onServerStopping(LevelEvent.Unload event) {
        // Only clear on server-side unload; client levels share the same event
        if (!event.getLevel().isClientSide()) REGISTRY.clear();
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

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setOwnerUUID(@Nullable UUID uuid) {
        if (level != null && !level.isClientSide()) deregisterSelf();
        this.ownerUUID = uuid;
        if (level != null && !level.isClientSide()) registerSelf();
        setChanged();
    }

    public void setChannel(DyeColor color) {
        if (level != null && !level.isClientSide()) deregisterSelf();
        this.channel = color;
        if (level != null && !level.isClientSide()) registerSelf();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Nullable public DyeColor getChannel()   { return channel; }
    @Nullable public UUID    getOwnerUUID()  { return ownerUUID; }

    // ── Transfer ──────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PhantomLinkBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.credits = Math.min(be.credits + CREDITS_PER_TICK, MAX_CREDITS);
        if (be.credits < CREDITS_PER_ITEM) return;
        if (be.channel == null || be.ownerUUID == null) return;
        be.doTransfer(serverLevel, state);
    }

    private void doTransfer(ServerLevel serverLevel, BlockState state) {
        Direction facing = state.getValue(PhantomLinkBlock.FACING);

        // Pull from the block on the back face
        BlockPos inputPos = worldPosition.relative(facing.getOpposite());
        IItemHandler inputHandler = serverLevel.getCapability(
                Capabilities.ItemHandler.BLOCK, inputPos, facing);
        if (inputHandler == null) return;

        // Simulate extract to find a transferable item
        ItemStack toSend = ItemStack.EMPTY;
        int sourceSlot = -1;
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack sim = inputHandler.extractItem(slot, 1, true);
            if (!sim.isEmpty()) {
                toSend = sim.copy();
                sourceSlot = slot;
                break;
            }
        }
        if (toSend.isEmpty()) return;

        // Find a paired link (same owner + channel) that can accept it
        Map<DyeColor, Set<GlobalPos>> ownerMap = REGISTRY.get(ownerUUID);
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
                ItemStack actual = inputHandler.extractItem(sourceSlot, 1, false);
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
                Capabilities.ItemHandler.BLOCK, outputPos, facing.getOpposite());
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
                Capabilities.ItemHandler.BLOCK, outputPos, facing.getOpposite());
        if (handler == null) {
            spawnDrop(stack, level, outputPos);
            return;
        }
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

    // ── Sync (channel tint needs to reach the client) ─────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        if (channel != null) tag.putString("Channel", channel.getSerializedName());
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (channel != null) tag.putString("Channel", channel.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        String name = tag.getString("Channel");
        channel = name.isEmpty() ? null
                : Arrays.stream(DyeColor.values())
                        .filter(c -> c.getSerializedName().equals(name))
                        .findFirst().orElse(null);
    }
}
