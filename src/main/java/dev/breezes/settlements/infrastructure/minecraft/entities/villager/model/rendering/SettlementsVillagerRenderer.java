package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationSelectionContext;
import dev.breezes.settlements.domain.animation.VillagerAnimator;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.presentation.ItemCategory;
import dev.breezes.settlements.infrastructure.minecraft.attachments.EquipmentLookup;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.SettlementsVillagerModel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public final class SettlementsVillagerRenderer extends MobRenderer<BaseVillager, SettlementsVillagerModel<BaseVillager>> {

    private static final float SHADOW_RADIUS = 0.5F;
    private static final float BABY_SCALE = 0.5F;

    private final AttachmentRenderLayer attachmentRenderLayer;

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
        this.model.prepareAnimation(animator, villager.getLocomotionNavigationType(), gameTime, partialTicks);
        try {
            super.render(villager, yaw, partialTicks, poseStack, buffer, packedLight);
        } finally {
            this.model.clearPreparedAnimation();
        }
    }

    public AnimationFrame currentFrame() {
        return this.model.getResolvedAnimationFrame();
    }

    /**
     * Advances the animator's motion state and returns it so the caller can sample both the
     * animation frame and the arm configuration from a single up-to-date animator instance.
     */
    private VillagerAnimator getOrUpdateAnimator(@Nonnull BaseVillager villager, long gameTime) {
        VillagerAnimator animator = SettlementsDagger.clientSessionOrThrow()
                .clientAnimatorRegistry()
                .getOrCreate(villager);

        // Sleep is read straight off the vanilla-synced state; while asleep the animator plays the
        // sleep clip and suppresses the idle ambience entirely (no new sync surface needed).
        animator.setSleeping(villager.isSleeping(), gameTime);

        AnimationArchetype currentMotion = villager.getMotion();
        byte currentGeneration = villager.getMotionGeneration();

        if (animator.getLastSeenArchetype() != currentMotion || animator.getLastSeenGeneration() != currentGeneration) {
            AnimationSelectionContext ctx = selectionContext(villager);
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
