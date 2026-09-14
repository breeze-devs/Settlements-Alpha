package dev.breezes.settlements.infrastructure.minecraft.mixins;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker {

    @Invoker("setPierceLevel")
    void invokeSetPierceLevel(byte pierceLevel);

}
