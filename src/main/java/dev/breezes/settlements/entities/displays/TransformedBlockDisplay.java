package dev.breezes.settlements.entities.displays;

import com.mojang.math.Transformation;
import dev.breezes.settlements.mixins.BlockDisplayMixin;
import dev.breezes.settlements.mixins.DisplayEntityMixin;
import dev.breezes.settlements.models.location.Location;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
public class TransformedBlockDisplay extends TransformedDisplay {

    @Nonnull
    private final BlockState blockState;

    @Builder
    public TransformedBlockDisplay(@Nonnull BlockState blockState, @Nonnull Matrix4f transform, boolean temporary) {
        super(transform, DisplayType.BLOCK, temporary);
        this.blockState = blockState;
    }

    @Override
    @Nonnull
    public Display.BlockDisplay createEntity(@Nonnull Location location) {
        Optional<Level> level = location.getLevel();
        if (level.isEmpty()) {
            throw new IllegalStateException("Cannot spawn block display when location has no level!");
        }

        Display.BlockDisplay blockDisplay = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level.get());
        ((BlockDisplayMixin) blockDisplay).invokeSetBlockState(this.blockState);
        ((DisplayEntityMixin) blockDisplay).invokeSetTransformation(new Transformation(this.transformationMatrix));

        level.get().addFreshEntity(blockDisplay);

        return blockDisplay;
    }

    @Nonnull
    @Override
    public TransformedDisplay cloneWithoutEntity(boolean temporary) {
        return new TransformedBlockDisplay(this.blockState, this.transformationMatrix, temporary);
    }

}
