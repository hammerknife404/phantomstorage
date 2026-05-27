package com.phantomstorage.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantomstorage.PhantomStorageMod;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple box model: 14×14×14 body, 14×5×14 lid.
 * Texture layout mirrors vanilla chest (64×64).
 */
public class PhantomChestModel extends EntityModel<PhantomChestEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(PhantomStorageMod.MODID, "phantom_chest"), "main");

    private final ModelPart bottom;
    private final ModelPart lid; // lock is a child and renders automatically

    public PhantomChestModel(ModelPart root) {
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Body: 14×10×14 — matches vanilla single-chest UV layout at texOffs(0,19)
        parts.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(0, 19).addBox(0f, 0f, 0f, 14, 10, 14),
                PartPose.offset(-7f, 0f, -7f));

        // Lid: pivot at the back hinge edge so xRot opens the lid correctly.
        // Box extends forward (-Z) from the hinge; at rest it occupies the same
        // world volume as before: X(-7..7), Y(-5..0), Z(-7..7).
        PartDefinition lidDef = parts.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, -5f, -14f, 14, 5, 14),
                PartPose.offset(-7f, 0f, 7f));

        // Lock is a child of the lid so it pivots with it.
        // Position relative to the new lid pivot (-7, 0, 7): same world position as before.
        lidDef.addOrReplaceChild("lock",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, -2f, 0f, 2, 4, 1),
                PartPose.offset(6f, -3f, -14.5f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PhantomChestEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        bottom.render(poseStack, buffer, packedLight, packedOverlay, color);
        lid.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
