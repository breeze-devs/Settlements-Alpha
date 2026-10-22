package dev.breezes.settlements.domain.ai.perception;

import net.minecraft.world.entity.LivingEntity;

public record SensedEntity(LivingEntity entity, double distanceSquared) {

    public double distance() {
        return Math.sqrt(this.distanceSquared);
    }

}
