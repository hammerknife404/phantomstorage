package com.phantomstorage.block;

import com.phantomstorage.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class PhantomAnchorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int DEFAULT_RADIUS = 4;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 16;

    @Nullable private UUID ownerUUID;
    @Nullable private UUID dockedChestId;
    private int radius = DEFAULT_RADIUS;

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

        // Release any previous anchor (freeform or another block) so it doesn't keep a stale reference.
        if (chest.isAnchored()) {
            chest.undock();
        }
        chest.dockToBlock(worldPosition);
        dockedChestId = chest.getUUID();
        setChanged();
        player.displayClientMessage(
                Component.translatable("message.phantomstorage.anchor.docked"), true);
    }

    /** Called by the chest itself when it undocks by any means (GUI eject, sneak-toggle, recall, etc.). */
    public void onChestUndocked(UUID chestId) {
        if (chestId.equals(dockedChestId)) {
            dockedChestId = null;
            setChanged();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        dockedChestId = tag.hasUUID("DockedChest") ? tag.getUUID("DockedChest") : null;
        radius = tag.contains("Radius")
                ? Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, tag.getInt("Radius")))
                : DEFAULT_RADIUS;
    }
}
