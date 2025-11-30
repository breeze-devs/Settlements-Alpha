package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.particles.ParticleRegistry;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class RepairIronGolemBehavior extends AbstractInteractAtTargetBehavior {

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 2.0;

    private final RepairIronGolemConfig config;
    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;
    private int remainingRepairAttempts;

    public RepairIronGolemBehavior(RepairIronGolemConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())),
                Tickable.of(Ticks.seconds(2))); // repair every 2 seconds; TODO: this should be based on the animation duration
        this.config = config;

        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.repairHpPercentage());
        this.preconditions.add(this.nearbyDamagedIronGolemExistsCondition);

        // Initialize variables
        this.targetToRepair = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.targetToRepair = this.nearbyDamagedIronGolemExistsCondition.getTargets().get(0);
        this.remainingRepairAttempts = RandomUtil.randomInt(1, 3, true); // TODO: this could be based on inventory, e.g. iron ingot count
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().walkTo(Location.fromEntity(this.targetToRepair, false), NAVIGATE_STOP_DISTANCE);
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        // TODO: play animation, particles, and sounds
        double healAmount = RandomUtil.randomDouble(3, 8); // TODO: calculate based on villager profession & expertise
        this.targetToRepair.heal((float) healAmount);

        Location targetLocation = Location.fromEntity(this.targetToRepair, false);
        SoundRegistry.REPAIR_IRON_GOLEM.playGlobally(targetLocation, SoundSource.NEUTRAL);
        ParticleRegistry.repairIronGolem(targetLocation);
        log.behaviorTrace("Repaired iron golem for {} HP, {} attempts remaining", healAmount, this.remainingRepairAttempts - 1);

        if (--this.remainingRepairAttempts <= 0) {
            this.requestStop();
        }
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        villager.setHeldItem(new ItemStack(Items.IRON_INGOT));
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.targetToRepair != null
                && this.targetToRepair.isAlive()
                && this.targetToRepair.getHealth() < this.targetToRepair.getMaxHealth() * this.config.repairHpPercentage();
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return DistanceUtils.isWithinDistance(villager.position(), this.targetToRepair.position(), INTERACTION_DISTANCE);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.targetToRepair = null;
    }

}
