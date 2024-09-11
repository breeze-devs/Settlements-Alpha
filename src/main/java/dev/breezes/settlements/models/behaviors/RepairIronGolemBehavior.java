package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.client.ClientExecutor;
import dev.breezes.settlements.client.ClientUtil;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.particles.ParticleRegistry;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
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
    private static final double INTERACTION_DISTANCE = 2D;
    private static final double REPAIR_HP_PERCENTAGE = 0.75D;

    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;
    private int remainingRepairAttempts;

    public RepairIronGolemBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(10), Ticks.seconds(20)),
                RandomRangeTickable.of(Ticks.seconds(30), Ticks.minutes(1)), // TODO: move to config
                Tickable.of(Ticks.seconds(2))); // repair every 3 seconds; TODO: this should be based on the animation duration


        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(30, 15, REPAIR_HP_PERCENTAGE);
        this.preconditions.add(this.nearbyDamagedIronGolemExistsCondition);

        // Initialize variables
        this.targetToRepair = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.targetToRepair = this.nearbyDamagedIronGolemExistsCondition.getTargets().get(0);
        this.remainingRepairAttempts = RandomUtil.randomInt(2, 5, true);
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToRepair.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        // TODO: play animation, particles, and sounds
        double healAmount = RandomUtil.randomDouble(3, 8); // TODO: calculate based on villager profession & expertise
        this.targetToRepair.heal((float) healAmount);

        SoundRegistry.REPAIR_IRON_GOLEM.playGlobally(level, villager.getX(), villager.getY(), villager.getZ(), SoundSource.NEUTRAL);
        ParticleRegistry.repairIronGolem((ServerLevel) level, this.targetToRepair.getX(), this.targetToRepair.getY(), this.targetToRepair.getZ());
        log.behaviorStatus("Repaired iron golem for %.2f HP, %d attempts remaining", healAmount, this.remainingRepairAttempts - 1);

        ClientExecutor.runOnClient(() -> {
            ClientUtil.getClientSideVillager(villager)
                    .ifPresent(clientVillager -> clientVillager.getSpinAnimator().playOnce());
        });

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
        return this.targetToRepair != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return DistanceUtils.isWithinDistance(villager.position(), this.targetToRepair.position(), INTERACTION_DISTANCE);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        this.targetToRepair = null;
    }

}
