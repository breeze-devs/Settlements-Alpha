package dev.breezes.settlements.models.sensors;

import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.sensors.result.ISenseResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public interface ISensor<T extends Entity> {

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
    ISenseResult doSense(@Nonnull Level world, @Nonnull T entity);

    /**
     * Cooldown between each sense, can be set initially to delay the first sense
     */
    ITickable getSenseCooldown();

}
