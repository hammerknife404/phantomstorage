package com.phantomstorage.client;

import com.phantomstorage.ModEntities;
import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.ModParticles;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.client.model.PhantomChestModel;
import com.phantomstorage.client.particle.PhantomLinkParticleProvider;
import com.phantomstorage.client.renderer.PhantomChestRenderer;
import com.phantomstorage.client.screen.PhantomChestScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

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
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.PHANTOM_LINK_FLOW.get(), PhantomLinkParticleProvider::new);
    }
}
