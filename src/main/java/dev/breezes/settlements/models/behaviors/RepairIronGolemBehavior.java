package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class RepairIronGolemBehavior extends AbstractBehavior<BaseVillager> {

//    private IActionPlan actionPlan = ActionPlan.builder()
//            .action(Actions.REPAIR_IRON_GOLEM)
//            .build();

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 1.5D;

    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;

    public RepairIronGolemBehavior() {
        super(log, Tickable.of(Ticks.fromSeconds(20)), Tickable.of(Ticks.fromMinutes(1)), Tickable.of(Ticks.of(1)));

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
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.targetToRepair == null) {
            this.requestStop();
            return;
        }

        villager.setHeldItem(new ItemStack(Items.IRON_INGOT));
        boolean inRange = DistanceUtils.isWithinDistance(villager.position(), this.targetToRepair.position(), INTERACTION_DISTANCE);
        if (!inRange) {
            if (!villager.getNavigationManager().isNavigating()) {
                villager.getNavigationManager().navigateTo(this.targetToRepair.position(), villager.getSpeed(), NAVIGATE_STOP_DISTANCE);
            }
        } else {
            // TODO: phase 2: assuming that we are next to the golem
            // TODO: play animation, particles, and sounds
            double healAmount = 4; // TODO: calculate based on villager profession & expertise
            this.targetToRepair.heal((float) healAmount);

            this.requestStop();
        }
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        // TODO: remove held item
        this.targetToRepair = null;
    }

}
