package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.misc.ITickable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractBehavior<T extends Entity & ISettlementsBrainEntity> implements IBehavior<T> {

    private final List<IEntityCondition<T>> preconditions;

    protected AbstractBehavior(Logger log, List<IEntityCondition<T>> preconditions) {
        this.preconditions = preconditions;
    }


    @Override
    public List<IEntityCondition<T>> getPreconditions() {
        return this.preconditions;
    }

    @Override
    public List<IEntityCondition<T>> getContinueConditions() {
        return List.of();
    }

    @Override
    public ITickable getPreconditionCheckCooldown() {
        return null;
    }

    @Override
    public ITickable getBehaviorCoolDown() {
        return null;
    }

    @Override
    public void tick(int delta, @Nonnull Level world, @Nonnull T entity) {

    }

    @Override
    public void start(@Nonnull Level world, @Nonnull T entity) {
        // TODO: logging and/or preprocessing
        this.doStart(world, entity);
    }

    public abstract void doStart(@Nonnull Level world, @Nonnull T entity);

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull T entity) {

    }

    @Override
    public void stop(@Nonnull Level world, @Nonnull T entity) {
        // TODO: logging and/or preprocessing
        this.doStop(world, entity);
    }

    public abstract void doStop(@Nonnull Level world, @Nonnull T entity);

}
