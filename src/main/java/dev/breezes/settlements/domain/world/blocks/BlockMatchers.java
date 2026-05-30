package dev.breezes.settlements.domain.world.blocks;

import dev.breezes.settlements.domain.ai.conditions.IBlockCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.neoforged.neoforge.common.Tags;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockMatchers {

    private static final IBlockCondition ALWAYS_TRUE = (ignored, ignored2) -> true;

    public static final BlockMatcher HARVESTABLE_PUMPKIN = new BlockMatcher(
            state -> state.is(Blocks.PUMPKIN),
            (pos, level) -> Direction.Plane.HORIZONTAL.stream()
                    .anyMatch(direction -> level.getBlockState(pos.relative(direction)).is(Blocks.ATTACHED_PUMPKIN_STEM))
    );

    public static final BlockMatcher HARVESTABLE_MELON = new BlockMatcher(
            state -> state.is(Blocks.MELON),
            (pos, level) -> Direction.Plane.HORIZONTAL.stream()
                    .anyMatch(direction -> level.getBlockState(pos.relative(direction)).is(Blocks.ATTACHED_MELON_STEM))
    );

    public static final BlockMatcher RIPE_SWEET_BERRY_BUSH = new BlockMatcher(
            state -> state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE,
            ALWAYS_TRUE
    );

    public static final BlockMatcher RIPE_CROP = new BlockMatcher(
            state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state),
            ALWAYS_TRUE
    );

    public static final BlockMatcher HARVESTABLE_NETHER_WART = new BlockMatcher(
            state -> state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE,
            (pos, level) -> level.getBlockState(pos.below()).is(Blocks.SOUL_SAND)
    );

    /**
     * The second-from-the-bottom sugarcane block is harvestable
     */
    public static final BlockMatcher HARVESTABLE_SUGARCANE = new BlockMatcher(
            state -> state.is(Blocks.SUGAR_CANE),
            (pos, level) -> level.getBlockState(pos).is(Blocks.SUGAR_CANE)
                    && level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)
                    && !level.getBlockState(pos.below(2)).is(Blocks.SUGAR_CANE)
    );

    public static final BlockMatcher FULL_HIVE = new BlockMatcher(
            state -> state.is(BlockTags.BEEHIVES)
                    && state.getOptionalValue(BeehiveBlock.HONEY_LEVEL)
                    .map(honeyLevel -> honeyLevel == BeehiveBlock.MAX_HONEY_LEVELS)
                    .orElse(false),
            ALWAYS_TRUE
    );

    public static final BlockMatcher HARVESTABLE_ORE = new BlockMatcher(
            state -> state.is(Tags.Blocks.ORES),
            (pos, level) -> Direction.stream()
                    .anyMatch(direction -> level.getBlockState(pos.relative(direction)).isAir())
    );

}
