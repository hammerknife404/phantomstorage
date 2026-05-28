package com.phantomstorage.events;

import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.item.PhantomChestSummonerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

@EventBusSubscriber(modid = PhantomStorageMod.MODID)
public class DimensionEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy phantom chest inventory and entity ID from original player to respawned player.
        // The entity ID must be carried over so the respawned player can still find their chest
        // via the O(dimensions) UUID lookup rather than a world-scale AABB scan.
        CompoundTag original = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        if (original.contains("PhantomChestInventory")) {
            newData.put("PhantomChestInventory",
                    original.getList("PhantomChestInventory", 10).copy());
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

        // O(1) lookup — no world-scale AABB scan needed.
        UUID entityId = data.getUUID(PhantomChestEntity.KEY_ENTITY_ID);
        ServerLevel oldLevel = event.getEntity().getServer().getLevel(event.getFrom());
        if (oldLevel != null) {
            Entity e = oldLevel.getEntity(entityId);
            if (e instanceof PhantomChestEntity chest) {
                chest.saveInventoryTo(event.getEntity());
                chest.discard();
            }
        }

        // Remove the stale ID — the player must re-summon in the new dimension.
        data.remove(PhantomChestEntity.KEY_ENTITY_ID);
        // Keep the tooltip in sync: chest is gone, so clear the "active" flag.
        PhantomChestSummonerItem.deactivateInInventory(event.getEntity());
    }
}
