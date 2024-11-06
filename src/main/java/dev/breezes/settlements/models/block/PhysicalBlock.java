package dev.breezes.settlements.models.block;

import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
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
        return new PhysicalBlock(location, blockState);
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


}
