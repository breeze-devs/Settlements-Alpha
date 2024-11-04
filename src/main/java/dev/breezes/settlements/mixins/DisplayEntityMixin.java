package dev.breezes.settlements.mixins;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

/*
 * TODO: https://www.fabricmc.net/wiki/tutorial:mixin_accessors
 */
@Mixin(Display.class)
public interface DisplayEntityMixin {

    @Invoker("setTransformation")
    void invokeSetTransformation(@Nonnull Transformation transformation);

}
