package dev.breezes.settlements.domain.world.blocks;

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

    public static final BlockMatcher HARVESTABLE_PUMPKIN = new BlockMatcher(
            state -> state.is(Blocks.PUMPKIN),
            (pos, view) -> Direction.Plane.HORIZONTAL.stream()
                    .anyMatch(direction -> view.getBlockState(pos.relative(direction)).is(Blocks.ATTACHED_PUMPKIN_STEM))
    );

    public static final BlockMatcher HARVESTABLE_MELON = new BlockMatcher(
            state -> state.is(Blocks.MELON),
            (pos, view) -> Direction.Plane.HORIZONTAL.stream()
                    .anyMatch(direction -> view.getBlockState(pos.relative(direction)).is(Blocks.ATTACHED_MELON_STEM))
    );

    public static final BlockMatcher RIPE_SWEET_BERRY_BUSH = new BlockMatcher(
            state -> state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE
    );

    public static final BlockMatcher RIPE_CROP = new BlockMatcher(
            state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
    );

    public static final BlockMatcher HARVESTABLE_NETHER_WART = new BlockMatcher(
            state -> state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE,
            (pos, view) -> view.getBlockState(pos.below()).is(Blocks.SOUL_SAND)
    );

    /**
     * The second-from-the-bottom sugarcane block is harvestable
     */
    public static final BlockMatcher HARVESTABLE_SUGARCANE = new BlockMatcher(
            state -> state.is(Blocks.SUGAR_CANE),
            (pos, view) -> view.getBlockState(pos).is(Blocks.SUGAR_CANE)
                    && view.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)
                    && !view.getBlockState(pos.below(2)).is(Blocks.SUGAR_CANE)
    );

    public static final BlockMatcher FULL_HIVE = new BlockMatcher(
            state -> state.is(BlockTags.BEEHIVES)
                    && state.getOptionalValue(BeehiveBlock.HONEY_LEVEL)
                    .map(honeyLevel -> honeyLevel == BeehiveBlock.MAX_HONEY_LEVELS)
                    .orElse(false)
    );

    public static final BlockMatcher HARVESTABLE_ORE = new BlockMatcher(
            state -> state.is(Tags.Blocks.ORES),
            (pos, view) -> Direction.stream()
                    .anyMatch(direction -> view.getBlockState(pos.relative(direction)).isAir())
    );

    public static final BlockMatcher LOOSE_GRAVEL = new BlockMatcher(
            state -> state.is(Blocks.GRAVEL),
            (pos, view) -> view.getBlockState(pos.above()).isAir()
    );

    public static final BlockMatcher LOOSE_SAND = new BlockMatcher(
            state -> state.is(Blocks.SAND) || state.is(Blocks.RED_SAND),
            (pos, view) -> view.getBlockState(pos.above()).isAir()
    );

}
