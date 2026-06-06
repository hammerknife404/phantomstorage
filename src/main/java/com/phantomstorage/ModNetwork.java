package com.phantomstorage;

import com.phantomstorage.network.LinkedStorageSyncPayload;
import com.phantomstorage.network.WrenchHighlightData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            LinkedStorageSyncPayload.TYPE,
            LinkedStorageSyncPayload.CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> WrenchHighlightData.update(payload.entries()))
        );
    }
}
