package dev.breezes.settlements.mixins;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(Display.class)
public interface DisplayEntityMixin {

    @Accessor("updateInterpolationDuration")
    void setUpdateInterpolationDuration(boolean shouldUpdate);

    @Invoker("setTransformation")
    void invokeSetTransformation(@Nonnull Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void invokeSetTransformationInterpolationDuration(int duration);

    @Invoker("setTransformationInterpolationDelay")
    void invokeSetTransformationInterpolationDelay(int duration);

}
