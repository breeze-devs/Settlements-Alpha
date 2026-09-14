package dev.breezes.settlements.infrastructure.minecraft.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.domain.ballista.BallistaBoltGeometry;
import dev.breezes.settlements.domain.ballista.BallistaGeometry;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * The ballista bolt's rig.
 * One model serves the bolt seated in a ballista and the bolt in flight.
 */
@ClientSide
public class BallistaBoltModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocationUtil.mod("ballista_bolt"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocationUtil.mod("textures/entity/ballista_bolt.png");

    private static final float PLANE_ROLL_RADIANS = 0.7854F;

    private final ModelPart bolt;

    public BallistaBoltModel(@Nonnull ModelPart root) {
        this.bolt = root.getChild("bolt");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The root bone's offset is where the export stood the pivot, which the draw subtracts again
        PartDefinition bolt = root.addOrReplaceChild("bolt", CubeListBuilder.create(),
                PartPose.offset(0.0F, 20.5F, 11.5F));

        // Each plane is centered on its own bone, half a length ahead of the tail end, which sits the pivot's inset
        // behind the pivot; the length comes from BallistaBoltGeometry, since the launch point measures the tip by it
        float halfLength = BallistaBoltGeometry.LENGTH_PIXELS / 2.0F;
        float planeCenterZ = BallistaBoltGeometry.PIVOT_INSET_PIXELS - halfLength;

        bolt.addOrReplaceChild("bolt_negative_r1", CubeListBuilder.create()
                        .texOffs(0, 29).addBox(0.0F, -2.5F, -halfLength, 0.0F, 5.0F,
                                BallistaBoltGeometry.LENGTH_PIXELS, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, planeCenterZ, 0.0F, 0.0F, -PLANE_ROLL_RADIANS));

        bolt.addOrReplaceChild("bolt_positive_r1", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, -2.5F, -halfLength, 0.0F, 5.0F,
                                BallistaBoltGeometry.LENGTH_PIXELS, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, planeCenterZ, 0.0F, 0.0F, PLANE_ROLL_RADIANS));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Draws the bolt at its drawn scale, with its pivot at the pose's origin and its tip along the pose's -z.
     */
    public void render(@Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        poseStack.pushPose();
        // Scaled ahead of the offset below, so the bolt grows about its pivot rather than about the draw origin
        poseStack.scale(BallistaBoltGeometry.DRAWN_SCALE, BallistaBoltGeometry.DRAWN_SCALE,
                BallistaBoltGeometry.DRAWN_SCALE);
        // A group's origin exports as its bone's offset, so the pivot stands off the draw origin by exactly that much
        poseStack.translate(-this.bolt.x / BallistaGeometry.PIXELS_PER_BLOCK,
                -this.bolt.y / BallistaGeometry.PIXELS_PER_BLOCK,
                -this.bolt.z / BallistaGeometry.PIXELS_PER_BLOCK);

        // Culling off, so each crossed plane reads correctly from either side
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.bolt.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

}
