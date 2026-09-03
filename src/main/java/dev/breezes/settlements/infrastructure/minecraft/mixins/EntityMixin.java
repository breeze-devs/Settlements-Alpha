package dev.breezes.settlements.infrastructure.minecraft.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityMixin {

    @Invoker("isInRain")
    boolean invokeIsInRain();

}
