package com.phantomstorage.item;

import com.phantomstorage.ModEntities;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * One-per-player non-stackable toggle item.
 * Inventory lives in player persistent data — never in this item.
 * Only one chest can exist per player at a time, across all dimensions.
 * The chest does not follow the player through portals; use this item in the
 * new dimension to bring it there (old copy is discarded automatically).
 * A lost/replaced summoner item still finds the existing chest on first use.
 *
 * CustomData tag: ChestActive (boolean, tooltip hint only)
 */
public class PhantomChestSummonerItem extends Item {

    private static final String KEY_ACTIVE = "ChestActive";

    /** 0 = base (chest only), 1 = upgraded (+ crafting), 2 = supreme (+ void filter) */
    private final int tier;

    public PhantomChestSummonerItem(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier > 0;
    }

    /** Cooldown applied on every use (summon or dismiss). 3 seconds = 60 ticks. */
    private static final int USE_COOLDOWN_TICKS = 60;

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Respect cooldown on the server side as well (client UI enforces it visually,
        // but a vanilla check here guards against packet-level bypasses).
        if (!player.getAbilities().instabuild && player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        PhantomChestEntity existing = findPlayerChest(serverLevel.getServer(), player);

        if (existing != null && existing.level() == serverLevel) {
            // Chest is here — dismiss it
            existing.saveInventoryTo(player);
            existing.saveFilterTo(player);
            existing.saveLinksTo(player);
            existing.discard();
            player.getPersistentData().remove(PhantomChestEntity.KEY_ENTITY_ID);
            setActive(stack, false);
            player.displayClientMessage(
                    Component.translatable("message.phantomstorage.dismissed"), true);
        } else {
            // Chest is in another dimension or doesn't exist — discard stray copy and summon here
            if (existing != null) {
                existing.saveInventoryTo(player);
                existing.saveFilterTo(player);
                existing.saveLinksTo(player);
                existing.discard();
            }
            PhantomChestEntity chest = ModEntities.PHANTOM_CHEST.get().create(serverLevel);
            if (chest == null) return InteractionResultHolder.fail(stack);

            chest.setOwnerUUID(player.getUUID());
            chest.setTier(this.tier);
            // Spawn at a random direction from the player so there's no look-direction bias.
            double spawnAngle = serverLevel.random.nextDouble() * Math.PI * 2;
            chest.moveTo(
                    player.getX() + Math.cos(spawnAngle) * PhantomChestEntity.SPAWN_OFFSET_H,
                    player.getY() + PhantomChestEntity.HOVER_Y_OFFSET,
                    player.getZ() + Math.sin(spawnAngle) * PhantomChestEntity.SPAWN_OFFSET_H,
                    0f, 0f);
            chest.loadInventoryFrom(player);
            chest.loadFilterFrom(player);
            chest.loadLinksFrom(player);
            serverLevel.addFreshEntity(chest);
            player.getPersistentData().putUUID(PhantomChestEntity.KEY_ENTITY_ID, chest.getUUID());
            setActive(stack, true);
            player.displayClientMessage(
                    Component.translatable("message.phantomstorage.summoned"), true);
        }

        // Apply cooldown after every successful use (summon or dismiss).
        // The vanilla hotbar renders a grey sweep overlay automatically.
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(stack.getItem(), USE_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.consume(stack);
    }

    /**
     * O(dimensions) lookup — reads the chest entity UUID from the player's PersistentData and
     * calls {@link ServerLevel#getEntity(UUID)}, which is a direct HashMap lookup inside each
     * level. Replaces the old world-scale AABB scan that iterated every loaded entity.
     */
    @Nullable
    public static PhantomChestEntity findPlayerChest(MinecraftServer server, Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(PhantomChestEntity.KEY_ENTITY_ID)) return null;
        UUID entityId = data.getUUID(PhantomChestEntity.KEY_ENTITY_ID);
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(entityId);
            if (e instanceof PhantomChestEntity chest) return chest;
        }
        // Stored UUID is stale (entity was discarded externally) — clean it up
        data.remove(PhantomChestEntity.KEY_ENTITY_ID);
        return null;
    }

    // ── DataComponents helpers ────────────────────────────────────────────────

    private static CompoundTag readTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    private static void setActive(ItemStack stack, boolean active) {
        CompoundTag tag = readTag(stack);
        tag.putBoolean(KEY_ACTIVE, active);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Clears the ChestActive flag on every summoner found in the player's
     * main inventory and offhand. Called by dimension-change and similar events
     * that discard the chest entity so the tooltip stays in sync with reality.
     */
    public static void deactivateInInventory(Player player) {
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof PhantomChestSummonerItem) setActive(s, false);
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (s.getItem() instanceof PhantomChestSummonerItem) setActive(s, false);
        }
    }

    public static boolean isActive(ItemStack stack) {
        return readTag(stack).getBoolean(KEY_ACTIVE);
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable(isActive(stack)
                ? "tooltip.phantomstorage.summoner.active"
                : "tooltip.phantomstorage.summoner.inactive"));
    }
}
