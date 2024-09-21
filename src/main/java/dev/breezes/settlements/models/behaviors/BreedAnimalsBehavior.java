package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyBreedableAnimalPairExistsCondition;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.particles.ParticleRegistry;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.tags.EntityTag;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@CustomLog
public class BreedAnimalsBehavior extends AbstractInteractAtTargetBehavior {

    private static final ItemStack WHEAT = new ItemStack(Items.WHEAT);
    private static final ItemStack CARROT = new ItemStack(Items.CARROT);
    private static final ItemStack POTATO = new ItemStack(Items.POTATO);
    private static final ItemStack BEETROOT = new ItemStack(Items.BEETROOT);
    private static final ItemStack WHEAT_SEEDS = new ItemStack(Items.WHEAT_SEEDS);
    private static final ItemStack BEETROOT_SEEDS = new ItemStack(Items.BEETROOT_SEEDS);
    private static final ItemStack MELON_SEEDS = new ItemStack(Items.MELON_SEEDS);
    private static final ItemStack PUMPKIN_SEEDS = new ItemStack(Items.PUMPKIN_SEEDS);

    private static final Map<EntityType<? extends Animal>, ItemStack[]> BREED_ITEMS = Map.of(
            EntityType.COW, new ItemStack[]{WHEAT},
            EntityType.SHEEP, new ItemStack[]{WHEAT},
            EntityType.CHICKEN, new ItemStack[]{WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS},
            EntityType.PIG, new ItemStack[]{CARROT, POTATO, BEETROOT},
            EntityType.RABBIT, new ItemStack[]{CARROT}
    );

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 2D;

    private final Set<EntityType<? extends Animal>> breedableAnimalTypes;
    private final NearbyBreedableAnimalPairExistsCondition<BaseVillager> nearbyBreedableAnimalPairExistsCondition;

    private final ITickable waitForBreedingTickable;

    @Nonnull
    private BehaviorState behaviorState;
    @Nullable
    private ItemStack heldItem;
    @Nullable
    private Animal breedTarget1;
    @Nullable
    private Animal breedTarget2;

    public BreedAnimalsBehavior(Set<EntityType<? extends Animal>> breedableAnimalTypes) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(10), Ticks.seconds(20)),
                RandomRangeTickable.of(Ticks.seconds(30), Ticks.minutes(1)),
                Tickable.of(Ticks.one()));

        this.breedableAnimalTypes = breedableAnimalTypes;

        // Create behavior preconditions
        this.nearbyBreedableAnimalPairExistsCondition = new NearbyBreedableAnimalPairExistsCondition<>(30, 15, this.breedableAnimalTypes);
        this.preconditions.add(this.nearbyBreedableAnimalPairExistsCondition);
        this.waitForBreedingTickable = Tickable.of(Ticks.seconds(3));

        // Initialize variables
        this.behaviorState = BehaviorState.STANDBY;
        this.heldItem = null;
        this.breedTarget1 = null;
        this.breedTarget2 = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager villager) {
        Optional<NearbyBreedableAnimalPairExistsCondition.BreedablePair<?>> breedablePair = this.nearbyBreedableAnimalPairExistsCondition.getBreedablePair();
        if (breedablePair.isEmpty()) {
            log.warn("No breedable pair found, stopping behavior");
            this.requestStop();
            return;
        }

        this.behaviorState = BehaviorState.FEEDING_FIRST;
        this.breedTarget1 = breedablePair.get().getFirst();
        this.breedTarget2 = breedablePair.get().getSecond();
        this.waitForBreedingTickable.reset();

        // Set held item
        ItemStack[] breedItems = BREED_ITEMS.get(this.breedTarget1.getType());
        this.heldItem = RandomUtil.choice(breedItems);
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        Animal target = this.behaviorState == BehaviorState.FEEDING_FIRST ? this.breedTarget1 : this.breedTarget2;
        if (target == null) {
            log.behaviorWarn("Target is null");
            return;
        }

        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.position(), 0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.behaviorState == BehaviorState.WAITING_FOR_BREEDING) {
            return;
        }

        Animal target = this.getCurrentTarget();
        if (target == null) {
            log.behaviorWarn("Target is null");
            return;
        }
        log.behaviorStatus("Feeding animal in behavior stage '%s': '%s'".formatted(this.behaviorState.toString(), target.toString()));

        // Feed the animal
        target.setLeashedTo(villager, true);

        // Display effects
        ParticleRegistry.breedHearts(((ServerLevel) world), target.getX(), target.getEyeY(), target.getZ());
        ParticleRegistry.itemBreak(((ServerLevel) world), target.getX(), target.getEyeY(), target.getZ(), this.heldItem);
        SoundRegistry.GENERIC_EAT.playGlobally(world, target.getX(), target.getY(), target.getZ(), SoundSource.NEUTRAL);

        // Update behavior state
        if (this.behaviorState == BehaviorState.FEEDING_FIRST) {
            log.behaviorStatus("Fed first animal");
            this.behaviorState = BehaviorState.FEEDING_SECOND;
        } else {
            log.behaviorStatus("Fed second animal");
            this.behaviorState = BehaviorState.WAITING_FOR_BREEDING;

            this.breedTarget1.setInLove(null);
            this.breedTarget2.setInLove(null);
            villager.clearHeldItem();
        }
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        if (this.behaviorState == BehaviorState.FEEDING_FIRST || this.behaviorState == BehaviorState.FEEDING_SECOND) {
            // Set held item
            if (this.heldItem == null) {
                log.behaviorWarn("Held item is null");
                return;
            }
            villager.setHeldItem(this.heldItem);
        } else if (this.behaviorState == BehaviorState.WAITING_FOR_BREEDING) {
            if (this.breedTarget1.getNavigation().isDone()) {
                this.breedTarget1.getNavigation().moveTo(this.breedTarget2, 1.0D);
            }
            if (this.breedTarget2.getNavigation().isDone()) {
                this.breedTarget2.getNavigation().moveTo(this.breedTarget1, 1.0D);
            }

            if (villager.getNavigationManager().isNavigating()) {
                villager.getNavigationManager().stop();
            }

            if (this.waitForBreedingTickable.tickAndCheck(delta)) {
                this.requestStop();
            }
        }
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.breedTarget1 != null && this.breedTarget2 != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        Animal target = this.getCurrentTarget();
        if (target == null) {
            return false;
        }
        return DistanceUtils.isWithinDistance(villager.position(), target.position(), INTERACTION_DISTANCE);
    }

    @Nullable
    private Animal getCurrentTarget() {
        return this.behaviorState == BehaviorState.FEEDING_FIRST ? this.breedTarget1 : this.breedTarget2;
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        // Scan for baby animals nearby to tag as village-owned
        this.claimNearbyBabyAnimals(villager, this.breedTarget1.getType());

        // Stop breeding for both animals
        for (Animal target : new Animal[]{this.breedTarget1, this.breedTarget2}) {
            if (target == null) {
                continue;
            }
            target.dropLeash(true, false);
            target.setAge(6000); // reset breeding cooldown
        }

        this.waitForBreedingTickable.reset();
        this.behaviorState = BehaviorState.STANDBY;
        this.heldItem = null;
        this.breedTarget1 = null;
        this.breedTarget2 = null;
    }

    private void claimNearbyBabyAnimals(@Nonnull BaseVillager villager, @Nonnull EntityType<?> type) {
        AABB scanBoundary = villager.getBoundingBox().inflate(6, 6, 6);
        Predicate<Entity> isBabyOfRightType = (targetEntity) -> targetEntity.getType() == type && ((Animal) targetEntity).isBaby();
        List<Entity> nearbyEntities = villager.level().getEntities(villager, scanBoundary, isBabyOfRightType);
        for (Entity nearbyEntity : nearbyEntities) {
            log.behaviorStatus("Claiming baby animal '%s'".formatted(nearbyEntity.toString()));
            nearbyEntity.addTag(EntityTag.VILLAGE_OWNED_ANIMAL.getTag());
        }
    }

    private enum BehaviorState {

        /**
         * Not actively breeding
         */
        STANDBY,

        /**
         * Feeding the first animal
         */
        FEEDING_FIRST,

        /**
         * Feeding the second animal
         */
        FEEDING_SECOND,

        /**
         * Idle time to wait until breeding completes
         */
        WAITING_FOR_BREEDING;

    }

}
