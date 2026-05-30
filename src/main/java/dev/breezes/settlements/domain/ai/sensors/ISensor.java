package dev.breezes.settlements.domain.ai.sensors;

import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.time.ITickable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public interface ISensor<T extends Entity & ISettlementsBrainEntity> {

    /**
     * List of preconditions that must be met before the sensor can be executed
     */
    List<IEntityCondition<T>> getPreconditions();

    /**
     * Perform the sensing action or tick cooldowns
     * <p>
     * Should be called every delta ticks
     */
    void tick(int delta, @Nonnull Level world, @Nonnull T entity);

    /**
     * Perform the sensing action and return the result
     */
    List<MemoryWrite<?>> doSense(@Nonnull Level world, @Nonnull T entity);

    /**
     * Cooldown between each sense, can be set initially to delay the first sense
     */
    ITickable getSenseCooldown();

}
