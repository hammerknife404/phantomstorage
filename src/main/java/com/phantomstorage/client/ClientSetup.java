package com.phantomstorage.client;

import com.phantomstorage.ModBlocks;
import com.phantomstorage.ModEntities;
import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.block.PhantomLinkBlockEntity;
import com.phantomstorage.client.model.PhantomChestModel;
import com.phantomstorage.client.renderer.PhantomChestRenderer;
import com.phantomstorage.client.screen.PhantomChestScreen;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PHANTOM_CHEST.get(), PhantomChestRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PhantomChestModel.LAYER_LOCATION, PhantomChestModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.PHANTOM_CHEST_MENU.get(), PhantomChestScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PhantomLinkBlockEntity link) {
                    DyeColor ch = link.getChannel();
                    if (ch != null) {
                        return ch.getFireworkColor();
                    }
                }
            }
            return 0xFFFFFF;
        }, ModBlocks.PHANTOM_LINK.get());
    }
}
