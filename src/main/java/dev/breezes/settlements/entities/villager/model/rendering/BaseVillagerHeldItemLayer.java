package dev.breezes.settlements.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.model.BaseVillagerModel;
import dev.breezes.settlements.entities.villager.model.rendering.predicates.IsSuitableToolPredicate;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Refer to: net.minecraft.client.renderer.entity.layers
 * Example: PlayerItemInHandLayer
 */
@OnlyIn(Dist.CLIENT)
public class BaseVillagerHeldItemLayer<V extends BaseVillager, M extends BaseVillagerModel<V> & ArmedModel & HeadedModel> extends ItemInHandLayer<V, M> {

    private static final float X_ROT_MIN = (-(float) Math.PI / 6F);
    private static final float X_ROT_MAX = ((float) Math.PI / 2F);

    private final ItemInHandRenderer itemInHandRenderer;

    public BaseVillagerHeldItemLayer(RenderLayerParent<V, M> renderer, ItemInHandRenderer itemInHandRenderer) {
        super(renderer, itemInHandRenderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    protected void renderArmWithItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        // Check if it's a tool to be rendered as a 3D model
        if (new IsSuitableToolPredicate().test(itemStack.getItem())) {
            // Render the tool as a 3D model
            this.renderArmWithSpyglass(livingEntity, itemStack, arm, pose, buffer, packedLight);
        } else {
            // Not a tool -- a block, item, or something else
            super.renderArmWithItem(livingEntity, itemStack, context, arm, pose, buffer, packedLight);
        }

    }

    private void renderArmWithSpyglass(LivingEntity pEntity, ItemStack pStack, HumanoidArm pArm, PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight) {
        pPoseStack.pushPose();
        ModelPart modelpart = this.getParentModel().getHead();
        float f = modelpart.xRot;
        modelpart.xRot = Mth.clamp(modelpart.xRot, (-(float) Math.PI / 6F), ((float) Math.PI / 2F));
        modelpart.translateAndRotate(pPoseStack);
        modelpart.xRot = f;
        CustomHeadLayer.translateToHead(pPoseStack, false);
        boolean flag = pArm == HumanoidArm.LEFT;
        pPoseStack.translate((flag ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
        this.itemInHandRenderer.renderItem(pEntity, pStack, ItemDisplayContext.HEAD, false, pPoseStack, pBuffer, pCombinedLight);
        pPoseStack.popPose();
    }

}
