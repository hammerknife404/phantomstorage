package com.phantomstorage;

import com.phantomstorage.block.PhantomAnchorBlockEntity;
import com.phantomstorage.block.PhantomLinkBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PhantomStorageMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhantomLinkBlockEntity>> PHANTOM_LINK =
            BLOCK_ENTITY_TYPES.register("phantom_link", () ->
                    BlockEntityType.Builder.of(PhantomLinkBlockEntity::new, ModBlocks.PHANTOM_LINK.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhantomAnchorBlockEntity>> PHANTOM_ANCHOR =
            BLOCK_ENTITY_TYPES.register("phantom_anchor", () ->
                    BlockEntityType.Builder.of(PhantomAnchorBlockEntity::new, ModBlocks.PHANTOM_ANCHOR.get())
                            .build(null));
}
