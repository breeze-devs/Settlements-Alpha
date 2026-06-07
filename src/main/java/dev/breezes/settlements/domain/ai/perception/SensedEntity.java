package dev.breezes.settlements.domain.ai.perception;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public record SensedEntity(@Nonnull LivingEntity entity, double distanceSquared) {

    public double distance() {
        return Math.sqrt(this.distanceSquared);
    }

}
