package dev.breezes.settlements.entities.villager.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.model.BaseVillagerModel;
import dev.breezes.settlements.util.ResourceLocationUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// TODO: something like public class BaseVillagerRenderer<T extends BaseVillager> extends MobRenderer<T, BaseVillagerModel<T>>
public class BaseVillagerRenderer extends MobRenderer<BaseVillager, BaseVillagerModel<BaseVillager>> {

    public BaseVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new BaseVillagerModel<>(context.bakeLayer(BaseVillagerModel.LAYER)), 2F);
        this.addLayer(new BaseVillagerHeldItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(BaseVillager pEntity) {
        return ResourceLocationUtil.mod("textures/entity/base_villager.png");
    }

    @Override
    public void render(BaseVillager entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }

        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

}
