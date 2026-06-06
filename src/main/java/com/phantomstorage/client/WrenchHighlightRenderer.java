package com.phantomstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantomstorage.DesignationMode;
import com.phantomstorage.ModItems;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.network.LinkedStorageSyncPayload;
import com.phantomstorage.network.WrenchHighlightData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WrenchHighlightRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // Only render for the player holding the wrench
        if (!player.getMainHandItem().is(ModItems.PHANTOM_WRENCH.get())
                && !player.getOffhandItem().is(ModItems.PHANTOM_WRENCH.get())) return;

        List<LinkedStorageSyncPayload.HighlightEntry> entries = WrenchHighlightData.get();
        if (entries.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        Vec3 playerPos = player.position();
        for (LinkedStorageSyncPayload.HighlightEntry entry : entries) {
            if (Vec3.atCenterOf(entry.pos()).distanceToSqr(playerPos) > 32.0 * 32.0) continue;
            AABB box = new AABB(entry.pos()).inflate(0.003);
            if (entry.mode() == DesignationMode.INPUT) {
                // Blue
                LevelRenderer.renderLineBox(poseStack, lines, box, 0.2f, 0.5f, 1.0f, 0.5f);
            } else {
                // Orange
                LevelRenderer.renderLineBox(poseStack, lines, box, 1.0f, 0.55f, 0.1f, 0.5f);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
