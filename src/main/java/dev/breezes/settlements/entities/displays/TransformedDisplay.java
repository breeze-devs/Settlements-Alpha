package dev.breezes.settlements.entities.displays;

import com.mojang.math.Transformation;
import dev.breezes.settlements.models.location.Location;
import lombok.Getter;
import net.minecraft.world.entity.Display;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * https://eszesbalint.github.io/bdstudio/editor
 */
@Getter
public abstract class TransformedDisplay {

    @Nonnull
    protected final Matrix4f transformationMatrix;
    @Nonnull
    protected final DisplayType displayType;

    @Nullable
    protected Display displayEntity;

    protected final boolean temporary;
    private boolean spawned;

    public TransformedDisplay(@Nonnull Matrix4f transformationMatrix, @Nonnull DisplayType displayType, boolean temporary) {
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

        try {
            MethodUtils.invokeMethod(this.displayEntity, "setTransformation", new Transformation(this.transformationMatrix));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        this.displayEntity.setTransformation(new Transformation(
//                transformationMatrix.getTranslation(new Vector3f()),
//                transformationMatrix.getUnnormalizedRotation(new Quaternionf()),
//                transformationMatrix.getScale(new Vector3f()),
//                new Quaternionf()  // Assuming no right rotation
//        ));

        this.spawned = true;

        // Add to removal list if temporary
        if (this.temporary) {
//            DisplayModuleController.TEMPORARY_DISPLAYS.add(this);
        }

        return this.displayEntity;
    }

    @Nonnull
    public abstract Display createEntity(@Nonnull Location location);

    public void remove() {
        if (this.displayEntity != null && !this.displayEntity.isAlive()) {
            this.displayEntity.discard();
        }

        this.spawned = false;
    }

    @Nonnull
    public abstract TransformedDisplay cloneWithoutEntity(boolean temporary);

    public enum DisplayType {
        BLOCK,
        ITEM
    }

}
