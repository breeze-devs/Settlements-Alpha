package dev.breezes.settlements.block.crops;

import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IPlantable;

public class CornCropBlock extends CropBlock {

    // Maximum age of the bottom crop block
    public static final int BOTTOM_MAX_AGE = 7;
    // Maximum age of the top crop block
    public static final int TOP_MAX_AGE = 1;
    // Maximum age of the entire crop
    public static final int MAX_AGE = BOTTOM_MAX_AGE + TOP_MAX_AGE;

    public static final int GROWTH_LIGHT_REQUIREMENT = 9;

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, BOTTOM_MAX_AGE + TOP_MAX_AGE);

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D), // vanilla maximum age
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D) // age 8, same as previous, a full block
    };

    public CornCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ItemRegistry.CORN_SEEDS.get();
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int nextAge = this.getAge(state) + this.getBonemealAgeIncrease(level);
        nextAge = Math.min(nextAge, this.getMaxAge() - 1);

        // Check if we've reached the maximum age of the bottom crop block
        if (nextAge < BOTTOM_MAX_AGE) {
            // If not, grow the bottom crop block
            level.setBlock(pos, this.getStateForAge(nextAge), 2);
            return;
        }

        // We've reached the maximum age of the bottom crop block, so grow the top crop block
        if (!level.getBlockState(pos.above()).is(Blocks.AIR)) {
            // If the top crop block is not air, don't grow it
            return;
        }

        // Grow the top crop block
        level.setBlock(pos.above(), this.getStateForAge(nextAge), 2);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (super.canSurvive(state, level, pos)) {
            return true;
        }

        // Check if the block below is the bottom crop block at maximum age
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(AGE) == BOTTOM_MAX_AGE;
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction facing, IPlantable plantable) {
        // TODO: this essentially checks for the correct plant soil, e.g. cactus on sand
        // TODO: read documentation on this method
        return super.mayPlaceOn(state, world, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }

        // TODO: might refactor this to use a method or something
        if (level.getRawBrightness(pos, 0) < GROWTH_LIGHT_REQUIREMENT) {
            return;
        }

        // Check if the crop already matured
        int age = this.getAge(state);
        if (age >= this.getMaxAge()) {
            return;
        }

        // Check if we are allowed to grow by 1 stage (random tick + pre-grow event)
        float growthSpeed = getGrowthSpeed(this, level, pos);
        boolean randomTick = random.nextInt((int) (25.0F / growthSpeed) + 1) == 0;
        if (!ForgeHooks.onCropsGrowPre(level, pos, state, randomTick)) {
            return;
        }

        // Grow the crop by 1 stage
        // TODO: need to change if top crop has 2+ stages
        if (age == BOTTOM_MAX_AGE && level.getBlockState(pos.above()).is(Blocks.AIR)) {
            // Grow top crop block
            level.setBlock(pos.above(), this.getStateForAge(age + 1), 2);
        } else if (age < BOTTOM_MAX_AGE) {
            // Grow bottom crop block
            level.setBlock(pos, this.getStateForAge(age + 1), 2);
        } else {
            return;
        }

        // Call post-grow event
        ForgeHooks.onCropsGrowPost(level, pos, state);
    }

}
