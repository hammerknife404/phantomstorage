package com.phantomstorage;

import com.phantomstorage.block.PhantomAnchorBlock;
import com.phantomstorage.block.PhantomLinkBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, PhantomStorageMod.MODID);

    public static final DeferredHolder<Block, PhantomLinkBlock> PHANTOM_LINK =
            BLOCKS.register("phantom_link", () -> new PhantomLinkBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(s -> 4)));

    public static final DeferredHolder<Block, PhantomAnchorBlock> PHANTOM_ANCHOR =
            BLOCKS.register("phantom_anchor", () -> new PhantomAnchorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(4.0f, 8.0f)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(s -> 6)));
}
