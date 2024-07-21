package dev.breezes.settlements.models.sensors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.sensors.result.ISenseResult;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
public abstract class AbstractSensor<T extends Entity & ISettlementsBrainEntity> implements ISensor<T> {

    protected final List<IEntityCondition<T>> preconditions;
    protected final ITickable senseCooldown;
    protected final T entity;

    protected AbstractSensor(List<IEntityCondition<T>> preconditions, ITickable senseCooldown, T entity) {
        this.preconditions = preconditions;
        this.senseCooldown = senseCooldown;
        this.entity = entity;
    }

    @Override
    public void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        // Check cooldown
        if (!this.senseCooldown.tickCheckAndReset(delta)) {
            return;
        }

        // Check preconditions
        for (IEntityCondition<T> precondition : this.preconditions) {
            if (!precondition.test(this.entity)) {
                return;
            }
        }

        // Perform sensing action
        ISenseResult senseResult = this.doSense(world, entity);

        // Handle the result
        this.handleSenseResult(senseResult);
    }

    /**
     * Default behavior is to overwrite the memory with the new memories or wipe the memory if no sense result of that type is present
     * <p>
     * This can be overridden to implement custom behavior
     */
    protected void handleSenseResult(ISenseResult senseResult) {
        senseResult.saveToMemory(this.entity.getSettlementsBrain());
    }

}
