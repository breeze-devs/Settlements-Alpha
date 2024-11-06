package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.client.ClientExecutor;
import dev.breezes.settlements.client.ClientUtil;
import dev.breezes.settlements.configurations.annotations.declarations.DoubleConfig;
import dev.breezes.settlements.configurations.annotations.declarations.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
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

    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 60, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 240, min = 1)
    private static int behaviorCooldownMax;

    @DoubleConfig(identifier = "repair_hp_percentage",
            description = "Health percentage threshold to consider the iron golem as damaged",
            defaultValue = 0.75, min = 0.0, max = 1.0)
    private static double repairHpPercentage;

    @IntegerConfig(identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to scan for iron golems to repair",
            defaultValue = 32, min = 5, max = 128)
    private static int scanRangeHorizontal;
    @IntegerConfig(identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to scan for iron golems to repair",
            defaultValue = 16, min = 1, max = 16)
    private static int scanRangeVertical;

    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;
    private int remainingRepairAttempts;

    public RepairIronGolemBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin), Ticks.seconds(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin), Ticks.seconds(behaviorCooldownMax)),
                Tickable.of(Ticks.seconds(2))); // repair every 2 seconds; TODO: this should be based on the animation duration

        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(scanRangeHorizontal, scanRangeVertical, repairHpPercentage);
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
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToRepair.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        // TODO: play animation, particles, and sounds
        double healAmount = RandomUtil.randomDouble(3, 8); // TODO: calculate based on villager profession & expertise
        this.targetToRepair.heal((float) healAmount);

        Location targetLocation = Location.fromEntity(this.targetToRepair, false);
        SoundRegistry.REPAIR_IRON_GOLEM.playGlobally(targetLocation, SoundSource.NEUTRAL);
        ParticleRegistry.repairIronGolem(targetLocation);
        log.behaviorTrace("Repaired iron golem for %.2f HP, %d attempts remaining", healAmount, this.remainingRepairAttempts - 1);

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
