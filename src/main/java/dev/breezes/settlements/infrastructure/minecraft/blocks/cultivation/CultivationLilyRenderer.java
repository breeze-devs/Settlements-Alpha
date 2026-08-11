package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

@ClientSide
public class CultivationLilyRenderer implements BlockEntityRenderer<CultivationLilyBlockEntity> {

    public static final ModelResourceLocation FLOATING_MESH_MODEL =
            ModelResourceLocation.standalone(ResourceLocationUtil.mod("block/cultivation_lily_floating"));

    private static final float FLOAT_HEIGHT_BLOCKS = CultivationLilyBlockEntity.FLOAT_HEIGHT_BLOCKS;
    private static final float BOB_AMPLITUDE_BLOCKS = 0.15F;
    private static final float BOB_DEGREES_PER_TICK = 3.6F;
    private static final float FLOATING_TOTEM_SCALE = 0.55F;
    private static final float SPIN_DEGREES_PER_TICK = 1.8F;

    private static final float FULL_TURN_DEGREES = 360.0F;

    private static final float FILTER_ITEM_SCALE = 0.45F;
    private static final double FILTER_ITEM_HEIGHT_BLOCKS = 0.12D;
    // Flat against the lily pad, so the filter reads as an item resting in the pad's socket
    private static final float FILTER_ITEM_TILT_DEGREES = 90.0F;

    /**
     * Headroom around the totem's fully risen height, absorbing its bob and spin silhouette so the render
     * bounding box need not track those. Applied sideways as well as upward: culling a lily a frame late
     * costs almost nothing, where a box narrower than the spinning silhouette pops the mesh in and out at
     * the screen edge.
     */
    private static final double RENDER_BOUNDS_MARGIN_BLOCKS = 1.0;

    private final ItemRenderer itemRenderer;

    public CultivationLilyRenderer(@Nonnull BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    /**
     * A continuously advancing motion's angle at the given moment, wrapped into a single turn so float
     * precision stays meaningful at game-time scales.
     * <p>
     * Wrapping the angle rather than the tick count is what keeps the speed a free parameter. Reducing in
     * tick space instead needs a period that divides a whole turn exactly, and a speed that does not divide
     * one snaps the mesh backwards on every wrap — silently, and only for whoever retunes the speed next.
     * <p>
     * Ticks accumulate in double because a float stops resolving a single tick past 2^24 of them.
     */
    private static float cyclicAngleDegrees(long gameTime, float partialTick, float degreesPerTick) {
        double elapsedTicks = gameTime + (double) partialTick;
        double totalDegrees = elapsedTicks * degreesPerTick;
        return (float) (totalDegrees % FULL_TURN_DEGREES);
    }

    @Override
    public void render(@Nonnull CultivationLilyBlockEntity lily,
                       float partialTick,
                       @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        renderCropFilter(lily, poseStack, bufferSource, packedLight, packedOverlay);
        if (lily.isValid()) {
            renderFloatingTotem(lily, partialTick, poseStack, bufferSource, packedOverlay);
        }
    }

    /**
     * Rise height and scale both climb from zero with {@link CultivationLilyBlockEntity#bloomProgress}, so
     * the totem grows out of the pad rather than popping in at full size.
     */
    private void renderFloatingTotem(@Nonnull CultivationLilyBlockEntity lily,
                                     float partialTick,
                                     @Nonnull PoseStack poseStack,
                                     @Nonnull MultiBufferSource bufferSource,
                                     int packedOverlay) {
        if (lily.getLevel() == null) {
            return;
        }

        long gameTime = lily.getLevel().getGameTime();
        float bloomProgress = lily.bloomProgress(gameTime, partialTick);

        float bobPhase = cyclicAngleDegrees(gameTime, partialTick, BOB_DEGREES_PER_TICK);
        float bobOffset = (float) (BOB_AMPLITUDE_BLOCKS * Math.sin(Math.toRadians(bobPhase))) * bloomProgress;

        float spin = cyclicAngleDegrees(gameTime, partialTick, SPIN_DEGREES_PER_TICK);
        float riseHeight = Mth.lerp(bloomProgress, 0.0F, FLOAT_HEIGHT_BLOCKS);
        float scale = Mth.lerp(bloomProgress, 0.0F, FLOATING_TOTEM_SCALE);

        BakedModel floatingMeshModel = Minecraft.getInstance().getModelManager().getModel(FLOATING_MESH_MODEL);

        poseStack.pushPose();
        poseStack.translate(0.5D, riseHeight + bobOffset, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(scale, scale, scale);
        // Render the floating mesh fully lit
        this.itemRenderer.render(ItemRegistry.CULTIVATION_LILY.get().getDefaultInstance(), ItemDisplayContext.FIXED,
                false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, floatingMeshModel);
        poseStack.popPose();
    }

    private void renderCropFilter(@Nonnull CultivationLilyBlockEntity lily,
                                  @Nonnull PoseStack poseStack,
                                  @Nonnull MultiBufferSource bufferSource,
                                  int packedLight,
                                  int packedOverlay) {
        ResourceLocation displayItemId = lily.getCropFilterDisplayItem();
        if (displayItemId == null || lily.getLevel() == null) {
            return;
        }

        ItemStack stack = BuiltInRegistries.ITEM.get(displayItemId).getDefaultInstance();

        poseStack.pushPose();
        poseStack.translate(0.5D, FILTER_ITEM_HEIGHT_BLOCKS, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(FILTER_ITEM_TILT_DEGREES));
        poseStack.scale(FILTER_ITEM_SCALE, FILTER_ITEM_SCALE, FILTER_ITEM_SCALE);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, lily.getLevel(), 0);
        poseStack.popPose();
    }

    /**
     * The totem rises {@link #FLOAT_HEIGHT_BLOCKS} above the pad, outside the unit-cube bounds the
     * dispatcher would otherwise cull against — widened here rather than opting out of frustum culling.
     */
    @Override
    public AABB getRenderBoundingBox(@Nonnull CultivationLilyBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double top = pos.getY() + 1.0 + FLOAT_HEIGHT_BLOCKS + RENDER_BOUNDS_MARGIN_BLOCKS;
        return new AABB(pos.getX() - RENDER_BOUNDS_MARGIN_BLOCKS, pos.getY(), pos.getZ() - RENDER_BOUNDS_MARGIN_BLOCKS,
                pos.getX() + 1.0 + RENDER_BOUNDS_MARGIN_BLOCKS, top, pos.getZ() + 1.0 + RENDER_BOUNDS_MARGIN_BLOCKS);
    }

}
