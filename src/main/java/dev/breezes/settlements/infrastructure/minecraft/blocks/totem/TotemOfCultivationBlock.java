package dev.breezes.settlements.infrastructure.minecraft.blocks.totem;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The Totem of Cultivation is a lily-pad-style block that designates a managed farm zone.
 * <p>
 * It occupies the air block directly above a water source, placed via {@link net.minecraft.world.item.PlaceOnWaterBlockItem}.
 * The base is visually and physically identical to a vanilla lily pad — same flat AABB, walkable,
 * instabreak. Only a water source block beneath it keeps it alive — if the water is drained,
 * neighborChanged triggers canSurvive which returns false, and the block drops for free with no polling.
 * <p>
 * The floating spinning totem mesh is render-only and lives in Phase 2 (BlockEntityRenderer).
 * The block entity (TotemOfCultivationBlockEntity) holds all zone configuration.
 */
public class TotemOfCultivationBlock extends BaseEntityBlock {

    public static final MapCodec<TotemOfCultivationBlock> CODEC = simpleCodec(TotemOfCultivationBlock::new);

    /**
     * Lily-pad hit box: a thin, nearly full-block slab at ground level
     */
    private static final VoxelShape SHAPE = box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    private static final String KEY_RESIZED = "action.settlements.totem_of_cultivation.resized";
    private static final String KEY_FILTER_SET = "action.settlements.totem_of_cultivation.filter_set";
    private static final String KEY_FILTER_CLEARED = "action.settlements.totem_of_cultivation.filter_cleared";

    public TotemOfCultivationBlock(@Nonnull BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    protected MapCodec<TotemOfCultivationBlock> codec() {
        return CODEC;
    }

    /**
     * The totem must sit directly above a water source block.
     * <p>
     * This matches the lily-pad placement contract: when the water is drained the block
     * receives a neighbor update, canSurvive returns false, and NeoForge schedules a drop.
     */
    @Override
    public boolean canSurvive(@Nonnull BlockState state,
                              @Nonnull LevelReader level,
                              @Nonnull BlockPos pos) {
        FluidState fluidBelow = level.getFluidState(pos.below());
        // A still water source is the only valid foundation
        return fluidBelow.is(Fluids.WATER) && fluidBelow.isSource();
    }

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }


    @Override
    @Nonnull
    protected BlockState updateShape(@Nonnull BlockState state,
                                     @Nonnull Direction direction,
                                     @Nonnull BlockState neighborState,
                                     @Nonnull LevelAccessor level,
                                     @Nonnull BlockPos pos,
                                     @Nonnull BlockPos neighborPos) {
        // Pop to air when water below is removed or replaced
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state,
                               @Nonnull BlockGetter level,
                               @Nonnull BlockPos pos,
                               @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nonnull
    public VoxelShape getCollisionShape(@Nonnull BlockState state,
                                        @Nonnull BlockGetter level,
                                        @Nonnull BlockPos pos,
                                        @Nonnull CollisionContext context) {
        // Walkable, exactly like a vanilla lily pad
        return SHAPE;
    }

    @Override
    @Nonnull
    protected InteractionResult useWithoutItem(@Nonnull BlockState state,
                                               @Nonnull Level level,
                                               @Nonnull BlockPos pos,
                                               @Nonnull Player player,
                                               @Nonnull BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TotemOfCultivationBlockEntity totem)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            resizeFacingAxis(player, totem);
            player.displayClientMessage(resizeMessage(totem), true);
        }

        totem.beginBorderPreview();
        return InteractionResult.CONSUME;
    }

    @Override
    @Nonnull
    protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack,
                                              @Nonnull BlockState state,
                                              @Nonnull Level level,
                                              @Nonnull BlockPos pos,
                                              @Nonnull Player player,
                                              @Nonnull InteractionHand hand,
                                              @Nonnull BlockHitResult hitResult) {
        CultivationCropRegistry cropRegistry = SettlementsDagger.component().cultivationCropDataManager();
        return cropRegistry.resolveBySeedItem(stack)
                .map(crop -> applyCropFilterInteraction(level, pos, player, crop))
                .orElse(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    private static ItemInteractionResult applyCropFilterInteraction(@Nonnull Level level,
                                                                    @Nonnull BlockPos pos,
                                                                    @Nonnull Player player,
                                                                    @Nonnull CultivationCropDefinition crop) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TotemOfCultivationBlockEntity totem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (totem.hasCropFilter(crop.cropBlock())) {
            totem.setCropFilter(null, null);
            player.displayClientMessage(Component.translatable(KEY_FILTER_CLEARED).withStyle(ChatFormatting.YELLOW), true);
        } else {
            totem.setCropFilter(crop.cropBlock(), crop.displayItem());
            player.displayClientMessage(Component.translatable(KEY_FILTER_SET,
                    Component.translatable(stackDescriptionKey(crop))).withStyle(ChatFormatting.GREEN), true);
        }

        totem.beginBorderPreview();
        return ItemInteractionResult.CONSUME;
    }

    private static void resizeFacingAxis(@Nonnull Player player, @Nonnull TotemOfCultivationBlockEntity totem) {
        Direction facing = player.getDirection();
        if (facing.getAxis() == Direction.Axis.X) {
            totem.cycleHalfExtentX(1);
        } else if (facing.getAxis() == Direction.Axis.Z) {
            totem.cycleHalfExtentZ(1);
        }
    }

    private static Component resizeMessage(@Nonnull TotemOfCultivationBlockEntity totem) {
        int widthX = totem.getHalfExtentX() * 2 + 1;
        int widthZ = totem.getHalfExtentZ() * 2 + 1;
        return Component.translatable(KEY_RESIZED, widthX, widthZ)
                .withStyle(ChatFormatting.GREEN);
    }

    private static String stackDescriptionKey(@Nonnull CultivationCropDefinition crop) {
        Item displayItem = BuiltInRegistries.ITEM.get(crop.displayItem());
        return displayItem.getDescriptionId();
    }

    /**
     * Returns a per-side ticker for the block entity.
     * <p>
     * The server ticker drives periodic validity recompute and the border-outline preview.
     * The client ticker drives the ambient magic-orb aura — emission is gated on the {@code valid}
     * flag and a cadence inside {@link TotemOfCultivationBlockEntity#clientTick}, so only live,
     * valid totems near a player actually add particles.
     * {@code createTickerHelper} is the NeoForge/MC-provided helper on BaseEntityBlock that
     * produces a type-safe ticker and avoids unchecked-cast warnings.
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level,
                                                                  @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.TOTEM_OF_CULTIVATION.get(),
                    TotemOfCultivationBlockEntity::clientTick);
        }
        return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.TOTEM_OF_CULTIVATION.get(),
                TotemOfCultivationBlockEntity::serverTick);
    }

    /**
     * Schedules a prompt validity recompute when a neighboring block changes.
     * <p>
     * {@code updateShape} already handles the "water removed → block drops" case via canSurvive.
     * This override catches in-zone changes that are not direct neighbors of the totem (e.g. a
     * player converting farmland to a path several blocks away). For those the throttled tick
     * is the primary driver; this dirty flag is an eagerness optimization for the immediate
     * water-neighbor case without duplicating scans on periodic recheck ticks.
     * <p>
     * Called on both sides; the BE recompute is only meaningful server-side where the level holds
     * authoritative block state, so we guard accordingly.
     */
    @Override
    protected void neighborChanged(@Nonnull BlockState state,
                                   @Nonnull Level level,
                                   @Nonnull BlockPos pos,
                                   @Nonnull Block neighborBlock,
                                   @Nonnull BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof TotemOfCultivationBlockEntity totem) {
            totem.markValidityDirty();
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos,
                                      @Nonnull BlockState state) {
        return BlockEntityTypeRegistry.TOTEM_OF_CULTIVATION.get().create(pos, state);
    }

}
