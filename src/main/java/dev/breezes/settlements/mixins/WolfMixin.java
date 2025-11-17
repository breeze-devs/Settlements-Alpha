package dev.breezes.settlements.mixins;

import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(Wolf.class)
public interface WolfMixin {

    @Invoker("setCollarColor")
    void invokeSetCollarColor(@Nonnull DyeColor collarColor);

}
