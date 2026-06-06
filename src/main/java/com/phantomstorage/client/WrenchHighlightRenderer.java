package com.phantomstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantomstorage.DesignationMode;
import com.phantomstorage.ModItems;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.network.LinkedStorageSyncPayload;
import com.phantomstorage.network.WrenchHighlightData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WrenchHighlightRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!player.getMainHandItem().is(ModItems.PHANTOM_WRENCH.get())
                && !player.getOffhandItem().is(ModItems.PHANTOM_WRENCH.get())) return;

        List<LinkedStorageSyncPayload.HighlightEntry> entries = WrenchHighlightData.get();
        if (entries.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        Vec3 playerPos = player.position();
        for (LinkedStorageSyncPayload.HighlightEntry entry : entries) {
            if (Vec3.atCenterOf(entry.pos()).distanceToSqr(playerPos) > 32.0 * 32.0) continue;

            AABB tight = new AABB(entry.pos()).inflate(0.003);
            AABB range = new AABB(entry.pos()).inflate(PhantomChestEntity.TRANSFER_RANGE);

            if (entry.mode() == DesignationMode.INPUT) {
                LevelRenderer.renderLineBox(poseStack, lines, tight, 0.2f, 0.5f, 1.0f, 0.5f);
                LevelRenderer.renderLineBox(poseStack, lines, range, 0.2f, 0.5f, 1.0f, 0.12f);
            } else {
                LevelRenderer.renderLineBox(poseStack, lines, tight, 1.0f, 0.55f, 0.1f, 0.5f);
                LevelRenderer.renderLineBox(poseStack, lines, range, 1.0f, 0.55f, 0.1f, 0.12f);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!player.getMainHandItem().is(ModItems.PHANTOM_WRENCH.get())
                && !player.getOffhandItem().is(ModItems.PHANTOM_WRENCH.get())) return;

        int count = WrenchHighlightData.get().size();
        Component text = Component.translatable("hud.phantomstorage.wrench_links", count);

        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int x = w / 2 - mc.font.width(text) / 2;
        int y = h - 44;
        gui.drawString(mc.font, text, x, y, 0xFFFFFFFF, true);
    }
}
