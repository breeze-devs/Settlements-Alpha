package dev.breezes.settlements.models.blocks;

import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AllArgsConstructor
@ToString
@CustomLog
public class PhysicalBlock {

    private final Location location;
    @Getter
    private final BlockState blockState;

    public static PhysicalBlock of(@Nonnull Location location, @Nonnull BlockState blockState) {
        if (location.getLevel().isEmpty()) {
            throw new IllegalArgumentException("Location level is empty");
        }

        return new PhysicalBlock(location, blockState);
    }

    /*
     * Miscellaneous methods
     */
    public void update(int flags) {
        this.getLevel().setBlock(this.location.toBlockPos(), this.blockState, flags);
    }

    /*
     * Comparison methods
     */
    public boolean is(@Nullable Block block) {
        return block != null && this.blockState.is(block);
    }

    /*
     * Additional getters
     */
    public Location getLocation(boolean center) {
        return center ? this.location.center(true) : this.location.clone();
    }

    public Level getLevel() {
        return this.location.getLevel().get();
    }


}
