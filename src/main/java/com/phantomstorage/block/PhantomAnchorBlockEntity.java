package com.phantomstorage.block;

import com.phantomstorage.ComparatorMode;
import com.phantomstorage.ModBlockEntities;
import com.phantomstorage.ModParticles;
import com.phantomstorage.RedstoneInputMode;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.inventory.PhantomAnchorMenu;
import com.phantomstorage.item.PhantomChestSummonerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

public class PhantomAnchorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int DEFAULT_RADIUS = 4;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 16;

    /**
     * Self-contained rate limit for redstone-triggered dock/release — deliberately NOT tied to
     * the owner's summon-item cooldown (ItemCooldowns lives on the player entity, so it silently
     * no-ops whenever the owner is offline, which is exactly when you'd want a redstone circuit
     * to keep working).
     */
    private static final int MIN_TRIGGER_INTERVAL_TICKS = 10;

    // Covers the full extent of any dimension, matching DimensionEvents' orphan-sweep bounds.
    private static final AABB WORLD_BOUNDS = new AABB(-3.0E7, -512, -3.0E7, 3.0E7, 4096, 3.0E7);

    @Nullable private UUID ownerUUID;
    @Nullable private UUID dockedChestId;
    private int radius = DEFAULT_RADIUS;

    private RedstoneInputMode inputMode = RedstoneInputMode.NONE;
    private ComparatorMode comparatorMode = ComparatorMode.NONE;
    private boolean lastPowered = false;
    private long lastTriggeredTick = Long.MIN_VALUE;
    private int lastComparatorSignal = -1;

    public PhantomAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHANTOM_ANCHOR.get(), pos, state);
    }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.phantomstorage.phantom_anchor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new PhantomAnchorMenu(id, playerInv, this);
    }

    // ── Owner / radius ────────────────────────────────────────────────────────

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
        setChanged();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int value) {
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, value));
        setChanged();
    }

    public boolean isDocked() {
        return dockedChestId != null;
    }

    // ── Redstone / comparator modes ──────────────────────────────────────────

    public RedstoneInputMode getInputMode() {
        return inputMode;
    }

    public void cycleInputMode() {
        inputMode = inputMode.cycle();
        setChanged();
    }

    public ComparatorMode getComparatorMode() {
        return comparatorMode;
    }

    public void cycleComparatorMode() {
        comparatorMode = comparatorMode.cycle();
        setChanged();
        pushComparatorUpdate();
    }

    /** Called from the block's neighborChanged whenever the incoming redstone signal may have changed. */
    public void onRedstoneChanged(boolean powered) {
        if (level == null || level.isClientSide || inputMode == RedstoneInputMode.NONE) {
            lastPowered = powered;
            return;
        }

        boolean risingEdge = powered && !lastPowered;
        lastPowered = powered;

        Boolean wantDocked = switch (inputMode) {
            case ACTIVE_HIGH -> powered;
            case ACTIVE_LOW -> !powered;
            case PULSE_TOGGLE -> risingEdge ? !isDocked() : null;
            case NONE -> null;
        };
        if (wantDocked == null || wantDocked == isDocked()) return;

        long now = level.getGameTime();
        if (now - lastTriggeredTick < MIN_TRIGGER_INTERVAL_TICKS) return;
        lastTriggeredTick = now;

        if (wantDocked) {
            PhantomChestEntity chest = findOwnerChestInWorld();
            if (chest != null && chest.level() == level) {
                dockChest(chest);
            }
        } else {
            releaseDockedChest();
        }
    }

    // ── Comparator output ────────────────────────────────────────────────────

    public int getComparatorOutput() {
        return switch (comparatorMode) {
            case NONE -> 0;
            case PRESENCE -> isDocked() ? 15 : 0;
            case FULLNESS -> fullnessSignal();
        };
    }

    private int fullnessSignal() {
        if (dockedChestId == null || !(level instanceof ServerLevel serverLevel)) return 0;
        Entity e = serverLevel.getEntity(dockedChestId);
        if (!(e instanceof PhantomChestEntity chest)) return 0;
        return AbstractContainerMenu.getRedstoneSignalFromContainer(chest.getInventory());
    }

    private void pushComparatorUpdate() {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhantomAnchorBlockEntity be) {
        if (be.isDocked() && level instanceof ServerLevel serverLevel && (level.getGameTime() & 0xF) == 0) {
            be.spawnDockedParticle(serverLevel);
        }

        // Comparator refresh only while docked with FULLNESS output selected, to keep it reasonably live.
        if (be.comparatorMode != ComparatorMode.FULLNESS || !be.isDocked()) return;
        if ((level.getGameTime() & 0x7) != 0) return; // every 8 ticks
        int signal = be.getComparatorOutput();
        if (signal != be.lastComparatorSignal) {
            be.lastComparatorSignal = signal;
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /**
     * Mirrors the docked chest's own ambient soul particle (same cadence, same spawn box, same
     * (0, 0.02, 0) drift) using the anchor's own longer-lived particle variant. count=0 spawns
     * exactly one particle at the given position with velocity (xOffset,yOffset,zOffset)*speed —
     * no positional jitter or extra randomness added by the packet itself.
     */
    private void spawnDockedParticle(ServerLevel level) {
        level.sendParticles(ModParticles.ANCHOR_SOUL.get(),
                worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8,
                worldPosition.getY() + 0.6 + level.random.nextDouble() * 0.75,
                worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8,
                0, 0.0, 0.02, 0.0, 1.0);
    }

    // ── Docking ───────────────────────────────────────────────────────────────

    /** Owner-only. Docks their active chest here, or releases it if already docked. */
    public void toggleDock(ServerPlayer player) {
        if (dockedChestId != null) {
            releaseDockedChest();
            player.displayClientMessage(
                    Component.translatable("message.phantomstorage.anchor.undocked"), true);
            return;
        }

        PhantomChestEntity chest = PhantomChestSummonerItem.findPlayerChest(player.getServer(), player);
        if (chest == null || chest.level() != level) {
            player.displayClientMessage(
                    Component.translatable("message.phantomstorage.anchor.no_chest"), true);
            return;
        }

        dockChest(chest);
        player.displayClientMessage(
                Component.translatable("message.phantomstorage.anchor.docked"), true);
    }

    /**
     * Finds a live Phantom Chest entity owned by this block's owner, anywhere in the world —
     * independent of the owner being online, since docking doesn't need a live Player reference,
     * only the entity itself. (In practice the chest is dismissed on the owner's logout, same as
     * everywhere else in the mod, so this is really "docks while you're online and playing", not
     * unattended automation while you're away — redstone can still trigger it hands-free, but it
     * can't act on a chest that no longer exists.)
     */
    @Nullable
    private PhantomChestEntity findOwnerChestInWorld() {
        if (!(level instanceof ServerLevel serverLevel) || ownerUUID == null) return null;
        for (ServerLevel lvl : serverLevel.getServer().getAllLevels()) {
            for (PhantomChestEntity chest : lvl.getEntitiesOfClass(
                    PhantomChestEntity.class, WORLD_BOUNDS,
                    c -> ownerUUID.equals(c.getOwnerUUID()))) {
                return chest;
            }
        }
        return null;
    }

    private void dockChest(PhantomChestEntity chest) {
        // Release any previous anchor (freeform or another block) so it doesn't keep a stale reference.
        if (chest.isAnchored()) {
            chest.undock();
        }
        chest.dockToBlock(worldPosition);
        dockedChestId = chest.getUUID();
        setChanged();
        pushComparatorUpdate();
        setChunkForced(true);
    }

    /** Called by the chest itself when it undocks by any means (GUI eject, sneak-toggle, recall, etc.). */
    public void onChestUndocked(UUID chestId) {
        if (chestId.equals(dockedChestId)) {
            dockedChestId = null;
            setChanged();
            pushComparatorUpdate();
            setChunkForced(false);
        }
    }

    private void releaseDockedChest() {
        if (dockedChestId != null && level instanceof ServerLevel serverLevel) {
            Entity e = serverLevel.getEntity(dockedChestId);
            if (e instanceof PhantomChestEntity chest) {
                chest.undock();
            }
        }
        dockedChestId = null;
        setChanged();
        pushComparatorUpdate();
        setChunkForced(false); // belt-and-suspenders: covers the case where the entity lookup above found nothing
    }

    /**
     * Forces just this block's own chunk to stay loaded while a chest is docked here — never a
     * radius, even though the chest's roam radius can itself span into a neighboring chunk. If
     * the chest wanders past this chunk's edge while docked, normal chunk-load rules apply to it
     * same as any other entity; only the anchor's home chunk is kept loaded.
     */
    private void setChunkForced(boolean forced) {
        if (level instanceof ServerLevel serverLevel) {
            ChunkPos cp = new ChunkPos(worldPosition);
            serverLevel.setChunkForced(cp.x, cp.z, forced);
        }
    }

    /** Block broken/replaced while a chest was docked — release it so it resumes following its owner. */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            releaseDockedChest();
        }
        super.setRemoved();
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (dockedChestId != null) tag.putUUID("DockedChest", dockedChestId);
        tag.putInt("Radius", radius);
        tag.putString("InputMode", inputMode.name());
        tag.putString("ComparatorMode", comparatorMode.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        dockedChestId = tag.hasUUID("DockedChest") ? tag.getUUID("DockedChest") : null;
        radius = tag.contains("Radius")
                ? Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, tag.getInt("Radius")))
                : DEFAULT_RADIUS;
        inputMode = tag.contains("InputMode")
                ? RedstoneInputMode.valueOf(tag.getString("InputMode")) : RedstoneInputMode.NONE;
        comparatorMode = tag.contains("ComparatorMode")
                ? ComparatorMode.valueOf(tag.getString("ComparatorMode")) : ComparatorMode.NONE;
    }
}
