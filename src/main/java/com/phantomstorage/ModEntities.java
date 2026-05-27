package com.phantomstorage;

import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, PhantomStorageMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomChestEntity>> PHANTOM_CHEST =
            ENTITY_TYPES.register("phantom_chest", () ->
                    EntityType.Builder.<PhantomChestEntity>of(PhantomChestEntity::new, MobCategory.MISC)
                            .sized(0.9f, 0.7f)
                            .clientTrackingRange(10)
                            .build("phantom_chest"));
}
