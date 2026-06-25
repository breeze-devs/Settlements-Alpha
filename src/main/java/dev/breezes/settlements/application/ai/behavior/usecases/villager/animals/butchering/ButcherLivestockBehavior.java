package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.ChopAnimations;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.tags.EntityTag;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@CustomLog
public class ButcherLivestockBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation IRON_AXE_ID = ResourceLocation.withDefaultNamespace("iron_axe");

    private enum ButcherStage implements StageKey {
        BUTCHER_TARGET,
        COLLECT_DROPS,
        END;
    }

    public static final String RESERVED_FOR_VILLAGER_KEY = "settlements_reserved_for_villager";

    private final ButcherLivestockConfig config;
    private final Map<EntityType<?>, Integer> minimumKeepByType;
    private final boolean requireVillageOwnedTag;
    private int butcherCountRemaining;

    @Nullable
    private EntityType<?> selectedAnimalType;
    @Nullable
    private LivingEntity target;
    private boolean shouldRewardExperience;

    public ButcherLivestockBehavior(@Nonnull ButcherLivestockConfig config,
                                    @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.config = config;
        this.minimumKeepByType = parseConfiguredMinimumKeep(config.minimumKeepCount());
        this.requireVillageOwnedTag = config.requireVillageOwnedTag();

        // Loose precondition: at least one entity of a configured type exists and is an adult.
        // The strict minimum-keep population check happens inside onBehaviorStart.
        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, LivingEntity>builder()
                .entityType(LivingEntity.class)
                .filter((villager, entity) -> this.minimumKeepByType.containsKey(entity.getType())
                        && isAdultOrNonAgeable(entity)
                        && (!this.requireVillageOwnedTag || entity.getTags().contains(EntityTag.VILLAGE_OWNED_ANIMAL.getTag())))
                .completionRange(1)
                .build());
        this.preconditions.add(support.getDemandSignalService().requireItem(new ItemMatch.ItemRef(IRON_AXE_ID), 1, 50, this.getClass().getSimpleName()));

        this.butcherCountRemaining = 0;
        this.selectedAnimalType = null;
        this.target = null;
        this.shouldRewardExperience = false;

        this.initializeStateMachine(this.createControlStep(), ButcherStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ButcherLivestockBehavior")
                .initialStage(ButcherStage.BUTCHER_TARGET)
                .stageStepMap(Map.of(
                        ButcherStage.BUTCHER_TARGET, this.createButcherStep(),
                        ButcherStage.COLLECT_DROPS, this.createCollectDropsStep()))
                .nextStage(ButcherStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createButcherStep() {
        TimeBasedStep<BaseVillager> butcherStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(ChopAnimations.CHOP_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator();
                    villager.setHeldItem(Items.IRON_AXE.getDefaultInstance());

                    // Start the swing before the action frame so the gameplay effect lands on the visual impact.
                    context.getInitiator().triggerMotion(AnimationArchetype.SWING_HEAVY);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(ChopAnimations.CHOP_IMPACT_TICKS), context -> {
                    if (this.target == null || !this.target.isAlive()) {
                        return StepResult.complete();
                    }

                    this.performButcher(this.target, context.getInitiator().getMinecraftEntity());
                    this.butcherCountRemaining--;
                    this.shouldRewardExperience = true;

                    // Record each successfully killed entity toward the deed total
                    context.primaryDeed()
                            .ifPresent(outcome -> outcome.recordYield(1));

                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return StepResult.transition(ButcherStage.COLLECT_DROPS);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(butcherStep)
                .build();
    }

    private BehaviorStep<BaseVillager> createCollectDropsStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(PickUpAnimations.PICK_UP_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    context.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(PickUpAnimations.PICK_UP_AT_TICK), context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();

                    List<ItemEntity> drops = this.findReservedDropsNearVillager(villager);
                    drops.forEach(itemEntity -> context.getInitiator().pickUp(itemEntity));
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();

                    if (this.butcherCountRemaining <= 0 || this.selectedAnimalType == null) {
                        return StepResult.complete();
                    }

                    Optional<LivingEntity> nextTarget = this.findNextTargetForType(villager, this.selectedAnimalType);
                    if (nextTarget.isEmpty()) {
                        return StepResult.complete();
                    }

                    this.target = nextTarget.get();
                    context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    return StepResult.transition(ButcherStage.BUTCHER_TARGET);
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        Expertise expertise = entity.getExpertise();
        int limit = this.config.expertiseButcherLimit().getOrDefault(expertise.getConfigName(), 1);
        this.butcherCountRemaining = limit;
        this.shouldRewardExperience = false;

        // findButcherableTarget also sets this.selectedAnimalType as a side effect
        Optional<LivingEntity> selectedTarget = this.findButcherableTarget(entity);
        if (selectedTarget.isEmpty()) {
            this.requestStop("No butcherable livestock found");
            return;
        }

        this.target = selectedTarget.get();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.LIVESTOCK_BUTCHERED, resolveSpeciesNoun(this.selectedAnimalType)));
        log.behaviorStatus("Villager is '{}' level, maximum butcher count is {}, targeting {}",
                expertise.toString(), limit, this.selectedAnimalType);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.target != null && this.selectedAnimalType != null;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.target = null;
        this.selectedAnimalType = null;
        this.butcherCountRemaining = 0;
        this.shouldRewardExperience = false;
    }

    /**
     * Scans perceived entities for the first type whose adult population exceeds the configured minimum keep.
     * Sets {@link #selectedAnimalType} as a side effect when a valid target is found.
     */
    private Optional<LivingEntity> findButcherableTarget(@Nonnull BaseVillager villager) {
        List<LivingEntity> candidates = this.getPerceivedEntities(villager)
                .ofType(LivingEntity.class, this::isButcherCandidate)
                .toList();

        Map<EntityType<?>, Integer> adultCountByType = new HashMap<>();
        for (LivingEntity entity : candidates) {
            EntityType<?> type = entity.getType();
            adultCountByType.merge(type, 1, Integer::sum);
        }

        List<? extends EntityType<?>> eligibleTypesByPopulation = adultCountByType.entrySet().stream()
                .filter(entry -> entry.getValue() > this.minimumKeepByType.getOrDefault(entry.getKey(), Integer.MAX_VALUE))
                .sorted(Map.Entry.<EntityType<?>, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString()))
                .map(Map.Entry::getKey)
                .toList();

        for (EntityType<?> type : eligibleTypesByPopulation) {
            Optional<LivingEntity> reachableTarget = candidates.stream()
                    .filter(entity -> entity.getType() == type)
                    .filter(entity -> this.canReachButcherTarget(villager, entity))
                    .min(Comparator.comparingDouble(villager::distanceToSqr));
            if (reachableTarget.isPresent()) {
                this.selectedAnimalType = type;
                return reachableTarget;
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the next butcherable entity of the given type, used for chaining kills within one session.
     */
    private Optional<LivingEntity> findNextTargetForType(@Nonnull BaseVillager villager, @Nonnull EntityType<?> desiredType) {
        List<LivingEntity> candidates = this.getPerceivedEntities(villager)
                .ofType(LivingEntity.class, entity -> entity.getType() == desiredType
                        && this.isButcherCandidate(entity))
                .toList();

        int minKeep = this.minimumKeepByType.getOrDefault(desiredType, Integer.MAX_VALUE);
        if (candidates.size() <= minKeep) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(entity -> this.canReachButcherTarget(villager, entity))
                .min(Comparator.comparingDouble(villager::distanceToSqr));
    }

    private boolean canReachButcherTarget(@Nonnull BaseVillager villager, @Nonnull LivingEntity entity) {
        return villager.getNavigationManager().canReach(Location.fromEntity(entity, false), 1);
    }

    private boolean isButcherCandidate(@Nonnull LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (!this.minimumKeepByType.containsKey(entity.getType())) {
            return false;
        }
        if (!isAdultOrNonAgeable(entity)) {
            return false;
        }
        if (this.requireVillageOwnedTag && !entity.getTags().contains(EntityTag.VILLAGE_OWNED_ANIMAL.getTag())) {
            return false;
        }
        return true;
    }

    private void performButcher(@Nonnull LivingEntity target,
                                @Nonnull BaseVillager villager) {
        target.getPersistentData().putUUID(RESERVED_FOR_VILLAGER_KEY, villager.getUUID());

        target.hurt(villager.damageSources().mobAttack(villager), Float.MAX_VALUE);
        if (target.isAlive()) {
            target.kill();
        }
    }

    private List<ItemEntity> findReservedDropsNearVillager(@Nonnull BaseVillager villager) {
        AABB area = villager.getBoundingBox().inflate(6, 3, 6);
        Predicate<ItemEntity> isReservedForVillager = itemEntity -> itemEntity != null
                && itemEntity.isAlive()
                && itemEntity.getPersistentData().hasUUID(RESERVED_FOR_VILLAGER_KEY)
                && villager.getUUID().equals(itemEntity.getPersistentData().getUUID(RESERVED_FOR_VILLAGER_KEY));

        return villager.level().getEntitiesOfClass(ItemEntity.class, area, isReservedForVillager);
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

    /**
     * Entities extending AgeableMob are only butcherable as adults; all others (e.g. modded mobs
     * that do not model an age lifecycle) are eligible unconditionally.
     */
    private static boolean isAdultOrNonAgeable(@Nonnull LivingEntity entity) {
        if (entity instanceof AgeableMob mob) {
            return !mob.isBaby();
        }
        return true;
    }

    private static String resolveSpeciesNoun(@Nullable EntityType<?> type) {
        if (type == null) {
            return "animals";
        }
        if (type == EntityType.COW) {
            return "cows";
        } else if (type == EntityType.PIG) {
            return "pigs";
        } else if (type == EntityType.SHEEP) {
            return "sheep";
        } else if (type == EntityType.CHICKEN) {
            return "chickens";
        } else if (type == EntityType.RABBIT) {
            return "rabbits";
        }
        return type.getDescriptionId();
    }

    private static Map<EntityType<?>, Integer> parseConfiguredMinimumKeep(@Nonnull Map<String, Integer> rawMap) {
        Map<EntityType<?>, Integer> parsed = new HashMap<>();

        for (Map.Entry<String, Integer> entry : rawMap.entrySet()) {
            String rawType = entry.getKey();
            int minimum = Math.max(0, entry.getValue());

            Optional<EntityType<?>> entityType = resolveConfiguredEntityType(rawType);
            if (entityType.isEmpty()) {
                continue;
            }
            parsed.put(entityType.get(), minimum);
        }

        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("minimum_keep_count must contain at least one valid animal entity id");
        }

        return parsed;
    }

    private static Optional<EntityType<?>> resolveConfiguredEntityType(@Nonnull String rawType) {
        if (rawType.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation entityId = ResourceLocation.tryParse(rawType);
        if (entityId == null) {
            entityId = ResourceLocation.withDefaultNamespace(rawType);
        }

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            log.error("Unable to parse {} candidate as entity type {}", rawType, entityId);
            return Optional.empty();
        }

        return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(entityId));
    }

}
