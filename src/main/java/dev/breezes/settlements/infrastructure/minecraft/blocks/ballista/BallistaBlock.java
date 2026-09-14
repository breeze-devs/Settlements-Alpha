package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A siege engine that occupies a single cell. The block is only the machine's base: everything drawn above and
 * around it belongs to the block entity's model, which overhangs the neighboring cells and collides with nothing.
 */
public class BallistaBlock extends BaseEntityBlock {

    public static final MapCodec<BallistaBlock> CODEC = simpleCodec(BallistaBlock::new);

    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

    /**
     * The redstone input as last seen.
     */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BallistaBlock(@Nonnull BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected MapCodec<BallistaBlock> codec() {
        return CODEC;
    }

    // The block entity renderer draws the whole machine; the block model only supplies the break-particle texture
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state,
                               @Nonnull BlockGetter level,
                               @Nonnull BlockPos pos,
                               @Nonnull CollisionContext context) {
        return SHAPE;
    }

    /**
     * Every use is handed to the ballista's block entity.
     */
    @Override
    protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack,
                                              @Nonnull BlockState state,
                                              @Nonnull Level level,
                                              @Nonnull BlockPos pos,
                                              @Nonnull Player player,
                                              @Nonnull InteractionHand hand,
                                              @Nonnull BlockHitResult hitResult) {
        // An empty-handed use arrives here with an empty stack before it would reach useWithoutItem
        if (!(level.getBlockEntity(pos) instanceof BallistaBlockEntity ballista)) {
            // Without its block entity there is no machine to operate, so vanilla's own handling of the use proceeds
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ballista.use(player, hand);
    }

    @Override
    public void setPlacedBy(@Nonnull Level level,
                            @Nonnull BlockPos pos,
                            @Nonnull BlockState state,
                            @Nullable LivingEntity placer,
                            @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BallistaBlockEntity ballista) {
            ballista.prepareForPlacement(placer);
        }
    }

    /**
     * A player breaking the ballista gets its ammunition back.
     */
    @Override
    public boolean onDestroyedByPlayer(@Nonnull BlockState state,
                                       @Nonnull Level level,
                                       @Nonnull BlockPos pos,
                                       @Nullable Player player,
                                       boolean willHarvest,
                                       @Nonnull FluidState fluid) {
        // willHarvest is false for a creative player and on a client
        ItemStack ammunition = willHarvest && level.getBlockEntity(pos) instanceof BallistaBlockEntity ballista
                ? ballista.seatedAmmunitionCopy()
                : ItemStack.EMPTY;

        boolean destroyed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (destroyed && !ammunition.isEmpty()) {
            Location.of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, level).dropItem(ammunition, true);
        }

        return destroyed;
    }

    /**
     * Records each change of redstone input, and hands the machine every rising edge.
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

        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }

        // The machine emits no signal, so its neighbors have nothing to recompute
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level.getBlockEntity(pos) instanceof BallistaBlockEntity ballista) {
            ballista.receiveRedstonePulse();
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level,
                                                                  @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.BALLISTA.get(),
                    BallistaBlockEntity::clientTick);
        }

        return createTickerHelper(blockEntityType, BlockEntityTypeRegistry.BALLISTA.get(),
                BallistaBlockEntity::serverTick);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos,
                                      @Nonnull BlockState state) {
        return BlockEntityTypeRegistry.BALLISTA.get().create(pos, state);
    }

}
