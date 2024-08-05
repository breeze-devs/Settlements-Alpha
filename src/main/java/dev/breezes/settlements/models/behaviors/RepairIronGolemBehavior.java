package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class RepairIronGolemBehavior extends AbstractBehavior<BaseVillager> {

//    private IActionPlan actionPlan = ActionPlan.builder()
//            .action(Actions.REPAIR_IRON_GOLEM)
//            .build();

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 1.5D;
    private static final double REPAIR_HP_PERCENTAGE = 0.75D;

    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;

    public RepairIronGolemBehavior() {
        super(log, Tickable.of(Ticks.fromSeconds(20)), RandomRangeTickable.of(Ticks.fromMinutes(1), Ticks.fromMinutes(2)), Tickable.of(Ticks.of(1)));

        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(10, 5, REPAIR_HP_PERCENTAGE);
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
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToRepair.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
//            if (!villager.getNavigationManager().isNavigating()) {
//                villager.getNavigationManager().navigateTo(this.targetToRepair.position(), villager.getSpeed(), NAVIGATE_STOP_DISTANCE);
//            }
        } else {
            // TODO: phase 2: assuming that we are next to the golem
            // TODO: play animation, particles, and sounds
            double healAmount = 4000; // TODO: calculate based on villager profession & expertise
            this.targetToRepair.heal((float) healAmount);
            villager.move(MoverType.SELF, new Vec3(0, 0.1D, 0));

            this.requestStop();
        }
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        this.targetToRepair = null;
    }

}
