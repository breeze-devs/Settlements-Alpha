package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationTargets;
import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.domain.presentation.ArmPose;
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
public class SettlementsVillagerModel<T extends Entity> extends HierarchicalModel<T> implements HeadedModel, VillagerHeadModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocationUtil.mod("base_villager"), "main");

    private static final float UNHAPPY_HEAD_ROLL_AMPLITUDE = 0.3F;
    private static final float UNHAPPY_HEAD_ROLL_FREQUENCY = 0.45F;
    private static final float UNHAPPY_HEAD_PITCH_RAD = 0.4F;
    private static final float LEG_SWING_FREQUENCY = 0.6662F;
    private static final float LEG_SWING_AMPLITUDE = 1.4F;
    private static final float LEG_SWING_SCALE = 0.5F;

    private final ModelPart root;
    private final ModelPart villager;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart monobrow;
    private final ModelPart eyes;
    private final ModelPart eye_left;
    private final ModelPart eyelid_left;
    private final ModelPart eyeball_left;
    private final ModelPart pupil_left;
    private final ModelPart eye_right;
    private final ModelPart eyelid_right;
    private final ModelPart eyeball_right;
    private final ModelPart pupil_right;
    private final ModelPart nose;
    private final ModelPart mouth;
    private final ModelPart headwear;
    private final ModelPart hat;
    private final ModelPart arms;
    private final ModelPart arms_crossed;
    private final ModelPart arms_crossed_socket;
    private final ModelPart arm_crossed_center_whole;
    private final ModelPart arm_crossed_left;
    private final ModelPart arm_crossed_center_left;
    private final ModelPart arm_crossed_right;
    private final ModelPart arm_crossed_center_right;
    private final ModelPart arms_straight;
    private final ModelPart arm_straight_left;
    private final ModelPart arm_straight_left_socket;
    private final ModelPart arm_straight_right;
    private final ModelPart arm_straight_right_socket;
    private final ModelPart bodywear;
    private final ModelPart legs;
    private final ModelPart leg_left;
    private final ModelPart leg_right;
    private final ModelPart feet_center_socket;
    @Nullable
    private AnimationFrame animationFrame;
    // Snapped once per render call from the animator so per-arm visibility and socket selection stay in sync.
    private ArmConfiguration armConfiguration = ArmConfiguration.BOTH_CROSSED;

    public SettlementsVillagerModel(ModelPart root) {
        this.root = root.getChild("root");
        this.villager = this.root.getChild("villager");
        this.torso = this.villager.getChild("torso");
        this.head = this.torso.getChild("head");
        this.monobrow = this.head.getChild("monobrow");
        this.eyes = this.head.getChild("eyes");
        this.eye_left = this.eyes.getChild("eye_left");
        this.eyelid_left = this.eye_left.getChild("eyelid_left");
        this.eyeball_left = this.eye_left.getChild("eyeball_left");
        this.pupil_left = this.eyeball_left.getChild("pupil_left");
        this.eye_right = this.eyes.getChild("eye_right");
        this.eyelid_right = this.eye_right.getChild("eyelid_right");
        this.eyeball_right = this.eye_right.getChild("eyeball_right");
        this.pupil_right = this.eyeball_right.getChild("pupil_right");
        this.nose = this.head.getChild("nose");
        this.mouth = this.head.getChild("mouth");
        this.headwear = this.head.getChild("headwear");
        this.hat = this.head.getChild("hat");
        this.arms = this.torso.getChild("arms");
        this.arms_crossed = this.arms.getChild("arms_crossed");
        this.arms_crossed_socket = this.arms_crossed.getChild("arms_crossed_socket");
        this.arm_crossed_center_whole = this.arms_crossed.getChild("arm_crossed_center_whole");
        this.arm_crossed_left = this.arms_crossed.getChild("arm_crossed_left");
        this.arm_crossed_center_left = this.arms_crossed.getChild("arm_crossed_center_left");
        this.arm_crossed_right = this.arms_crossed.getChild("arm_crossed_right");
        this.arm_crossed_center_right = this.arms_crossed.getChild("arm_crossed_center_right");
        this.arms_straight = this.arms.getChild("arms_straight");
        this.arm_straight_left = this.arms_straight.getChild("arm_straight_left");
        this.arm_straight_left_socket = this.arm_straight_left.getChild("arm_straight_left_socket");
        this.arm_straight_right = this.arms_straight.getChild("arm_straight_right");
        this.arm_straight_right_socket = this.arm_straight_right.getChild("arm_straight_right_socket");
        this.bodywear = this.torso.getChild("bodywear");
        this.legs = this.villager.getChild("legs");
        this.leg_left = this.legs.getChild("leg_left");
        this.leg_right = this.legs.getChild("leg_right");
        this.feet_center_socket = this.legs.getChild("feet_center_socket");

        // Both parent groups start visible; the per-arm children are toggled every frame in applyAnimationFrame
        // based on armConfiguration, so the initial state here does not affect runtime rendering.
        this.arms_crossed.visible = true;
        this.arms_straight.visible = true;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition villager = root.addOrReplaceChild("villager", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition torso = villager.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition monobrow = head.addOrReplaceChild("monobrow", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, -4.5F, -3.6875F));

        PartDefinition eyes = head.addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.offset(2.0F, -3.5F, -4.0F));

        PartDefinition eye_left = eyes.addOrReplaceChild("eye_left", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition eyelid_left = eye_left.addOrReplaceChild("eyelid_left", CubeListBuilder.create().texOffs(24, 4).addBox(-1.0F, -0.5F, -0.0625F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition eyeball_left = eye_left.addOrReplaceChild("eyeball_left", CubeListBuilder.create().texOffs(24, 6).addBox(-1.0F, -0.5F, -0.0125F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition pupil_left = eyeball_left.addOrReplaceChild("pupil_left", CubeListBuilder.create().texOffs(30, 4).addBox(-0.5F, -0.5F, -0.0375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 0.0F, 0.0F));

        PartDefinition eye_right = eyes.addOrReplaceChild("eye_right", CubeListBuilder.create(), PartPose.offset(-4.0F, 0.0F, 0.0F));

        PartDefinition eyelid_right = eye_right.addOrReplaceChild("eyelid_right", CubeListBuilder.create().texOffs(24, 4).addBox(-1.0F, -0.5F, -0.0625F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition eyeball_right = eye_right.addOrReplaceChild("eyeball_right", CubeListBuilder.create().texOffs(24, 6).addBox(-1.0F, -0.5F, -0.0125F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition pupil_right = eyeball_right.addOrReplaceChild("pupil_right", CubeListBuilder.create().texOffs(30, 4).addBox(-0.5F, -0.5F, -0.0375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.0F, 0.0F));

        PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -4.0F));

        PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(24, 2).addBox(-2.0F, -0.5F, -0.125F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.5F, -4.0F));

        PartDefinition headwear = head.addOrReplaceChild("headwear", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition arms = torso.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, -9.525F, 0.0F));

        PartDefinition arms_crossed = arms.addOrReplaceChild("arms_crossed", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.475F, -1.05F, -0.7854F, 0.0F, 0.0F));

        PartDefinition arms_crossed_socket = arms_crossed.addOrReplaceChild("arms_crossed_socket", CubeListBuilder.create(), PartPose.offset(0.0F, 3.7123F, -2.6517F));

        PartDefinition arm_crossed_center_whole = arms_crossed.addOrReplaceChild("arm_crossed_center_whole", CubeListBuilder.create().texOffs(40, 38).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.0F, 0.0F));

        PartDefinition arm_crossed_left = arms_crossed.addOrReplaceChild("arm_crossed_left", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-2.0F, -4.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(6.0F, 2.0F, 0.0F));

        PartDefinition arm_crossed_center_left = arms_crossed.addOrReplaceChild("arm_crossed_center_left", CubeListBuilder.create().texOffs(7, 10).addBox(4.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(46, 38).addBox(6.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 4.0F, 0.0F));

        PartDefinition arm_crossed_right = arms_crossed.addOrReplaceChild("arm_crossed_right", CubeListBuilder.create().texOffs(44, 22).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 2.0F, 0.0F));

        PartDefinition arm_crossed_center_right = arms_crossed.addOrReplaceChild("arm_crossed_center_right", CubeListBuilder.create().texOffs(9, 10).addBox(-2.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(46, 38).addBox(-4.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.0F, 0.0F));

        PartDefinition arms_straight = arms.addOrReplaceChild("arms_straight", CubeListBuilder.create(), PartPose.offset(0.0F, -0.475F, 0.0F));

        PartDefinition arm_straight_left = arms_straight.addOrReplaceChild("arm_straight_left", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-1.0F, -2.05F, -2.05F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(44, 38).mirror().addBox(-1.0F, 5.95F, -2.05F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(4, 12).mirror().addBox(-1.0F, 7.95F, -2.05F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(5.0F, 0.0F, 0.0F));

        PartDefinition arm_straight_left_socket = arm_straight_left.addOrReplaceChild("arm_straight_left_socket", CubeListBuilder.create(), PartPose.offset(1.0F, 8.0F, 0.0F));

        PartDefinition arm_straight_right = arms_straight.addOrReplaceChild("arm_straight_right", CubeListBuilder.create().texOffs(44, 22).addBox(-3.0F, -2.05F, -2.05F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(44, 38).addBox(-3.0F, 5.95F, -2.05F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(4, 12).addBox(-3.0F, 7.95F, -2.05F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 0.0F, 0.0F));

        PartDefinition arm_straight_right_socket = arm_straight_right.addOrReplaceChild("arm_straight_right_socket", CubeListBuilder.create(), PartPose.offset(-1.0F, 8.0F, 0.0F));

        PartDefinition bodywear = torso.addOrReplaceChild("bodywear", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition legs = villager.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(2.0F, -12.0F, 0.0F));

        PartDefinition leg_left = legs.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition leg_right = legs.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 0.0F, 0.0F));

        PartDefinition feet_center_socket = legs.addOrReplaceChild("feet_center_socket", CubeListBuilder.create(), PartPose.offset(-2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        boolean isUnhappy = false;
        if (entity instanceof AbstractVillager abstractVillager) {
            isUnhappy = abstractVillager.getUnhappyCounter() > 0;
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        if (isUnhappy) {
            this.head.zRot = UNHAPPY_HEAD_ROLL_AMPLITUDE * Mth.sin(UNHAPPY_HEAD_ROLL_FREQUENCY * ageInTicks);
            this.head.xRot = UNHAPPY_HEAD_PITCH_RAD;
        } else {
            this.head.zRot = 0.0F;
        }

        // Leg swing uses new-rig part names but the same vanilla cosine formula.
        this.leg_right.xRot = Mth.cos(limbSwing * LEG_SWING_FREQUENCY) * LEG_SWING_AMPLITUDE * limbSwingAmount * LEG_SWING_SCALE;
        this.leg_left.xRot = Mth.cos(limbSwing * LEG_SWING_FREQUENCY + (float) Math.PI) * LEG_SWING_AMPLITUDE * limbSwingAmount * LEG_SWING_SCALE;
        this.leg_right.yRot = 0.0F;
        this.leg_left.yRot = 0.0F;

        this.applyAnimationFrame();
    }

    /**
     * Applies the cumulative transform of every ancestor down to the target bone.
     * Must start from this.root because the renderer's layer pass enters model space
     * after root()'s own push/pop has already closed — nothing from renderToBuffer
     * persists into layer rendering, so reaching any socket requires re-walking
     * the full chain from the top of the hierarchy.
     */
    public void applyBoneTransform(PoseStack pose, ModelPartRef ref) {
        switch (ref) {
            case ROOT -> chain(pose, this.root);
            case BODY -> chain(pose, this.root, this.villager, this.torso);
            case HEAD -> chain(pose, this.root, this.villager, this.torso, this.head);
            case ARMS -> chain(pose, this.root, this.villager, this.torso, this.arms, this.arms_crossed);
            case ARMS_CROSSED_SOCKET ->
                    chain(pose, this.root, this.villager, this.torso, this.arms, this.arms_crossed, this.arms_crossed_socket);
            case ARM_STRAIGHT_RIGHT_SOCKET ->
                    chain(pose, this.root, this.villager, this.torso, this.arms, this.arms_straight, this.arm_straight_right, this.arm_straight_right_socket);
            case ARM_STRAIGHT_LEFT_SOCKET ->
                    chain(pose, this.root, this.villager, this.torso, this.arms, this.arms_straight, this.arm_straight_left, this.arm_straight_left_socket);
            // Legs branch off villager directly (not through torso), so the chain skips torso entirely.
            case FEET_CENTER_SOCKET -> chain(pose, this.root, this.villager, this.legs, this.feet_center_socket);
        }
    }

    private static void chain(PoseStack pose, ModelPart... parts) {
        for (ModelPart part : parts) {
            part.translateAndRotate(pose);
        }
    }

    public void setAnimationFrame(@Nullable AnimationFrame animationFrame) {
        this.animationFrame = animationFrame;
    }

    public void setArmConfiguration(ArmConfiguration armConfiguration) {
        this.armConfiguration = armConfiguration;
    }

    @Override
    public void hatVisible(boolean visible) {
        // The profession layer drives this; all three head-dress parts must toggle together
        // so neither the base hat mesh nor the overlay ever appears without the other.
        this.head.visible = visible;
        this.headwear.visible = visible;
        this.hat.visible = visible;
    }

    private void applyAnimationFrame() {
        AnimationFrame frame = this.animationFrame == null ? AnimationFrame.EMPTY : this.animationFrame;

        // Per-arm child visibility is resolved from the config each frame so the three consumers
        // (geometry visibility, animation targets, held-item socket) can never desync.
        boolean leftCrossed = this.armConfiguration.left() == ArmPose.CROSSED;
        boolean rightCrossed = this.armConfiguration.right() == ArmPose.CROSSED;
        this.arm_crossed_left.visible = leftCrossed;
        this.arm_straight_left.visible = !leftCrossed;
        this.arm_crossed_right.visible = rightCrossed;
        this.arm_straight_right.visible = !rightCrossed;
        // The whole center bar is shown only when both arms are crossed; each half-cap is shown
        // when exactly one arm is crossed so the bar terminates cleanly without the opposing arm.
        this.arm_crossed_center_whole.visible = leftCrossed && rightCrossed;
        this.arm_crossed_center_left.visible = leftCrossed && !rightCrossed;
        this.arm_crossed_center_right.visible = rightCrossed && !leftCrossed;

        // All 12 arm targets are applied unconditionally. For a hidden arm the additive of
        // neutral is a no-op, so authors only need to author the targets that match their
        // declared arm config — unused targets quietly do nothing.
        applyRotation(this.arms_crossed, frame.get(AnimationTargets.ARMS_CROSSED_ROTATION));
        applyTranslation(this.arms_crossed, frame.get(AnimationTargets.ARMS_CROSSED_TRANSLATION));
        applyRotation(this.arms_straight, frame.get(AnimationTargets.ARMS_STRAIGHT_ROTATION));
        applyTranslation(this.arms_straight, frame.get(AnimationTargets.ARMS_STRAIGHT_TRANSLATION));
        applyRotation(this.arm_crossed_left, frame.get(AnimationTargets.ARM_CROSSED_LEFT_ROTATION));
        applyTranslation(this.arm_crossed_left, frame.get(AnimationTargets.ARM_CROSSED_LEFT_TRANSLATION));
        applyRotation(this.arm_crossed_right, frame.get(AnimationTargets.ARM_CROSSED_RIGHT_ROTATION));
        applyTranslation(this.arm_crossed_right, frame.get(AnimationTargets.ARM_CROSSED_RIGHT_TRANSLATION));
        applyRotation(this.arm_straight_left, frame.get(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION));
        applyTranslation(this.arm_straight_left, frame.get(AnimationTargets.ARM_STRAIGHT_LEFT_TRANSLATION));
        applyRotation(this.arm_straight_right, frame.get(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION));
        applyTranslation(this.arm_straight_right, frame.get(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION));

        applyRotation(this.torso, frame.get(AnimationTargets.BODY_ROTATION));

        // Absolute head override: only applied when the frame explicitly carries this target,
        // so normal yaw/pitch tracking from setupAnim is not clobbered during idle.
        if (frame.has(AnimationTargets.HEAD_ROTATION_OVERRIDE)) {
            applyAbsoluteRotation(this.head, frame.get(AnimationTargets.HEAD_ROTATION_OVERRIDE));
        }

        // Root motion is additive on top of resetPose's baseline, so it never accumulates drift
        // across frames — resetPose runs at the top of setupAnim each tick.
        applyTranslation(this.root, frame.get(AnimationTargets.ROOT_TRANSLATION));
        applyRotation(this.root, frame.get(AnimationTargets.ROOT_ROTATION));

        // Leg overrides are gated by frame.has so the vanilla walk-swing from setupAnim is left
        // untouched during idle; the override only takes effect when a track explicitly authors it
        // (e.g. moonwalk needs full control of leg rotation).
        if (frame.has(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)) {
            applyAbsoluteRotation(this.leg_left, frame.get(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE));
        }
        if (frame.has(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)) {
            applyAbsoluteRotation(this.leg_right, frame.get(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE));
        }

        // Face expression targets — PROVISIONAL: axes and ids need validation against the first
        // authored face animation before they are considered stable.
        applyTranslation(this.monobrow, frame.get(AnimationTargets.MONOBROW_TRANSLATION));
        applyRotation(this.monobrow, frame.get(AnimationTargets.MONOBROW_ROTATION));
        applyTranslation(this.mouth, frame.get(AnimationTargets.MOUTH_TRANSLATION));
        applyTranslation(this.eyelid_left, frame.get(AnimationTargets.EYELID_LEFT_TRANSLATION));
        applyTranslation(this.eyelid_right, frame.get(AnimationTargets.EYELID_RIGHT_TRANSLATION));
        applyTranslation(this.pupil_left, frame.get(AnimationTargets.PUPIL_LEFT_TRANSLATION));
        applyTranslation(this.pupil_right, frame.get(AnimationTargets.PUPIL_RIGHT_TRANSLATION));
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

    private static void applyAbsoluteRotation(ModelPart part, Vector3f rotation) {
        part.xRot = rotation.x();
        part.yRot = rotation.y();
        part.zRot = rotation.z();
    }

}
