package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.HurtBySensor;

import javax.annotation.Nonnull;

/**
 * Mod-wide panic gate. Behaves exactly like vanilla {@link HurtBySensor}, except a hit below
 * {@link BaseVillager#PANIC_DAMAGE_THRESHOLD} is treated as no damage: {@code HURT_BY} and
 * {@code HURT_BY_ENTITY} are cleared again, so neither the plan runner's safety check nor the
 * vanilla panic trigger (both of which read those memories) ever fires for a trivial hit.
 * <p>
 * Subclassing rather than reimplementing keeps us in lockstep with whatever vanilla does to populate
 * those memories; we only add the threshold veto on top. This lets effects like the cucco revenge
 * swarm chip negligible health without scaring the villager out of its behavior.
 */
public class SettlementsHurtBySensor extends HurtBySensor {

    @Override
    protected void doTick(@Nonnull ServerLevel level, @Nonnull LivingEntity entity) {
        super.doTick(level, entity);

        if (!(entity instanceof BaseVillager villager)) {
            return;
        }
        if (villager.getLastHurtAmount() >= BaseVillager.PANIC_DAMAGE_THRESHOLD) {
            return;
        }

        villager.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
        villager.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
    }

}
