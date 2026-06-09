package dev.breezes.settlements.infrastructure.minecraft.mixins;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandMixin {

    @Invoker("setMarker")
    void invokeSetMarker(boolean marker);

}
