package com.phantomstorage.events;

import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.item.PhantomChestSummonerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

@EventBusSubscriber(modid = PhantomStorageMod.MODID)
public class DimensionEvents {

    // Covers the full extent of any Minecraft dimension for the orphan sweep.
    private static final AABB WORLD_BOUNDS = new AABB(-3.0E7, -512, -3.0E7, 3.0E7, 4096, 3.0E7);

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy all phantom chest player data from original to respawned player.
        CompoundTag original = event.getOriginal().getPersistentData();
        CompoundTag newData  = event.getEntity().getPersistentData();

        if (original.contains("PhantomChestInventory")) {
            newData.put("PhantomChestInventory",
                    original.getList("PhantomChestInventory", Tag.TAG_COMPOUND).copy());
        }
        if (original.contains(PhantomChestEntity.KEY_FILTER)) {
            newData.put(PhantomChestEntity.KEY_FILTER,
                    original.getList(PhantomChestEntity.KEY_FILTER, Tag.TAG_COMPOUND).copy());
        }
        if (original.contains(PhantomChestEntity.KEY_LINKS)) {
            newData.put(PhantomChestEntity.KEY_LINKS,
                    original.getList(PhantomChestEntity.KEY_LINKS, Tag.TAG_COMPOUND).copy());
        }
        if (original.hasUUID(PhantomChestEntity.KEY_ENTITY_ID)) {
            newData.putUUID(PhantomChestEntity.KEY_ENTITY_ID,
                    original.getUUID(PhantomChestEntity.KEY_ENTITY_ID));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().getServer() == null) return;

        CompoundTag data = event.getEntity().getPersistentData();
        if (!data.hasUUID(PhantomChestEntity.KEY_ENTITY_ID)) return;

        UUID entityId = data.getUUID(PhantomChestEntity.KEY_ENTITY_ID);
        ServerLevel oldLevel = event.getEntity().getServer().getLevel(event.getFrom());
        if (oldLevel != null) {
            Entity e = oldLevel.getEntity(entityId);
            if (e instanceof PhantomChestEntity chest) {
                chest.saveInventoryTo(event.getEntity());
                chest.saveFilterTo(event.getEntity());
                chest.saveLinksTo(event.getEntity());
                chest.discard();
            }
        }

        data.remove(PhantomChestEntity.KEY_ENTITY_ID);
        PhantomChestSummonerItem.deactivateInInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.getServer() == null) return;
        dismissAllChests(sp.getServer(), sp);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.getServer() == null) return;
        dismissAllChests(sp.getServer(), sp);
    }

    /**
     * Saves and discards every PhantomChestEntity owned by this player across all loaded levels.
     * The tracked chest (registered in player data) is saved authoritatively. Orphaned chests
     * from older mod versions that are not registered in player data are also discarded; their
     * state is only saved if no tracked chest exists (migration path).
     */
    private static void dismissAllChests(MinecraftServer server, ServerPlayer player) {
        PhantomChestEntity tracked = PhantomChestSummonerItem.findPlayerChest(server, player);

        for (ServerLevel level : server.getAllLevels()) {
            for (PhantomChestEntity chest : level.getEntitiesOfClass(
                    PhantomChestEntity.class, WORLD_BOUNDS,
                    c -> player.getUUID().equals(c.getOwnerUUID()))) {
                if (chest == tracked || tracked == null) {
                    chest.saveInventoryTo(player);
                    chest.saveFilterTo(player);
                    chest.saveLinksTo(player);
                }
                chest.discard();
            }
        }

        player.getPersistentData().remove(PhantomChestEntity.KEY_ENTITY_ID);
        PhantomChestSummonerItem.deactivateInInventory(player);
    }
}
