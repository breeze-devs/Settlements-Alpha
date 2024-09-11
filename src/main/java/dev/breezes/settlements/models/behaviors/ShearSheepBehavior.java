package dev.breezes.settlements.models.behaviors;

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

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;

    @Nullable
    private Sheep targetToShear;

    public ShearSheepBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(10), Ticks.seconds(20)),
                RandomRangeTickable.of(Ticks.seconds(30), Ticks.minutes(1)),
                Tickable.of(Ticks.one()));

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = new NearbyShearableSheepExistsCondition<>(30, 15);
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
