package com.phantomstorage.events;

import com.phantomstorage.ModEntities;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.entity.PhantomChestEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ServerEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PHANTOM_CHEST.get(), PhantomChestEntity.createAttributes().build());
    }
}
