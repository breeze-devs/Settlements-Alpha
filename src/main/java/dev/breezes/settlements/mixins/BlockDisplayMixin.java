package dev.breezes.settlements.mixins;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayMixin {

    @Invoker("setBlockState")
    void invokeSetBlockState(@Nonnull BlockState blockState);

}
