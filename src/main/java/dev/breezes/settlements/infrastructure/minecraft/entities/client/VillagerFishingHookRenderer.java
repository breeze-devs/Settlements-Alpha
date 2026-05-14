package dev.breezes.settlements.infrastructure.minecraft.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.VillagerFishingHook;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Renders the VillagerFishingHook -- bobber sprite and fishing line to the villager's hand.
 * <p>
 * Adapted from vanilla FishingHookRenderer, replacing the player hand position with villager's
 */
@OnlyIn(Dist.CLIENT)
public class VillagerFishingHookRenderer extends EntityRenderer<VillagerFishingHook> {

    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);

    private static final int LINE_SEGMENTS = 16;

    // Empirical offsets to approximate the rod-tip world position from the villager root.
    // TODO: replace with SocketRegistry lookup once non-villager renderers can sample sockets,
    // so the line origin tracks the rig instead of drifting when the model changes.
    private static final double ROD_TIP_FORWARD_OFFSET = 0.6D;
    private static final double ROD_TIP_UPWARD_OFFSET = 1.6D;

    // Threshold past which the line vibrates to sell the "fish struggling" beat.
    private static final float TENSION_VIBRATION_THRESHOLD = 0.9F;
    private static final float TENSION_VIBRATION_FREQUENCY = 0.8F;
    private static final float TENSION_VIBRATION_AMPLITUDE = 0.05F;

    public VillagerFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@Nonnull VillagerFishingHook entity, float entityYaw, float partialTicks,
                       @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight) {
        Mob villager = entity.getVillagerOwner();
        if (villager == null) {
            return;
        }

        poseStack.pushPose();

        // Render bobber sprite
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose bobberPose = poseStack.last();

        VertexConsumer bobberConsumer = buffer.getBuffer(RENDER_TYPE);
        vertex(bobberConsumer, bobberPose, packedLight, 0.0F, 0, 0, 1);
        vertex(bobberConsumer, bobberPose, packedLight, 1.0F, 0, 1, 1);
        vertex(bobberConsumer, bobberPose, packedLight, 1.0F, 1, 1, 0);
        vertex(bobberConsumer, bobberPose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        // Interpolating the yaw across partial ticks prevents the line origin from popping when the villager pivots between ticks.
        float bodyYaw = Mth.lerp(partialTicks, villager.yBodyRotO, villager.yBodyRot);
        float angleRad = bodyYaw * ((float) Math.PI / 180F);
        float sin = Mth.sin(angleRad);
        float cos = Mth.cos(angleRad);

        Vec3 rodTipWorldPos = villager.getPosition(partialTicks).add(
                -sin * ROD_TIP_FORWARD_OFFSET,
                ROD_TIP_UPWARD_OFFSET,
                cos * ROD_TIP_FORWARD_OFFSET
        );
        Vec3 hookPos = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);

        // The PoseStack origin is already at the hook's position (set up by EntityRenderer base class),
        // so line vertices are emitted as deltas from the hook toward the rod tip.
        float dx = (float) (rodTipWorldPos.x - hookPos.x);
        float dy = (float) (rodTipWorldPos.y - hookPos.y);
        float dz = (float) (rodTipWorldPos.z - hookPos.z);

        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose linePose = poseStack.last();

        float tension = computeTension(villager, entity.tickCount);
        for (int i = 0; i <= LINE_SEGMENTS; i++) {
            stringVertex(dx, dy, dz, lineConsumer, linePose, fraction(i), fraction(i + 1), tension);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float fraction(int numerator) {
        return (float) numerator / (float) VillagerFishingHookRenderer.LINE_SEGMENTS;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(float x, float y, float z, VertexConsumer consumer,
                                     PoseStack.Pose pose, float frac, float nextFrac, float tension) {
        float segmentX = x * frac;
        float segmentY = lineHeight(y, frac, tension);
        float segmentZ = z * frac;
        float directionX = x * nextFrac - segmentX;
        float directionY = lineHeight(y, nextFrac, tension) - segmentY;
        float directionZ = z * nextFrac - segmentZ;
        float length = Mth.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        directionX /= length;
        directionY /= length;
        directionZ /= length;
        consumer.addVertex(pose, segmentX, segmentY, segmentZ)
                .setColor(-16777216)
                .setNormal(pose, directionX, directionY, directionZ);
    }

    /**
     * Calculates the Y position of a line segment.
     * Uses a straight line base and subtracts a parabolic sag.
     */
    private static float lineHeight(float y, float frac, float tension) {
        // Define the maximum depth (in blocks) the line hangs below a straight path
        float maxLooseDroop = 1.8F; // Increase for more slack
        float maxTightDroop = 0.3F;

        // Lerp the absolute droop amount based on current tension
        float currentDroop = Mth.lerp(tension, maxLooseDroop, maxTightDroop);

        // Base Straight Line - Parabolic Sag + Hook Offset
        return (y * frac) - (currentDroop * frac * (1.0F - frac)) + 0.25F;
    }

    /**
     * REEL_OUT (jigging the fish) and REEL_IN (final yank) are both "line under load" states from the player's POV,
     * so both flip the line taut. The vibration past the threshold gives the impression of a fish fighting back.
     */
    private static float computeTension(@Nonnull Mob villager, int tickCount) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return 0.0F;
        }
        AnimationArchetype motion = baseVillager.getMotion();
        boolean fighting = motion == AnimationArchetype.REEL_OUT || motion == AnimationArchetype.REEL_IN;
        float tension = fighting ? 1.0F : 0.0F;
        if (tension > TENSION_VIBRATION_THRESHOLD) {
            tension -= Mth.sin(tickCount * TENSION_VIBRATION_FREQUENCY) * TENSION_VIBRATION_AMPLITUDE;
        }
        return tension;
    }

    @Override
    @Nonnull
    public ResourceLocation getTextureLocation(@Nonnull VillagerFishingHook entity) {
        return TEXTURE_LOCATION;
    }

}
