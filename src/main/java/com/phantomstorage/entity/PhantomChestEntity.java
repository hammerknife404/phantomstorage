package com.phantomstorage.entity;

import com.phantomstorage.inventory.PhantomChestMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class PhantomChestEntity extends PathfinderMob implements MenuProvider {

    public static final int INVENTORY_SIZE = 54; // 6 rows × 9 cols, matches GUI texture

    /** Key used to store the chest entity's UUID in the owner's PersistentData for O(1) lookup. */
    public static final String KEY_ENTITY_ID = "PhantomChest.EntityId";

    // Direct follow-movement constants (no A* pathfinding)
    private static final double MIN_FOLLOW_DIST_SQ = 2.5 * 2.5;   // personal-space radius
    private static final double MAX_FOLLOW_DIST_SQ = 20.0 * 20.0; // snap-teleport threshold
    private static final double FOLLOW_SPEED       = 0.3;          // blocks per tick (~6 b/s)

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PhantomChestEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OPEN_COUNT =
            SynchedEntityData.defineId(PhantomChestEntity.class, EntityDataSerializers.INT);

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);


    public PhantomChestEntity(EntityType<? extends PhantomChestEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        // Movement is handled directly in tick() via followOwner() — no FlyingMoveControl
        // or FlyingPathNavigation needed, so no A* is run.
        // Inventory is saved to player PersistentData once, when the menu is closed (or the
        // chest is dismissed). No per-slot listener — that fired on every item move and
        // serialised all 54 slots to NBT each time, causing unnecessary GC pressure.
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OPEN_COUNT, 0);
    }

    // ── Owner ─────────────────────────────────────────────────────────────────

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public Player getOwner() {
        UUID uuid = getOwnerUUID();
        return uuid != null ? this.level().getPlayerByUUID(uuid) : null;
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Serialises inventory slots into a ListTag under the given key. */
    public ListTag saveInventory(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                list.add(stack.save(provider, slot));
            }
        }
        return list;
    }

    public void loadInventory(ListTag list, HolderLookup.Provider provider) {
        inventory.clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            int index = slot.getByte("Slot") & 0xFF;
            if (index < inventory.getContainerSize()) {
                ItemStack.parse(provider, slot).ifPresent(s -> inventory.setItem(index, s));
            }
        }
    }

    /** Loads the 54-slot inventory from the owner player's persistent data. */
    public void loadInventoryFrom(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains("PhantomChestInventory")) {
            loadInventory(data.getList("PhantomChestInventory", 10), this.level().registryAccess());
        } else {
            inventory.clearContent();
        }
    }

    /** Saves the 54-slot inventory into the owner player's persistent data. */
    public void saveInventoryTo(Player player) {
        player.getPersistentData().put("PhantomChestInventory",
                saveInventory(this.level().registryAccess()));
    }

    // ── Open / close ──────────────────────────────────────────────────────────

    public void onMenuOpened() {
        int prev = this.entityData.get(OPEN_COUNT);
        this.entityData.set(OPEN_COUNT, prev + 1);
        if (prev == 0) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS,
                    0.5f, this.random.nextFloat() * 0.1f + 0.9f);
        }
    }

    public void onMenuClosed() {
        int prev = this.entityData.get(OPEN_COUNT);
        int next = Math.max(0, prev - 1);
        this.entityData.set(OPEN_COUNT, next);
        if (next == 0 && prev > 0) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS,
                    0.5f, this.random.nextFloat() * 0.1f + 0.9f);
        }
    }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.phantomstorage.phantom_chest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        if (!player.getUUID().equals(getOwnerUUID())) return null;
        return new PhantomChestMenu(id, playerInventory, inventory, this);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            if (player.getUUID().equals(getOwnerUUID())) {
                onMenuOpened();
                player.openMenu(this);
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(
                        Component.translatable("message.phantomstorage.not_owner"), true);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // ── Tick / particles ──────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            followOwner();
        }
        if (this.level().isClientSide && this.tickCount % 16 == 0) {
            // Chest visual spans getY() (feet) to getY() + 0.9375 (lid top) at scale 1.0.
            // Spawn particles within the body/lid zone so they appear to rise from the chest.
            this.level().addParticle(
                    ParticleTypes.SOUL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getY() + 0.1 + this.random.nextDouble() * 0.75,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                    0, 0.02, 0);
        }
    }

    /**
     * Moves the chest toward the hover-point behind the owner each tick.
     * Uses direct linear stepping instead of A* pathfinding — appropriate for
     * an entity that always moves through open air next to its owner.
     * Complexity: O(1) per tick per chest.
     */
    private void followOwner() {
        Player owner = getOwner();
        if (owner == null) return;

        // Hover just behind and slightly above the player's shoulder (same offset as before)
        Vec3 target = owner.position()
                .add(owner.getLookAngle().scale(-1.5))
                .add(0, 1.2, 0);

        double distSq = this.distanceToSqr(target.x, target.y, target.z);
        if (distSq <= MIN_FOLLOW_DIST_SQ) return; // already in position — nothing to do

        if (distSq > MAX_FOLLOW_DIST_SQ) {
            // Player sprinted or teleported far away — snap rather than chase
            this.teleportTo(target.x, target.y, target.z);
            return;
        }

        // Clamp step length so the chest never overshoots on the final approach
        double step = Math.min(Math.sqrt(distSq), FOLLOW_SPEED);
        Vec3 dir = target.subtract(this.position()).normalize().scale(step);
        this.setPos(this.getX() + dir.x, this.getY() + dir.y, this.getZ() + dir.z);
        this.getLookControl().setLookAt(owner, 10f, 10f);
    }

    // ── Combat / physics ──────────────────────────────────────────────────────

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amount) {
        return false; // completely invulnerable
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerUUID() != null) {
            tag.putUUID("Owner", getOwnerUUID());
        }
        tag.put("Inventory", saveInventory(this.level().registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("Inventory")) {
            loadInventory(tag.getList("Inventory", 10), this.level().registryAccess());
        }
    }

    // ── Teleport to another dimension (follows player through portals) ─────────

    /**
     * Called by the dimension-change event handler. Drops to the new dimension
     * at the player's feet and clears the old entity.
     */
}
