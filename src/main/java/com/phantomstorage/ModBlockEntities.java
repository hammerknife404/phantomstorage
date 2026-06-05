package com.phantomstorage;

import com.phantomstorage.block.EtherealLinkBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PhantomStorageMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EtherealLinkBlockEntity>> ETHEREAL_LINK =
            BLOCK_ENTITY_TYPES.register("ethereal_link", () ->
                    BlockEntityType.Builder.of(EtherealLinkBlockEntity::new, ModBlocks.ETHEREAL_LINK.get())
                            .build(null));
}
