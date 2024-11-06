package dev.breezes.settlements.entities.displays;

import dev.breezes.settlements.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.mixins.DisplayEntityMixin;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.util.Ticks;
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

    protected final boolean temporary;
    private boolean spawned;

    public TransformedDisplay(@Nonnull TransformationMatrix transformationMatrix, @Nonnull DisplayType displayType, boolean temporary) {
        this.transformationMatrix = transformationMatrix;
        this.displayType = displayType;
        this.temporary = temporary;

        this.spawned = false;
    }

    @Nonnull
    public Display spawn(@Nonnull Location location) {
        if (this.spawned) {
            throw new IllegalStateException("Tried to spawn a display entity that has already been spawned!");
        }

        this.displayEntity = this.createEntity(location);
        this.spawned = true;

        // TODO: Add to removal list if temporary
        if (this.temporary) {
//            DisplayModuleController.TEMPORARY_DISPLAYS.add(this);
        }

        return this.displayEntity;
    }

    @Nonnull
    protected abstract Display createEntity(@Nonnull Location location);

    public void remove() {
        if (this.displayEntity != null) {
            this.displayEntity.discard();
        }
        this.spawned = false;
    }

    @Nonnull
    public abstract TransformedDisplay cloneWithoutEntity(boolean temporary);

    public void setTransformation(@Nonnull TransformationMatrix newTransformation) {
        this.setTransformation(newTransformation, null);
    }

    public void setTransformation(@Nonnull TransformationMatrix newTransformation, @Nullable Ticks duration) {
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
