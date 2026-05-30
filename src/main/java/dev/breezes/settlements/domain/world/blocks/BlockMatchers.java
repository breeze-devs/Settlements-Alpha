package dev.breezes.settlements.domain.world.blocks;

import dev.breezes.settlements.domain.ai.conditions.IBlockCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;

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

}
