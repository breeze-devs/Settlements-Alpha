package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The Cultivation Lily is a lily-pad-style block that designates a managed farm zone.
 * <p>
 * The base is physically identical to a vanilla lily pad — same hitbox, walkable, instabreak.
 * The floating spinning mesh is render-only, handled separately by {@link CultivationLilyRenderer}.
 * The block entity (CultivationLilyBlockEntity) holds all zone configuration.
 */
public class CultivationLilyBlock extends BaseEntityBlock {

    public static final MapCodec<CultivationLilyBlock> CODEC = simpleCodec(CultivationLilyBlock::new);

    private static final int ACTIVE_LIGHT_LEVEL = 15;

    /**
     * Lily-pad hit box.
     */
    private static final VoxelShape SHAPE = box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    public CultivationLilyBlock(@Nonnull BlockBehaviour.Properties properties) {
        super(properties);
        // A freshly placed lily is unlit; the block entity flips LIT on once it validates its water base
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    /**
     * The lily only radiates light while lit, i.e. while it is an active, valid farm zone.
     * The {@link BlockStateProperties#LIT} flag is authored by the block entity's validity recompute.
     */
    public static int lightEmission(@Nonnull BlockState state) {
        return state.getValue(BlockStateProperties.LIT) ? ACTIVE_LIGHT_LEVEL : 0;
    }

    @Override
    @Nonnull
    protected MapCodec<CultivationLilyBlock> codec() {
        return CODEC;
    }

    /**
     * The lily must sit directly above a water source block.
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

    /**
     * Vanilla dispatch alone decides what reaches here. A decline passes to the default block interaction,
     * so the held item still gets its own turn afterwards — placing a block, eating, drawing a bow.
     * <p>
     * An empty-handed right-click arrives here too, with an empty stack, rather than at useWithoutItem.
     * Both empty-handed gestures — resize and clearing the filter — therefore live here with the rest of
     * the verb table, and useWithoutItem is left un-overridden: its vanilla default is a non-consuming
     * pass, which is what a decline needs to fall through to placement.
     */
    @Override
    @Nonnull
    protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack,
                                              @Nonnull BlockState state,
                                              @Nonnull Level level,
                                              @Nonnull BlockPos pos,
                                              @Nonnull Player player,
                                              @Nonnull InteractionHand hand,
                                              @Nonnull BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CultivationLilyBlockEntity lily)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return level.isClientSide()
                ? resolveClientVerb(stack, hand, player)
                : resolveServerVerb(stack, hand, player, lily);
    }

    /**
     * Predicts the verb without mutating: the block entity's sync packet is what the client's rendered
     * state follows.
     * <p>
     * The crop registry itself is server-only (CultivationCropDataManager is a datapack
     * reload listener), so seed-ness is read from the synced client projection rather than resolved
     * here directly. A missing client graph degrades to treating the stack as not-a-seed, which only
     * ever narrows the predicted verb — the server still decides what actually happens.
     */
    private static ItemInteractionResult resolveClientVerb(@Nonnull ItemStack stack,
                                                           @Nonnull InteractionHand hand,
                                                           @Nonnull Player player) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        boolean stackIsAcceptedSeed = clientComponent != null
                && clientComponent.cultivationSeedSetClientProjection().isAcceptedSeed(stack);
        CultivationLilyVerb verb = resolveVerb(stack, hand, player, stackIsAcceptedSeed);

        return verb == CultivationLilyVerb.DECLINE
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SUCCESS;
    }

    private static ItemInteractionResult resolveServerVerb(@Nonnull ItemStack stack,
                                                           @Nonnull InteractionHand hand,
                                                           @Nonnull Player player,
                                                           @Nonnull CultivationLilyBlockEntity lily) {
        CultivationCropRegistry cropRegistry = SettlementsDagger.component().cultivationCropDataManager();
        Optional<CultivationCropDefinition> resolvedCrop = cropRegistry.resolveBySeedItem(stack);
        CultivationLilyVerb verb = resolveVerb(stack, hand, player, resolvedCrop.isPresent());

        return switch (verb) {
            case RESIZE -> applyResize(player, lily);
            // resolvedCrop is present by construction: SET_FILTER only results when resolvedCrop.isPresent() fed the resolve() call above.
            case SET_FILTER -> applySetFilter(player, lily, resolvedCrop.orElseThrow());
            case CLEAR_FILTER -> applyClearFilter(player, lily);
            case DECLINE -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /**
     * Seed-ness is the only input the two sides can source differently — the client reads its synced
     * projection, the server the datapack registry. Every other input is derived here exactly once so
     * that a later edit to what counts as a gesture cannot land on one side and miss the other, which
     * is the one divergence this interaction has no way to recover from.
     */
    private static CultivationLilyVerb resolveVerb(@Nonnull ItemStack stack,
                                                   @Nonnull InteractionHand hand,
                                                   @Nonnull Player player,
                                                   boolean stackIsAcceptedSeed) {
        boolean handEmpty = CultivationLilyVerb.isEmptyHandGesture(stack.isEmpty(), hand == InteractionHand.MAIN_HAND);
        return CultivationLilyVerb.resolve(player.isShiftKeyDown(), handEmpty, stackIsAcceptedSeed);
    }

    private static ItemInteractionResult applyResize(@Nonnull Player player, @Nonnull CultivationLilyBlockEntity lily) {
        int resultingHalfExtent = resizeFacingAxis(player, lily);
        CultivationLilySoundPalette.resize(locationOf(player, lily), resultingHalfExtent);
        return ItemInteractionResult.CONSUME;
    }

    private static ItemInteractionResult applySetFilter(@Nonnull Player player,
                                                        @Nonnull CultivationLilyBlockEntity lily,
                                                        @Nonnull CultivationCropDefinition crop) {
        lily.setCropFilter(crop.cropBlock(), crop.displayItem());
        CultivationLilySoundPalette.filterSet(locationOf(player, lily));
        return ItemInteractionResult.CONSUME;
    }

    private static ItemInteractionResult applyClearFilter(@Nonnull Player player, @Nonnull CultivationLilyBlockEntity lily) {
        if (lily.getCropFilter() == null) {
            CultivationLilySoundPalette.filterAlreadyCleared(locationOf(player, lily));
            return ItemInteractionResult.CONSUME;
        }

        lily.setCropFilter(null, null);
        CultivationLilySoundPalette.filterCleared(locationOf(player, lily));
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Resizes the half-extent facing the player and returns the resulting value, so the caller can
     * sound the resize without re-reading which axis just changed. Player.getDirection only ever
     * reports a horizontal facing, so the X/Z split is exhaustive despite {@link Direction.Axis}
     * nominally including Y.
     */
    private static int resizeFacingAxis(@Nonnull Player player, @Nonnull CultivationLilyBlockEntity lily) {
        Direction facing = player.getDirection();
        if (facing.getAxis() == Direction.Axis.X) {
            lily.cycleHalfExtentX();
            return lily.getZone().halfExtentX();
        }

        lily.cycleHalfExtentZ();
        return lily.getZone().halfExtentZ();
    }

    private static Location locationOf(@Nonnull Player player, @Nonnull CultivationLilyBlockEntity lily) {
        return Location.of(lily.getBlockPos(), player.level());
    }

    /**
     * Returns a per-side ticker for the block entity.
     * <p>
     * The server ticker drives periodic validity and cultivation-need recompute.
     * The client ticker drives the ambient magic-orb aura — emission is gated on the validity flag and a
     * cadence inside {@link CultivationLilyBlockEntity#clientTick}, so only live, valid lilies near a
     * player actually add particles.
     * createTickerHelper is the NeoForge/MC-provided helper on BaseEntityBlock that produces a type-safe
     * ticker and avoids unchecked-cast warnings.
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level,
                                                                  @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.CULTIVATION_LILY.get(),
                    CultivationLilyBlockEntity::clientTick);
        }
        return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.CULTIVATION_LILY.get(),
                CultivationLilyBlockEntity::serverTick);
    }

    /**
     * Schedules a prompt validity recompute when a neighboring block changes.
     * <p>
     * The updateShape override already handles the "water removed → block drops" case via canSurvive.
     * This override catches in-zone changes that are not direct neighbors of the lily (e.g. a
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
        if (level.getBlockEntity(pos) instanceof CultivationLilyBlockEntity lily) {
            lily.markValidityDirty();
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos,
                                      @Nonnull BlockState state) {
        return BlockEntityTypeRegistry.CULTIVATION_LILY.get().create(pos, state);
    }

}
