package dev.breezes.settlements.entities.villager.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.animations.animator.ConditionAnimator;
import dev.breezes.settlements.entities.villager.animations.animator.OneShotAnimator;
import dev.breezes.settlements.entities.villager.animations.definitions.BaseVillagerAnimation;
import dev.breezes.settlements.util.ResourceLocationUtil;
import lombok.CustomLog;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

@CustomLog
public class BaseVillagerModel<T extends BaseVillager> extends AbstractVillagerModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocationUtil.mod("base_villager"), "main");

    private final ModelPart root;
    private final ModelPart villager;
    private final ModelPart head;

    public BaseVillagerModel(ModelPart root) {
        this.root = root.getChild("root");
        this.villager = this.root.getChild("villager");
        this.head = this.villager.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition villager = root.addOrReplaceChild("villager", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition head = villager.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));
        PartDefinition monobrow = head.addOrReplaceChild("monobrow", CubeListBuilder.create().texOffs(33, 2).addBox(-3.0F, -0.5F, -0.1875F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, -4.5F, -4.0F));
        PartDefinition eye_left = head.addOrReplaceChild("eye_left", CubeListBuilder.create(), PartPose.offset(2.0F, -3.5F, -4.0F));
        PartDefinition eyelid_left = eye_left.addOrReplaceChild("eyelid_left", CubeListBuilder.create().texOffs(43, 8).addBox(-1.0F, -0.5F, -0.0625F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition eyeball_left = eye_left.addOrReplaceChild("eyeball_left", CubeListBuilder.create().texOffs(33, 8).addBox(-1.0F, -0.5F, -0.0125F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pupil_left = eyeball_left.addOrReplaceChild("pupil_left", CubeListBuilder.create().texOffs(39, 8).addBox(-0.5F, -0.5F, -0.0375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 0.0F, 0.0F));
        PartDefinition eye_right = head.addOrReplaceChild("eye_right", CubeListBuilder.create(), PartPose.offset(-2.0F, -3.5F, -4.0F));
        PartDefinition eyelid_right = eye_right.addOrReplaceChild("eyelid_right", CubeListBuilder.create().texOffs(43, 8).addBox(-1.0F, -0.5F, -0.0625F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition eyeball_right = eye_right.addOrReplaceChild("eyeball_right", CubeListBuilder.create().texOffs(33, 8).addBox(-1.0F, -0.5F, -0.0125F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pupil_right = eyeball_right.addOrReplaceChild("pupil_right", CubeListBuilder.create().texOffs(39, 8).addBox(-0.5F, -0.5F, -0.0375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.0F, 0.0F));
        PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -4.0F));
        PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(33, 5).addBox(-2.0F, -0.5F, -0.125F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.5F, -4.0F));
        PartDefinition torso = villager.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, -24.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arms = torso.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -22.0F, 0.0F, -0.7854F, 0.0F, 0.0F));
        PartDefinition right_arm = arms.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 22).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 0.0F, 0.0F));
        PartDefinition right_hand = right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)), PartPose.offset(-3.0F, 6.0F, 0.0F));
        PartDefinition left_arm = arms.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(6.0F, 0.0F, 0.0F));
        PartDefinition left_hand = left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(0, 48).addBox(-8.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)), PartPose.offset(2.0F, 6.0F, 0.0F));
        PartDefinition legs = villager.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(-2.0F, -12.0F, 0.0F));
        PartDefinition left_leg = legs.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(4.0F, 0.0F, 0.0F));
        PartDefinition right_leg = legs.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * Executed every tick to update the villager's animations
     */
    @Override
    public void setupAnim(T villager, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Apply head rotations
        this.head.yRot = Mth.clamp(netHeadYaw, -30F, 30F) * Mth.DEG_TO_RAD;
        this.head.xRot = Mth.clamp(headPitch, -25F, 45F) * Mth.DEG_TO_RAD;

        /*
         * Animations
         */
        // Walk & run animations
        // TODO: differentiate between walk and run
        this.animateWalk(BaseVillagerAnimation.WALK_1.definition(), limbSwing, limbSwingAmount, 1, 2);

        // Nitwit animation
        ConditionAnimator nitwitAnimator = villager.getWiggleAnimator();
        if (nitwitAnimator.isAnimationPlaying()) {
            this.animate(nitwitAnimator.getCurrentState().get(), nitwitAnimator.getCurrentDefinition().get(), ageInTicks, 1);
        }

        // Repair iron golem animation; TODO: make an one-shot animator
        OneShotAnimator spinAnimator = villager.getSpinAnimator();
        if (spinAnimator.isAnimationPlaying()) {
            this.animate(spinAnimator.getCurrentState().get(), spinAnimator.getCurrentDefinition().get(), ageInTicks, 1);
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack stack) {
        // TODO: implement
    }

}
