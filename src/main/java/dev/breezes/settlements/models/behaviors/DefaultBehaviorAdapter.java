package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

@CustomLog
public class DefaultBehaviorAdapter extends Behavior<Villager> {

    private static final Ticks DEFAULT_MAX_DURATION = Ticks.minutes(2);

    private final IBehavior<BaseVillager> wrappedBehavior;

    private long previousGameTime;

    private DefaultBehaviorAdapter(@Nonnull IBehavior<BaseVillager> wrappedBehavior, @Nonnull Ticks maxDuration) {
        super(Map.of(), 1, (int) maxDuration.getTicks());
        this.wrappedBehavior = wrappedBehavior;
        this.previousGameTime = -1;
    }

    public static DefaultBehaviorAdapter adapt(@Nonnull IBehavior<BaseVillager> wrappedBehavior) {
        return new DefaultBehaviorAdapter(wrappedBehavior, DEFAULT_MAX_DURATION);
    }

    public static DefaultBehaviorAdapter adapt(@Nonnull IBehavior<BaseVillager> wrappedBehavior, @Nonnull Ticks maxDuration) {
        return new DefaultBehaviorAdapter(wrappedBehavior, maxDuration);
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        // This method is called every tick in vanilla
        return this.wrappedBehavior.tickPreconditions(1, level, (BaseVillager) villager);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        log.behaviorTrace("Starting behavior because parent behavior is starting");
        this.previousGameTime = gameTime;
        this.wrappedBehavior.start(level, (BaseVillager) villager);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // Stop the wrapper behavior if the wrapped behavior is stopped
        if (this.wrappedBehavior.getStatus() == BehaviorStatus.STOPPED) {
            log.behaviorTrace("Stopping behavior because wrapped behavior is stopping");
            this.doStop(level, villager, gameTime);
            return;
        }

        this.wrappedBehavior.tick(this.calculateDeltaTicks(gameTime), level, (BaseVillager) villager);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (this.wrappedBehavior.getStatus() != BehaviorStatus.STOPPED) {
            log.behaviorTrace("Stopping behavior because parent behavior is stopping");
            this.wrappedBehavior.stop(level, (BaseVillager) villager);
        }
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // Always return true here since the wrapped behavior will handle its own stopping
        return true;
    }

    private int calculateDeltaTicks(long gameTime) {
        if (this.previousGameTime == -1) {
            this.previousGameTime = gameTime;
            return 1;
        }

        int delta = (int) (gameTime - this.previousGameTime);
        this.previousGameTime = gameTime;
        return Math.max(1, delta);
    }

}
