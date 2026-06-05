package com.phantomstorage.entity;

import com.phantomstorage.inventory.PhantomChestMenu;
import com.phantomstorage.inventory.VoidFilterContainer;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class PhantomChestEntity extends PathfinderMob implements MenuProvider {

    public static final int INVENTORY_SIZE = 54; // 6 rows × 9 cols, matches GUI texture

    /** Key used to store the chest entity's UUID in the owner's PersistentData for O(1) lookup. */
    public static final String KEY_ENTITY_ID = "PhantomChest.EntityId";

    // ── Follow constants ──────────────────────────────────────────────────────
    /** Height above the player's foot position the chest hovers at. */
    public  static final double HOVER_Y_OFFSET    = 1.5;
    /** Horizontal distance from the player used as the spawn offset. */
    public  static final double SPAWN_OFFSET_H    = 2.5;
    /** Start following when the chest is more than this many blocks from the owner. */
    private static final double FOLLOW_START_DIST = 6.0;
    /** Stop following and enter idle when within this many blocks of the owner. */
    private static final double FOLLOW_STOP_DIST  = 3.5;
    /** Navigator speed modifier while following. */
    private static final double FOLLOW_SPEED      = 1.0;
    /** Teleport threshold — only snap when genuinely out of range (tp, login). */
    private static final double SNAP_DIST_SQ      = 20.0 * 20.0;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PhantomChestEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OPEN_COUNT =
            SynchedEntityData.defineId(PhantomChestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(PhantomChestEntity.class, EntityDataSerializers.INT);

    public static final String KEY_FILTER = "PhantomChestFilter";

    private final SimpleContainer filterSlots = new SimpleContainer(9);
    private final VoidFilterContainer inventory = new VoidFilterContainer(INVENTORY_SIZE, filterSlots);

    // Flow state set by EtherealLinkBlockEntity; read by PhantomChestMenu.broadcastChanges()
    // 0 = idle, 1 = items flowing in, 2 = items flowing out
    private int flowState = 0;

    public PhantomChestEntity(EntityType<? extends PhantomChestEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // Inventory is saved to player PersistentData once when the menu is closed (or the
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
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(false);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OPEN_COUNT, 0);
        builder.define(TIER, 0);
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

    // ── Tier ──────────────────────────────────────────────────────────────────

    public int getTier() {
        return this.entityData.get(TIER);
    }

    public void setTier(int tier) {
        this.entityData.set(TIER, tier);
    }

    // ── Flow state ────────────────────────────────────────────────────────────

    public int getFlowState() { return flowState; }

    public void setFlowState(int state) { this.flowState = state; }

    // ── Inventory ─────────────────────────────────────────────────────────────

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Serialises inventory slots into a ListTag. */
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
                // loadItem bypasses the void filter so saved items are always restored
                ItemStack.parse(provider, slot).ifPresent(s -> inventory.loadItem(index, s));
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    public SimpleContainer getFilterSlots() {
        return filterSlots;
    }

    public ListTag saveFilter(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < filterSlots.getContainerSize(); i++) {
            ItemStack stack = filterSlots.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                list.add(stack.save(provider, slot));
            }
        }
        return list;
    }

    public void loadFilter(ListTag list, HolderLookup.Provider provider) {
        filterSlots.clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            int index = slot.getByte("Slot") & 0xFF;
            if (index < filterSlots.getContainerSize()) {
                ItemStack.parse(provider, slot).ifPresent(s -> filterSlots.setItem(index, s));
            }
        }
    }

    public void saveFilterTo(Player player) {
        player.getPersistentData().put(KEY_FILTER, saveFilter(this.level().registryAccess()));
    }

    public void loadFilterFrom(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY_FILTER)) {
            loadFilter(data.getList(KEY_FILTER, 10), this.level().registryAccess());
        } else {
            filterSlots.clearContent();
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
        return new PhantomChestMenu(id, playerInventory, inventory, filterSlots, this);
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
            // Hard snap to owner when genuinely out of range (tp / login gap).
            // Normal follow is handled by FollowOwnerGoal via FlyingPathNavigation.
            Player owner = getOwner();
            if (owner != null && this.distanceToSqr(owner) > SNAP_DIST_SQ) {
                this.teleportTo(owner.getX(), owner.getY() + HOVER_Y_OFFSET, owner.getZ());
            }
        }
        if (this.level().isClientSide && this.tickCount % 16 == 0) {
            // Chest visual spans getY() (feet) to getY() + 0.9375 (lid top) at scale 1.0.
            this.level().addParticle(
                    ParticleTypes.SOUL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getY() + 0.1 + this.random.nextDouble() * 0.75,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                    0, 0.02, 0);
        }
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
        tag.putInt("Tier", getTier());
        tag.put("Inventory", saveInventory(this.level().registryAccess()));
        tag.put("VoidFilter", saveFilter(this.level().registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("Tier")) {
            setTier(tag.getInt("Tier"));
        }
        if (tag.contains("Inventory")) {
            loadInventory(tag.getList("Inventory", 10), this.level().registryAccess());
        }
        if (tag.contains("VoidFilter")) {
            loadFilter(tag.getList("VoidFilter", 10), this.level().registryAccess());
        }
    }

    // ── Follow goal ───────────────────────────────────────────────────────────

    /**
     * Allay-style follow: paths to the owner via FlyingPathNavigation when the chest
     * drifts beyond FOLLOW_START_DIST, stops once within FOLLOW_STOP_DIST.
     * The navigator handles block avoidance; no manual escapeY needed.
     */
    private static class FollowOwnerGoal extends Goal {

        private final PhantomChestEntity chest;
        @Nullable private Player owner;
        private int recalcTimer;

        FollowOwnerGoal(PhantomChestEntity chest) {
            this.chest = chest;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = chest.getOwner();
            if (o == null || o.isSpectator()) return false;
            owner = o;
            return chest.distanceToSqr(o) > FOLLOW_START_DIST * FOLLOW_START_DIST;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null
                    && owner.isAlive()
                    && !chest.getNavigation().isDone()
                    && chest.distanceToSqr(owner) > FOLLOW_STOP_DIST * FOLLOW_STOP_DIST;
        }

        @Override
        public void start() {
            recalcTimer = 0;
            pathToOwner();
        }

        @Override
        public void stop() {
            owner = null;
            chest.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            chest.getLookControl().setLookAt(owner, 10f, 10f);
            if (--recalcTimer <= 0) {
                recalcTimer = 10;
                pathToOwner();
            }
        }

        private void pathToOwner() {
            chest.getNavigation().moveTo(owner, FOLLOW_SPEED);
        }
    }
}
