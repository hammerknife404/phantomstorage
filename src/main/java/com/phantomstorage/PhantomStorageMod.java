package com.phantomstorage;

import com.mojang.logging.LogUtils;
import com.phantomstorage.block.PhantomLinkBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(PhantomStorageMod.MODID)
public class PhantomStorageMod {
    public static final String MODID = "phantomstorage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PhantomStorageMod(IEventBus modEventBus) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(PhantomLinkBlockEntity::onServerStopping);
    }
}
