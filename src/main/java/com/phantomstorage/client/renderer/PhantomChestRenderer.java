package com.phantomstorage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.phantomstorage.client.model.PhantomChestModel;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class PhantomChestRenderer extends MobRenderer<PhantomChestEntity, PhantomChestModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("phantomstorage", "textures/entity/phantom_chest.png");

    // Semi-transparent: 80% opacity encoded as ARGB int (0xCC = 204/255)
    private static final int GHOST_COLOR = 0xCCFFFFFF;

    public PhantomChestRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PhantomChestModel(ctx.bakeLayer(PhantomChestModel.LAYER_LOCATION)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomChestEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PhantomChestEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        float age = entity.tickCount + partialTick;
        float bob = (float) Math.sin(age * 0.05f) * 0.1f;

        // Model vertices are pre-divided by 16 (block units). Body bottom = 10/16 = 0.625 blocks.
        // Translate by that amount so the body bottom sits exactly at the entity's feet.
        poseStack.translate(0.0, bob + 0.625, 0.0);

        // Scale 1.0 renders at the same visual size as a vanilla chest block placed in the world
        // (14/16 = 0.875 blocks wide). Negate X and Y for standard entity orientation.
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, GHOST_COLOR);

        poseStack.popPose();
    }
}
