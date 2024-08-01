package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

@CustomLog
public class RepairIronGolemBehaviorAdapter extends Behavior<Villager> {

    private final RepairIronGolemBehavior wrappedBehavior;

    private long previousGameTime;

    public RepairIronGolemBehaviorAdapter() {
        super(Map.of(), 20, 100 * 20);
        this.wrappedBehavior = new RepairIronGolemBehavior();
        this.previousGameTime = 0;
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        // There's no game time passed into this method, so we'll default to 1
        return this.wrappedBehavior.tickPreconditions(1, level, (BaseVillager) villager);
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        log.debug("Starting behavior because parent behavior is starting");
        this.wrappedBehavior.start(level, (BaseVillager) villager);
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        this.wrappedBehavior.tick(1, level, (BaseVillager) villager);

        // Stop the wrapper behavior if the wrapped behavior is stopped
        if (this.wrappedBehavior.getStatus() == BehaviorStatus.STOPPED) {
            this.doStop(level, villager, gameTime);
        }
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        log.debug("Stopping behavior because parent behavior is stopping");
        this.wrappedBehavior.stop(level, (BaseVillager) villager);
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return this.wrappedBehavior.tickContinueConditions(this.calculateDeltaTicks(gameTime), level, (BaseVillager) villager);
    }

    private int calculateDeltaTicks(long gameTime) {
        if (this.previousGameTime == -1) {
            this.previousGameTime = gameTime;
            return 1;
        }

        int delta = (int) (gameTime - this.previousGameTime);
        this.previousGameTime = gameTime;
        return delta;
    }

}
