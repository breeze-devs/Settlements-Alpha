package dev.breezes.settlements.infrastructure.minecraft.entities.displays;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.infrastructure.minecraft.mixins.DisplayEntityMixin;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.world.entity.Display;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * https://eszesbalint.github.io/bdstudio/editor
 */
@CustomLog
@Getter
public abstract class TransformedDisplay {

    @Nonnull
    protected final TransformationMatrix transformationMatrix;
    @Nonnull
    protected final DisplayType displayType;

    @Nullable
    protected Display displayEntity;

    private boolean spawned;

    public TransformedDisplay(@Nonnull TransformationMatrix transformationMatrix, @Nonnull DisplayType displayType) {
        this.transformationMatrix = transformationMatrix;
        this.displayType = displayType;
        this.spawned = false;
    }

    public Display spawn(@Nonnull Location location) {
        if (this.spawned) {
            throw new IllegalStateException("Tried to spawn a display entity that has already been spawned!");
        }

        this.displayEntity = this.createEntity(location);
        this.spawned = true;
        return this.displayEntity;
    }

    protected abstract Display createEntity(@Nonnull Location location);

    public void remove() {
        if (this.displayEntity != null) {
            this.displayEntity.discard();
        }
        this.spawned = false;
    }

    public abstract TransformedDisplay cloneWithoutEntity();

    public void setTransformation(@Nonnull TransformationMatrix newTransformation) {
        this.setTransformation(newTransformation, null);
    }

    public void setTransformation(@Nonnull TransformationMatrix newTransformation, @Nullable ClockTicks duration) {
        if (this.displayEntity == null) {
            log.info("TODO: change to entity-level logger"); // TODO: change to entity-level logger
            log.error("Cannot set transformation on a display that has not been spawned!");
            return;
        }

        DisplayEntityMixin display = (DisplayEntityMixin) this.displayEntity;
        if (duration != null) {
            display.invokeSetTransformationInterpolationDelay(0);
            display.invokeSetTransformationInterpolationDuration(duration.getTicksAsInt());
            display.setUpdateInterpolationDuration(true);
        }

        ((DisplayEntityMixin) this.displayEntity).invokeSetTransformation(newTransformation.getMinecraftTransformation());
    }


    public enum DisplayType {
        BLOCK,
        ITEM
    }

}
