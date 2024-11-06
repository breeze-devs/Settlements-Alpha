package dev.breezes.settlements.entities.displays;

import dev.breezes.settlements.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.mixins.BlockDisplayMixin;
import dev.breezes.settlements.mixins.DisplayEntityMixin;
import dev.breezes.settlements.models.location.Location;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@CustomLog
public class TransformedBlockDisplay extends TransformedDisplay {

    @Nonnull
    private final BlockState blockState;

    @Builder
    public TransformedBlockDisplay(@Nonnull BlockState blockState, @Nonnull TransformationMatrix transform, boolean temporary) {
        super(transform, DisplayType.BLOCK, temporary);
        this.blockState = blockState;
    }

    @Override
    @Nonnull
    protected Display.BlockDisplay createEntity(@Nonnull Location location) {
        Optional<Level> level = location.getLevel();
        if (level.isEmpty()) {
            throw new IllegalStateException("Cannot spawn block display when location has no level!");
        }

        Display.BlockDisplay blockDisplay = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level.get());
        ((BlockDisplayMixin) blockDisplay).invokeSetBlockState(this.blockState);
        // noinspection DataFlowIssue -- since BlockDisplay extends Display this will be fine
        ((DisplayEntityMixin) blockDisplay).invokeSetTransformation(this.transformationMatrix.getMinecraftTransformation());

        location.spawnEntity(blockDisplay);
        return blockDisplay;
    }

    @Nonnull
    @Override
    public TransformedDisplay cloneWithoutEntity(boolean temporary) {
        return new TransformedBlockDisplay(this.blockState, this.transformationMatrix, temporary);
    }

}
