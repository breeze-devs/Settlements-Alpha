package dev.breezes.settlements.infrastructure.minecraft.mixins;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;

@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayMixin {

    @Invoker("setItemStack")
    void invokeSetItemStack(@Nonnull ItemStack itemStack);

}
