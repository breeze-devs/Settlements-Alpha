package dev.breezes.settlements.infrastructure.minecraft.entities.displays;

import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.infrastructure.minecraft.mixins.DisplayEntityMixin;
import dev.breezes.settlements.infrastructure.minecraft.mixins.ItemDisplayMixin;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@CustomLog
public class TransformedItemDisplay extends TransformedDisplay {

    private ItemStack itemStack;

    @Builder
    public TransformedItemDisplay(@Nonnull ItemStack itemStack, @Nonnull TransformationMatrix transform) {
        super(transform, DisplayType.ITEM);
        this.itemStack = itemStack;
    }

    @Override
    protected Display.ItemDisplay createEntity(@Nonnull Location location) {
        Optional<Level> level = location.getLevel();
        if (level.isEmpty()) {
            throw new IllegalStateException("Cannot spawn item display when location has no level!");
        }

        Display.ItemDisplay itemDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level.get());
        // noinspection DataFlowIssue
        ((ItemDisplayMixin) itemDisplay).invokeSetItemStack(this.itemStack);
        // noinspection DataFlowIssue -- since ItemDisplay extends Display this will be fine
        ((DisplayEntityMixin) itemDisplay).invokeSetTransformation(this.transformationMatrix.getMinecraftTransformation());

        location.spawnEntity(itemDisplay);
        return itemDisplay;
    }

    @Override
    public TransformedDisplay cloneWithoutEntity() {
        return new TransformedItemDisplay(this.itemStack, this.transformationMatrix);
    }

    public void setItemStack(@Nonnull ItemStack itemStack) {
        this.itemStack = itemStack;
        if (this.displayEntity != null) {
            ((ItemDisplayMixin) this.displayEntity).invokeSetItemStack(itemStack);
        }
    }

}
