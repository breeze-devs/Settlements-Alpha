package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.sensors.ISensor;
import dev.breezes.settlements.domain.time.ITickable;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
public abstract class AbstractSensor<T extends Entity & ISettlementsBrainEntity> implements ISensor<T> {

    protected final List<IEntityCondition<T>> preconditions;
    protected final ITickable senseCooldown;

    protected AbstractSensor(List<IEntityCondition<T>> preconditions, ITickable senseCooldown) {
        this.preconditions = preconditions;
        this.senseCooldown = senseCooldown;
    }

    @Override
    public void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        // Check cooldown
        if (!this.senseCooldown.tickCheckAndReset(delta)) {
            return;
        }

        for (IEntityCondition<T> precondition : this.preconditions) {
            if (!precondition.test(entity)) {
                return;
            }
        }

        IBrain brain = entity.getSettlementsBrain();
        this.doSense(world, entity).forEach(write -> write.applyTo(brain));
    }

}
