package dev.breezes.settlements.models.behaviors;

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

    private final ThrowPotionsConfig config;
    private final NearbyFriendlyNeedsPotionCondition<BaseVillager> nearbyFriendlyNeedsPotionCondition;

    @Nullable
    private LivingEntity targetToThrow;
    @Nullable
    private ItemStack potionToThrow;

    public ThrowPotionsBehavior(ThrowPotionsConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())),
                Tickable.of(Ticks.one())); // TODO: this should be based on the animation duration
        this.config = config;

        // Preconditions to this behavior
        this.nearbyFriendlyNeedsPotionCondition = new NearbyFriendlyNeedsPotionCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.minimumPlayerReputation());
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
