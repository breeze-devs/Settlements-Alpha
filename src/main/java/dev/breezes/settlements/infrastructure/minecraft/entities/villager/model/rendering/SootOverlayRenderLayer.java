package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.SettlementsVillagerModel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import javax.annotation.Nonnull;

/**
 * Renders a translucent soot texture over the villager's head after a blast misfire.
 * The texture already confines soot pixels to the head UV region, so no model-part
 * masking is needed — re-rendering the full model with the soot texture is correct and safe.
 */
public final class SootOverlayRenderLayer extends RenderLayer<BaseVillager, SettlementsVillagerModel<BaseVillager>> {

    public SootOverlayRenderLayer(@Nonnull SettlementsVillagerRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource buffer,
                       int packedLight,
                       @Nonnull BaseVillager villager,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        if (!villager.isSooty()) {
            return;
        }

        RenderType renderType = RenderType.entityTranslucent(ResourceLocationUtil.mod("textures/entity/villager_soot.png"));
        // Re-use the already-posed model with the soot texture so the overlay exactly follows
        // the villager's head without any additional transform work
        this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(renderType), packedLight,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

}
