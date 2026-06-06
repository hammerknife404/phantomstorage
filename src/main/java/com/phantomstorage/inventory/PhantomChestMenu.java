package com.phantomstorage.inventory;

import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

public class PhantomChestMenu extends AbstractContainerMenu {

    // ── Tab IDs ───────────────────────────────────────────────────────────────
    public static final int TAB_CHEST  = 0;
    public static final int TAB_CRAFT  = 1;
    public static final int TAB_FILTER = 2;

    // ── Slot layout ───────────────────────────────────────────────────────────
    public static final int CHEST_SIZE       = 54;
    public static final int CRAFT_RESULT     = 54;
    public static final int CRAFT_GRID_START = 55;
    public static final int FILTER_START     = 64;
    public static final int PLAYER_INV_START = 73;
    public static final int HOTBAR_START     = 100;
    public static final int TOTAL_SLOTS      = 109;

    // ── Screen coordinates (slot top-left, relative to leftPos/topPos) ───────
    private static final int CHEST_X         = 8;
    private static final int CHEST_Y         = 18;
    private static final int PLAYER_INV_X    = 8;
    private static final int PLAYER_INV_Y    = 140;
    private static final int HOTBAR_Y        = 198;

    public static final int CRAFT_GRID_X     = 30;
    public static final int CRAFT_GRID_Y     = 20;
    public static final int CRAFT_RESULT_X   = 124;
    public static final int CRAFT_RESULT_Y   = 38;

    public static final int FILTER_X         = 61;
    public static final int FILTER_Y         = 35;

    // ── State — [0]=active tab, [1]=entity tier, [2]=flow state (0/1/2) ──────
    private final ContainerData uiData = new SimpleContainerData(3);

    private final Container chestContainer;
    private final SimpleContainer filterContainer;
    @Nullable private final PhantomChestEntity entity;

    private final Level level;
    private final Player player;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();

    /** Server-side constructor. */
    public PhantomChestMenu(int id, Inventory playerInventory, Container chestContainer,
                            SimpleContainer filterContainer, PhantomChestEntity entity) {
        super(ModMenuTypes.PHANTOM_CHEST_MENU.get(), id);
        this.chestContainer   = chestContainer;
        this.filterContainer  = filterContainer;
        this.entity           = entity;
        this.player           = playerInventory.player;
        this.level            = playerInventory.player.level();
        this.craftSlots       = new TransientCraftingContainer(this, 3, 3);

        checkContainerSize(chestContainer, CHEST_SIZE);
        if (entity != null) uiData.set(1, entity.getTier());
        addDataSlots(uiData);

        addChestSlots();
        addCraftingSlots();
        addFilterSlots();
        addPlayerSlots(playerInventory);
    }

    /** Client-side constructor (from network). */
    public PhantomChestMenu(int id, Inventory playerInventory, FriendlyByteBuf ignored) {
        this(id, playerInventory, new SimpleContainer(CHEST_SIZE), new SimpleContainer(9), null);
    }

    // ── Slot registration ─────────────────────────────────────────────────────

    private void addChestSlots() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new TabSlot(chestContainer, row * 9 + col,
                        CHEST_X + col * 18, CHEST_Y + row * 18, TAB_CHEST));
            }
        }
    }

    private void addCraftingSlots() {
        addSlot(new CraftResultTabSlot(player, craftSlots, resultSlots, 0,
                CRAFT_RESULT_X, CRAFT_RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new TabSlot(craftSlots, col + row * 3,
                        CRAFT_GRID_X + col * 18, CRAFT_GRID_Y + row * 18, TAB_CRAFT));
            }
        }
    }

    private void addFilterSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new GhostTabSlot(filterContainer, col + row * 3,
                        FILTER_X + col * 18, FILTER_Y + row * 18));
            }
        }
    }

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    // ── Tab ───────────────────────────────────────────────────────────────────

    public int getActiveTab()  { return uiData.get(0); }
    public int getEntityTier() { return uiData.get(1); }
    public int getFlowState()  { return uiData.get(2); }

    @Override
    public void broadcastChanges() {
        if (entity != null) {
            uiData.set(1, entity.getTier());
            uiData.set(2, entity.getFlowState());
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == TAB_CHEST) {
            uiData.set(0, TAB_CHEST);
            return true;
        }
        if (id == TAB_CRAFT && getEntityTier() >= 1) {
            uiData.set(0, TAB_CRAFT);
            return true;
        }
        if (id == TAB_FILTER && getEntityTier() >= 2) {
            uiData.set(0, TAB_FILTER);
            return true;
        }
        return false;
    }

    // ── Crafting ──────────────────────────────────────────────────────────────

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftSlots && !level.isClientSide()) {
            updateCraftingResult();
        }
    }

    private void updateCraftingResult() {
        ServerPlayer sp = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots.asCraftInput(), level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (resultSlots.setRecipeUsed(level, sp, holder)) {
                ItemStack assembled = holder.value().assemble(craftSlots.asCraftInput(), level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(CRAFT_RESULT, result);
        sp.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), CRAFT_RESULT, result));
    }

    // ── Ghost slot clicks ─────────────────────────────────────────────────────

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= FILTER_START && slotId < PLAYER_INV_START) {
            Slot slot = slots.get(slotId);
            ItemStack cursor = getCarried();
            slot.set(cursor.isEmpty() ? ItemStack.EMPTY : cursor.copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // ── Quick move ────────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= FILTER_START && index < PLAYER_INV_START) {
            slots.get(index).set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == CRAFT_RESULT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, result);
        } else if (index < CHEST_SIZE) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else if (index < FILTER_START) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else if (index >= PLAYER_INV_START && index < HOTBAR_START) {
            boolean moved = switch (getActiveTab()) {
                case TAB_CHEST -> moveItemStackTo(stack, 0, CHEST_SIZE, false);
                case TAB_CRAFT -> moveItemStackTo(stack, CRAFT_GRID_START, FILTER_START, false);
                default -> false;
            };
            if (!moved && !moveItemStackTo(stack, HOTBAR_START, TOTAL_SLOTS, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = switch (getActiveTab()) {
                case TAB_CHEST -> moveItemStackTo(stack, 0, CHEST_SIZE, false);
                case TAB_CRAFT -> moveItemStackTo(stack, CRAFT_GRID_START, FILTER_START, false);
                default -> false;
            };
            if (!moved && !moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_START, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean stillValid(Player player) {
        return entity == null || entity.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (entity != null) {
            entity.saveInventoryTo(player);
            entity.saveFilterTo(player);
            entity.onMenuClosed();
        }
    }

    // ── Inner slot classes ────────────────────────────────────────────────────

    private class TabSlot extends Slot {
        private final int tab;
        TabSlot(Container c, int index, int x, int y, int tab) {
            super(c, index, x, y);
            this.tab = tab;
        }
        @Override public boolean isActive() { return getActiveTab() == tab; }
    }

    private class CraftResultTabSlot extends ResultSlot {
        CraftResultTabSlot(Player p, TransientCraftingContainer c, ResultContainer r, int idx, int x, int y) {
            super(p, c, r, idx, x, y);
        }
        @Override public boolean isActive() { return getActiveTab() == TAB_CRAFT; }
    }

    private class GhostTabSlot extends Slot {
        GhostTabSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }
        @Override public boolean isActive() { return getActiveTab() == TAB_FILTER; }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
        @Override public int getMaxStackSize() { return 1; }
    }
}
