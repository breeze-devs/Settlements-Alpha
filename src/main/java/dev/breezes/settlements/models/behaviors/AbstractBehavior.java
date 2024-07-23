package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.misc.ITickable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractBehavior<T extends Entity & ISettlementsBrainEntity> implements IBehavior<T> {

    private final ILogger log;
    private final List<IEntityCondition<T>> preconditions;
    private final List<IEntityCondition<T>> continueConditions;
    private final ITickable preconditionCheckCooldown;
    private final ITickable behaviorCoolDown;
    private final ITickable behaviorTickCooldown;


    protected AbstractBehavior(@Nonnull ILogger log,
                               @Nonnull List<IEntityCondition<T>> preconditions,
                               @Nonnull List<IEntityCondition<T>> continueConditions,
                               @Nonnull ITickable preconditionCheckCooldown,
                               @Nonnull ITickable behaviorCoolDown,
                               @Nonnull ITickable behaviorTickCooldown) {
        this.log = log;
        this.preconditions = preconditions;
        this.continueConditions = continueConditions;
        this.preconditionCheckCooldown = preconditionCheckCooldown;
        this.behaviorCoolDown = behaviorCoolDown;
        this.behaviorTickCooldown = behaviorTickCooldown;
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
        log.debug("Behavior %s has started", this.getClass().getSimpleName());
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
