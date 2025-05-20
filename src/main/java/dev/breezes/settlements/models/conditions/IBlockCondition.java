package dev.breezes.settlements.models.conditions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.parser.Entity;
import java.util.function.BiPredicate;

public interface IBlockCondition extends BiPredicate<BlockPos, Level> {
    @Override
    boolean test(@Nullable BlockPos blockPos, @Nullable Level level);
}
