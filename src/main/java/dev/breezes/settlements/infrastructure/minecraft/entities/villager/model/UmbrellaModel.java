package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.UmbrellaAnimationTargets;
import dev.breezes.settlements.domain.attachment.UmbrellaPattern;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering.AttachmentModel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.Getter;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;


@OnlyIn(Dist.CLIENT)
@Getter
public class UmbrellaModel implements AttachmentModel {

    public static final ResourceLocation ID = ResourceLocationUtil.mod("umbrella");
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ID, "main");

    private static final float MODEL_UNITS_PER_BLOCK = 16.0F;

    private static final ResourceLocation[] TEXTURES = Arrays.stream(UmbrellaPattern.values())
            .map(pattern -> ResourceLocationUtil.mod("textures/entity/umbrella/" + pattern.name().toLowerCase(Locale.ROOT) + ".png"))
            .toArray(ResourceLocation[]::new);

    private final ModelPart root;
    private final ModelPart canopy;
    private final ModelPart north;
    private final ModelPart south;
    private final ModelPart east;
    private final ModelPart west;

    public UmbrellaModel(ModelPart root) {
        this.root = root.getChild("umbrella");
        this.canopy = this.root.getChild("canopy");
        this.north = this.canopy.getChild("north");
        this.south = this.canopy.getChild("south");
        this.east = this.canopy.getChild("east");
        this.west = this.canopy.getChild("west");
    }

    /**
     * @return the texture that paints the given skin onto this model
     */
    public static ResourceLocation textureFor(@Nonnull UmbrellaPattern pattern) {
        return TEXTURES[pattern.ordinal()];
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // The root pivot sits on the shaft just above the crook, where the villager grips it
        PartDefinition umbrella = partdefinition.addOrReplaceChild("umbrella", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition canopy = umbrella.addOrReplaceChild("canopy", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -24.0F, -0.1F, -3.1416F, 0.0F, 3.1416F));

        addPanel(canopy, "north", PartPose.offset(0.0F, -1.0F, -0.4F), PartPose.rotation(-1.5708F, -1.1345F, 1.5708F), -21, 32);
        addPanel(canopy, "east", PartPose.offset(-0.3F, -1.0F, -0.1F), PartPose.rotation(0.0F, 0.0F, 0.4363F), -21, 7);
        addPanel(canopy, "south", PartPose.offset(0.0F, -1.0F, 0.2F), PartPose.rotation(1.5708F, 1.1345F, 1.5708F), 9, 7);
        addPanel(canopy, "west", PartPose.offset(0.3F, -1.0F, -0.1F), PartPose.rotation(3.1416F, 0.0F, 2.7053F), 9, 32);

        PartDefinition handle = umbrella.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -19.0F, -0.5F, 1.0F, 30.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 0).addBox(-0.5F, 10.0F, 0.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(18, 0).addBox(-0.5F, 9.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(4, 0).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * Adds one canopy panel as two nested bones: an outer bone carrying no geometry and a child holding
     * the panel mesh at its fan-out rotation.
     *
     * @param texOffsX box-UV origin of this panel's own region of the texture, horizontally
     * @param texOffsY box-UV origin of this panel's own region of the texture, vertically
     */
    private static void addPanel(PartDefinition canopy,
                                 String name,
                                 PartPose animatedPose,
                                 PartPose panelPose,
                                 int texOffsX,
                                 int texOffsY) {
        PartDefinition panel = canopy.addOrReplaceChild(name, CubeListBuilder.create(), animatedPose);
        panel.addOrReplaceChild(name + "_panel",
                CubeListBuilder.create().texOffs(texOffsX, texOffsY).addBox(0.0F, 0.0F, -12.5F, 15.0F, 0.0F, 25.0F, new CubeDeformation(0.0F)),
                panelPose);
    }

    @Override
    public Vec3 rootPivotOffset() {
        return new Vec3(this.root.x / MODEL_UNITS_PER_BLOCK, this.root.y / MODEL_UNITS_PER_BLOCK, this.root.z / MODEL_UNITS_PER_BLOCK);
    }

    @Override
    public void render(@Nonnull AnimationFrame frame,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource buffer,
                       int packedLight,
                       @Nonnull ResourceLocation texture) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.applyAnimationFrame(frame);

        // entityCutoutNoCull, not a culling cutout: only one face of each canopy panel is painted, so
        // culling would make the canopy vanish when viewed from the unpainted side.
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.root.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    /**
     * Applies the umbrella-bone targets in the given frame onto this model's bones.
     *
     * @param frame the sampled animation frame to apply; a target absent from it leaves its bone untouched
     */
    private void applyAnimationFrame(AnimationFrame frame) {
        applyTranslation(this.canopy, frame.get(UmbrellaAnimationTargets.CANOPY_TRANSLATION));

        applyRotation(this.north, frame.get(UmbrellaAnimationTargets.NORTH_ROTATION));
        applyTranslation(this.north, frame.get(UmbrellaAnimationTargets.NORTH_TRANSLATION));
        applyScale(this.north, frame.get(UmbrellaAnimationTargets.NORTH_SCALE));

        applyRotation(this.south, frame.get(UmbrellaAnimationTargets.SOUTH_ROTATION));
        applyTranslation(this.south, frame.get(UmbrellaAnimationTargets.SOUTH_TRANSLATION));
        applyScale(this.south, frame.get(UmbrellaAnimationTargets.SOUTH_SCALE));

        applyRotation(this.east, frame.get(UmbrellaAnimationTargets.EAST_ROTATION));
        applyTranslation(this.east, frame.get(UmbrellaAnimationTargets.EAST_TRANSLATION));
        applyScale(this.east, frame.get(UmbrellaAnimationTargets.EAST_SCALE));

        applyRotation(this.west, frame.get(UmbrellaAnimationTargets.WEST_ROTATION));
        applyTranslation(this.west, frame.get(UmbrellaAnimationTargets.WEST_TRANSLATION));
        applyScale(this.west, frame.get(UmbrellaAnimationTargets.WEST_SCALE));
    }

    private static void applyRotation(ModelPart part, Vector3f rotation) {
        part.xRot += rotation.x();
        part.yRot += rotation.y();
        part.zRot += rotation.z();
    }

    private static void applyTranslation(ModelPart part, Vec3 translation) {
        part.x += (float) translation.x;
        part.y += (float) translation.y;
        part.z += (float) translation.z;
    }

    // Multiplicative onto resetPose's unit scale, so the unit-neutral (1,1,1) is a no-op
    private static void applyScale(ModelPart part, Vector3f scale) {
        part.xScale *= scale.x();
        part.yScale *= scale.y();
        part.zScale *= scale.z();
    }

}
