package dev.breezes.settlements.infrastructure.minecraft.blocks.totem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TotemOfCultivationRenderer implements BlockEntityRenderer<TotemOfCultivationBlockEntity> {

    private static final float FLOAT_HEIGHT_BLOCKS = TotemOfCultivationBlockEntity.FLOAT_HEIGHT_BLOCKS;
    private static final float BOB_AMPLITUDE_BLOCKS = 0.15F;
    private static final float BOB_DEGREES_PER_TICK = 3.6F;
    private static final float FLOATING_TOTEM_SCALE = 0.55F;
    private static final float FILTER_ITEM_SCALE = 0.45F;
    private static final float SPIN_DEGREES_PER_TICK = 1.8F;

    private final ItemRenderer itemRenderer;

    public TotemOfCultivationRenderer(@Nonnull BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(@Nonnull TotemOfCultivationBlockEntity totem,
                       float partialTick,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        renderCropFilter(totem, poseStack, bufferSource, packedLight, packedOverlay);
        if (totem.isValid()) {
            renderFloatingTotem(totem, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderFloatingTotem(@Nonnull TotemOfCultivationBlockEntity totem,
                                     float partialTick,
                                     @Nonnull PoseStack poseStack,
                                     @Nonnull MultiBufferSource bufferSource,
                                     int packedLight,
                                     int packedOverlay) {
        if (totem.getLevel() == null) {
            return;
        }

        long gameTime = totem.getLevel().getGameTime();

        // Bob phase uses a 100-tick (5-second) cycle; float precision is safe at game-time scales
        // because we reduce to [0, 100) before multiplying by the per-tick rate.
        float bobPhase = (gameTime % 100L + partialTick) * BOB_DEGREES_PER_TICK;
        float bobOffset = (float) (BOB_AMPLITUDE_BLOCKS * Math.sin(Math.toRadians(bobPhase)));

        float spin = (gameTime % 360L + partialTick) * SPIN_DEGREES_PER_TICK;

        poseStack.pushPose();
        poseStack.translate(0.5D, FLOAT_HEIGHT_BLOCKS + bobOffset, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(FLOATING_TOTEM_SCALE, FLOATING_TOTEM_SCALE, FLOATING_TOTEM_SCALE);
        this.itemRenderer.renderStatic(ItemRegistry.TOTEM_OF_CULTIVATION.get().getDefaultInstance(),
                ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, totem.getLevel(), 0);
        poseStack.popPose();
    }

    private void renderCropFilter(@Nonnull TotemOfCultivationBlockEntity totem,
                                  @Nonnull PoseStack poseStack,
                                  @Nonnull MultiBufferSource bufferSource,
                                  int packedLight,
                                  int packedOverlay) {
        ResourceLocation displayItemId = totem.getCropFilterDisplayItem();
        if (displayItemId == null) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(displayItemId);
        renderFilterStack(totem, item.getDefaultInstance(), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderFilterStack(@Nonnull TotemOfCultivationBlockEntity totem,
                                   @Nonnull ItemStack stack,
                                   @Nonnull PoseStack poseStack,
                                   @Nonnull MultiBufferSource bufferSource,
                                   int packedLight,
                                   int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.12D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(FILTER_ITEM_SCALE, FILTER_ITEM_SCALE, FILTER_ITEM_SCALE);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, totem.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@Nonnull TotemOfCultivationBlockEntity blockEntity) {
        return true;
    }

}
