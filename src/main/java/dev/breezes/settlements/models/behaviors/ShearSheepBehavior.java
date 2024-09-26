package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.declarations.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyShearableSheepExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class ShearSheepBehavior extends AbstractInteractAtTargetBehavior {

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 2D;

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

    @IntegerConfig(identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to scan for nearby sheep to shear",
            defaultValue = 32, min = 5, max = 128)
    private static int scanRangeHorizontal;
    @IntegerConfig(identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to scan for nearby sheep to shear",
            defaultValue = 16, min = 1, max = 16)
    private static int scanRangeVertical;

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;

    @Nullable
    private Sheep targetToShear;

    public ShearSheepBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin), Ticks.seconds(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin), Ticks.seconds(behaviorCooldownMax)),
                Tickable.of(Ticks.one()));

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = new NearbyShearableSheepExistsCondition<>(scanRangeHorizontal, scanRangeVertical);
        this.preconditions.add(this.nearbyShearableSheepExistsCondition);

        // Initialize variables
        this.targetToShear = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.targetToShear = this.nearbyShearableSheepExistsCondition.getTargets().get(0);
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToShear.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        this.targetToShear.shear(SoundSource.NEUTRAL);
        log.info("Sheared sheep '%s'", this.targetToShear.getStringUUID());
        this.requestStop();
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        villager.setHeldItem(new ItemStack(Items.SHEARS));
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.targetToShear != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return DistanceUtils.isWithinDistance(villager.position(), this.targetToShear.position(), INTERACTION_DISTANCE);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        this.targetToShear = null;
    }

}
