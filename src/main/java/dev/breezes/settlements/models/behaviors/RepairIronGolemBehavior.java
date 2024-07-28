package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class RepairIronGolemBehavior extends AbstractBehavior<BaseVillager> {

//    private IActionPlan actionPlan = ActionPlan.builder()
//            .action(Actions.REPAIR_IRON_GOLEM)
//            .build();

    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;

    public RepairIronGolemBehavior() {
        super(log, Tickable.of(Ticks.of(10)), Tickable.of(Ticks.of(10)), Tickable.of(Ticks.of(10)));

        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(10, 5, 0.9D);
        this.preconditions.add(this.nearbyDamagedIronGolemExistsCondition);

        // Initialize variables
        this.targetToRepair = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.targetToRepair = this.nearbyDamagedIronGolemExistsCondition.getTargets().get(0);
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        if (this.targetToRepair == null) {
            this.requestStop();
            return;
        }

        // TODO: look at golem, hold iron ingot

        // TODO: phase 1: walk to golem

        // TODO: phase 2: assuming that we are next to the golem
        // TODO: play animation, particles, and sounds
        double healAmount = 4; // TODO: calculate based on villager profession & expertise
        this.targetToRepair.heal((float) healAmount);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        // TODO: remove held item
        this.targetToRepair = null;
    }

}
