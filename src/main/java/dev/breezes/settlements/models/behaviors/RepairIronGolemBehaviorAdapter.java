package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;

import javax.annotation.Nonnull;
import java.util.Map;

public class RepairIronGolemBehaviorAdapter extends Behavior<BaseVillager> {

    private final RepairIronGolemBehavior wrappedBehavior;

    public RepairIronGolemBehaviorAdapter() {
        super(Map.of(), 20, 10 * 20);
        this.wrappedBehavior = new RepairIronGolemBehavior();
    }

    @Override
    protected void start(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long pGameTime) {

    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long pGameTime) {
        this.wrappedBehavior.tick(1, level, villager);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long pGameTime) {
        super.stop(level, villager, pGameTime);
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long pGameTime) {
        return true;
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // TODO: implement this
//        return this.wrappedBehavior.tick(1, level, villager);
        return false;
    }
}
