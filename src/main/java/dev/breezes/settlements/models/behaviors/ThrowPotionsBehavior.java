package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.annotations.configurations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyFriendlyNeedsPotionCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.location.Vector;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public class ThrowPotionsBehavior extends AbstractInteractAtTargetBehavior {

    private static final int NAVIGATE_STOP_DISTANCE = 3;
    private static final double INTERACTION_DISTANCE = 4;

    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 5, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 30, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 120, min = 1)
    private static int behaviorCooldownMax;

    @IntegerConfig(identifier = "minimum_player_reputation",
            description = "Minimum reputation level required to throw a potion at a player",
            defaultValue = 20, min = -100, max = 100)
    private static int minimumPlayerReputation;

    @IntegerConfig(identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to search for entities that need a potion",
            defaultValue = 16, min = 5, max = 128)
    private static int scanRangeHorizontal;
    @IntegerConfig(identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to search for entities that need a potion",
            defaultValue = 8, min = 1, max = 16)
    private static int scanRangeVertical;

    private final NearbyFriendlyNeedsPotionCondition<BaseVillager> nearbyFriendlyNeedsPotionCondition;

    @Nullable
    private LivingEntity targetToThrow;
    @Nullable
    private ItemStack potionToThrow;

    public ThrowPotionsBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin), Ticks.seconds(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin), Ticks.seconds(behaviorCooldownMax)),
                Tickable.of(Ticks.one())); // TODO: this should be based on the animation duration

        // Preconditions to this behavior
        this.nearbyFriendlyNeedsPotionCondition = new NearbyFriendlyNeedsPotionCondition<>(scanRangeHorizontal, scanRangeVertical, minimumPlayerReputation);
        this.preconditions.add(this.nearbyFriendlyNeedsPotionCondition);

        // Initialize variables
        this.targetToThrow = null;
        this.potionToThrow = null;
    }

    public void doStart(@Nonnull Level world, @Nonnull BaseVillager villager) {
        double currentHpPercentage = Double.MAX_VALUE;
        for (Entity entity : this.nearbyFriendlyNeedsPotionCondition.getFriendlyNeedsPotionMap().keySet()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            double hp = livingEntity.getHealth() / livingEntity.getMaxHealth();
            if (hp < currentHpPercentage) {
                this.targetToThrow = livingEntity;
                currentHpPercentage = hp;
            }
        }

        if (this.targetToThrow == null) {
            log.behaviorStatus("No target found to throw potion at");
            this.requestStop();
            return;
        }

        Holder<Potion> potionNeeded = this.nearbyFriendlyNeedsPotionCondition.getFriendlyNeedsPotionMap().get(this.targetToThrow).getPotion();
        this.potionToThrow = PotionContents.createItemStack(Items.SPLASH_POTION, potionNeeded);
        villager.setHeldItem(potionToThrow);
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToThrow.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        Location location = Location.fromEntity(villager, true);
        SoundRegistry.THROW_POTION.playGlobally(location, SoundSource.NEUTRAL);

        ThrownPotion potionEntity = new ThrownPotion(world, villager);
        potionEntity.setItem(potionToThrow);
        potionEntity.setXRot(potionEntity.getXRot() + 20.0F);

        Vector direction = villager.getLocation().getDirectionTo(Location.fromEntity(targetToThrow, true));
        potionEntity.shoot(direction.getX(), direction.getY(), direction.getZ(), 1, 4.0F);

        villager.level().addFreshEntity(potionEntity);
        villager.clearHeldItem();
        this.requestStop();
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.targetToThrow != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.targetToThrow != null && DistanceUtils.isWithinDistance(villager.position(), this.targetToThrow.position(), INTERACTION_DISTANCE);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.targetToThrow = null;
        this.potionToThrow = null;
    }

}
