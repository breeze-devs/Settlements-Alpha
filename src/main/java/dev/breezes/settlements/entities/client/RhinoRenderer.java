package dev.breezes.settlements.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.entities.custom.RhinoEntity;
import dev.breezes.settlements.util.ResourceLocationUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RhinoRenderer extends MobRenderer<RhinoEntity, RhinoModel<RhinoEntity>> {

    public RhinoRenderer(EntityRendererProvider.Context context) {
        super(context, new RhinoModel<>(context.bakeLayer(ModModelLayers.RHINO_LAYER)), 2F);
    }

    @Override
    public ResourceLocation getTextureLocation(RhinoEntity rhinoEntity) {
        return ResourceLocationUtil.mod("textures/entity/rhino.png");
    }

    @Override
    public void render(RhinoEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }

        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

}
