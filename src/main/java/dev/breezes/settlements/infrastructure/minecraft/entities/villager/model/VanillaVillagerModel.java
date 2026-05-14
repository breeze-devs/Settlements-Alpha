package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model;

import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationTargets;
import dev.breezes.settlements.domain.presentation.ModelPartRef;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.VillagerHeadModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@CustomLog
@OnlyIn(Dist.CLIENT)
@Getter
public class VanillaVillagerModel<T extends Entity> extends HierarchicalModel<T> implements HeadedModel, VillagerHeadModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocationUtil.mod("base_villager"), "main");

    private static final float HAT_INFLATION = 0.51F;
    private static final float JACKET_INFLATION = 0.5F;
    private static final float ARMS_PITCH_RAD = -0.75F;
    private static final float UNHAPPY_HEAD_ROLL_AMPLITUDE = 0.3F;
    private static final float UNHAPPY_HEAD_ROLL_FREQUENCY = 0.45F;
    private static final float UNHAPPY_HEAD_PITCH_RAD = 0.4F;
    private static final float LEG_SWING_FREQUENCY = 0.6662F;
    private static final float LEG_SWING_AMPLITUDE = 1.4F;
    private static final float LEG_SWING_SCALE = 0.5F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart arms;
    private final ModelPart hat;
    private final ModelPart hatRim;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    protected final ModelPart nose;
    @Nullable
    private AnimationFrame animationFrame;

    public VanillaVillagerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.arms = root.getChild("arms");
        this.hat = this.head.getChild("hat");
        this.hatRim = this.hat.getChild("hat_rim");
        this.nose = this.head.getChild("nose");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(createBodyModel(), 64, 64);
    }

    private static MeshDefinition createBodyModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition modelRoot = mesh.getRoot();

        PartDefinition head = modelRoot.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(HAT_INFLATION)), PartPose.ZERO);
        hat.addOrReplaceChild("hat_rim", CubeListBuilder.create()
                .texOffs(30, 47)
                .addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F), PartPose.rotation((-(float) Math.PI / 2F), 0.0F, 0.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(24, 0)
                .addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition body = modelRoot.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 20)
                .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("jacket", CubeListBuilder.create()
                .texOffs(0, 38)
                .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(JACKET_INFLATION)), PartPose.ZERO);

        modelRoot.addOrReplaceChild("arms", CubeListBuilder.create()
                .texOffs(44, 22)
                .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(44, 22)
                .addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
                .texOffs(40, 38)
                .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, ARMS_PITCH_RAD, 0.0F, 0.0F));
        modelRoot.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 22)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        modelRoot.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 22)
                .mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));

        return mesh;
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        boolean flag = false;
        if (entity instanceof AbstractVillager) {
            flag = ((AbstractVillager) entity).getUnhappyCounter() > 0;
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        if (flag) {
            this.head.zRot = UNHAPPY_HEAD_ROLL_AMPLITUDE * Mth.sin(UNHAPPY_HEAD_ROLL_FREQUENCY * ageInTicks);
            this.head.xRot = UNHAPPY_HEAD_PITCH_RAD;
        } else {
            this.head.zRot = 0.0F;
        }

        this.rightLeg.xRot = Mth.cos(limbSwing * LEG_SWING_FREQUENCY) * LEG_SWING_AMPLITUDE * limbSwingAmount * LEG_SWING_SCALE;
        this.leftLeg.xRot = Mth.cos(limbSwing * LEG_SWING_FREQUENCY + (float) Math.PI) * LEG_SWING_AMPLITUDE * limbSwingAmount * LEG_SWING_SCALE;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        this.applyAnimationFrame();
    }

    public ModelPart getPart(ModelPartRef partRef) {
        return switch (partRef) {
            case ROOT -> this.root;
            case BODY -> this.body;
            case HEAD -> this.head;
            case ARMS -> this.arms;
        };
    }

    public void setAnimationFrame(@Nullable AnimationFrame animationFrame) {
        this.animationFrame = animationFrame;
    }

    public void hatVisible(boolean visible) {
        this.head.visible = visible;
        this.hat.visible = visible;
        this.hatRim.visible = visible;
    }

    private void applyAnimationFrame() {
        AnimationFrame frame = this.animationFrame == null ? AnimationFrame.EMPTY : this.animationFrame;

        applyRotation(this.arms, frame.get(AnimationTargets.ARMS_ROTATION));
        applyTranslation(this.arms, frame.get(AnimationTargets.ARMS_TRANSLATION));
        applyRotation(this.body, frame.get(AnimationTargets.BODY_ROTATION));

        Vector3f headOverride = frame.get(AnimationTargets.HEAD_ROTATION_OVERRIDE);
        if (frame.has(AnimationTargets.HEAD_ROTATION_OVERRIDE)) {
            this.head.xRot = headOverride.x();
            this.head.yRot = headOverride.y();
            this.head.zRot = headOverride.z();
        }
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

}
