package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationSelectionContext;
import dev.breezes.settlements.domain.animation.VillagerAnimator;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.domain.presentation.ItemCategory;
import dev.breezes.settlements.infrastructure.minecraft.attachments.EquipmentLookup;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.SettlementsVillagerModel;
import dev.breezes.settlements.infrastructure.rendering.animation.debug.DebugPoseOverride;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.CustomLog;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

@CustomLog
public final class SettlementsVillagerRenderer extends MobRenderer<BaseVillager, SettlementsVillagerModel<BaseVillager>> {

    private static final float SHADOW_RADIUS = 0.5F;
    private static final float BABY_SCALE = 0.5F;

    private final AttachmentRenderLayer attachmentRenderLayer;
    private final DebugPoseOverride debugPoseOverride;
    private AnimationFrame currentFrame = AnimationFrame.EMPTY;

    public SettlementsVillagerRenderer(@Nonnull EntityRendererProvider.Context context) {
        super(context, new SettlementsVillagerModel<>(context.bakeLayer(SettlementsVillagerModel.LAYER)), SHADOW_RADIUS);

        // Minecraft owns renderer construction, so the Dagger graph is reached through the project bootstrap bridge.
        ClientComponent clientComponent = SettlementsDagger.client();
        this.attachmentRenderLayer = new AttachmentRenderLayer(
                this,
                context.getItemInHandRenderer(),
                clientComponent.attachmentProviders(),
                clientComponent.slotAnchorRegistry(),
                clientComponent.socketRegistry(),
                clientComponent.attachmentDisplayProfileRegistry());
        this.debugPoseOverride = clientComponent.debugPoseOverride();
        this.addLayer(this.attachmentRenderLayer);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager"));
        this.addLayer(new SootOverlayRenderLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(@Nonnull BaseVillager villager) {
        return ResourceLocationUtil.mod("textures/entity/base_villager.png");
    }

    @Override
    public void render(@Nonnull BaseVillager villager,
                       float yaw,
                       float partialTicks,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource buffer,
                       int packedLight) {
        if (villager.isBaby()) {
            poseStack.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
        }

        long gameTime = villager.level().getGameTime();
        VillagerAnimator animator = this.getOrUpdateAnimator(villager, gameTime);
        this.currentFrame = this.debugPoseOverride.applyTo(animator.sample(gameTime, partialTicks));
        this.model.setAnimationFrame(this.currentFrame);
        // Arm config is read from the animator after sampling so the same resolved animation
        // drives both the frame and the per-arm geometry visibility.
        this.model.setArmConfiguration(animator.currentArmConfiguration());
        try {
            super.render(villager, yaw, partialTicks, poseStack, buffer, packedLight);
        } finally {
            this.currentFrame = AnimationFrame.EMPTY;
            this.model.setAnimationFrame(null);
            this.model.setArmConfiguration(ArmConfiguration.BOTH_CROSSED);
        }
    }

    public AnimationFrame currentFrame() {
        return this.currentFrame;
    }

    /**
     * Advances the animator's motion state and returns it so the caller can sample both the
     * animation frame and the arm configuration from a single up-to-date animator instance.
     */
    private VillagerAnimator getOrUpdateAnimator(@Nonnull BaseVillager villager, long gameTime) {
        VillagerAnimator animator = SettlementsDagger.clientSessionOrThrow()
                .clientAnimatorRegistry()
                .getOrCreate(villager);
        AnimationArchetype currentMotion = villager.getMotion();
        byte currentGeneration = villager.getMotionGeneration();

        if (animator.getLastSeenArchetype() != currentMotion || animator.getLastSeenGeneration() != currentGeneration) {
            AnimationSelectionContext ctx = selectionContext(villager);
            log.debug("Renderer: entity {} motion {} -> {} (gen={}, main hand: {})",
                    villager.getId(), animator.getLastSeenArchetype(), currentMotion, currentGeneration, ctx.mainHandCategory());
            animator.onMotionChanged(currentMotion, currentGeneration, ctx, gameTime);
        } else {
            // Re-evaluate context every frame so item category changes caused by the 1-frame
            // equipment-sync lag after archetype sync are caught and resolved correctly.
            animator.tickContext(selectionContext(villager), gameTime);
        }

        return animator;
    }

    private static AnimationSelectionContext selectionContext(@Nonnull BaseVillager villager) {
        ItemCategory mainHandCategory = EquipmentLookup.find(villager)
                .flatMap(equipment -> equipment.getEquipped(EquipmentSlot.MAIN_HAND))
                .map(ItemCategory::of)
                .orElse(ItemCategory.GENERIC);
        return new AnimationSelectionContext(mainHandCategory);
    }

}
