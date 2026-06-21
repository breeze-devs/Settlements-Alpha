package dev.breezes.settlements.application.ai.behavior.usecases.villager.support;

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
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.RepairIronGolemAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class RepairIronGolemBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation IRON_INGOT_ID = ResourceLocation.withDefaultNamespace("iron_ingot");
    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;
    private static final float HEAL_AMOUNT = 25.0f;
    private static final int DEFAULT_REPAIR_ATTEMPTS = 1;

    private enum RepairStage implements StageKey {
        REPAIR_GOLEM,
        END;
    }

    private final RepairIronGolemConfig config;

    @Nullable
    private IronGolem targetToRepair;
    private int remainingRepairAttempts;
    private boolean shouldRewardExperience;

    public RepairIronGolemBehavior(RepairIronGolemConfig config,
                                   HungerConfig hungerConfig,
                                   DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.config = config;

        // Create behavior preconditions
        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, IronGolem>builder()
                .entityType(IronGolem.class)
                .filter((villager, golem) -> this.isSufficientlyDamaged(golem))
                .build());
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(IRON_INGOT_ID), 1, 50, this.getClass().getSimpleName()));

        // Initialize variables
        this.targetToRepair = null;
        this.remainingRepairAttempts = 0;
        this.shouldRewardExperience = false;

        this.initializeStateMachine(this.createControlStep(), RepairStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("RepairIronGolemBehavior")
                .initialStage(RepairStage.REPAIR_GOLEM)
                .stageStepMap(Map.of(RepairStage.REPAIR_GOLEM, this.createRepairStep()))
                .nextStage(RepairStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createRepairStep() {
        TimeBasedStep<BaseVillager> repairTick = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(RepairIronGolemAnimations.REPAIR_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.REPAIR_IRON_GOLEM);
                    ctx.getInitiator().setHeldItem(ItemRegistry.HAMMER.get().getDefaultInstance());
                    ctx.getInitiator().setOffhandItem(new ItemStack(Items.IRON_INGOT));
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(RepairIronGolemAnimations.REPAIR_PEAK_TICK), ctx -> {
                    if (this.targetToRepair == null || !this.targetToRepair.isAlive()) {
                        return StepResult.complete();
                    }

                    if (!ctx.getInitiator().getSettlementsInventory()
                            .consumeIfRequired(Items.IRON_INGOT, 1, GeneralConfig.bypassInventoryRequirements)) {
                        // Ran out of ingots mid-task — stop gracefully
                        return StepResult.complete();
                    }

                    this.targetToRepair.heal(HEAL_AMOUNT);
                    this.shouldRewardExperience = true;

                    if (ctx.primaryDeed().isEmpty()) {
                        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.GOLEM_REPAIRED, null);
                        outcome.markSucceeded();
                        ctx.declarePrimaryDeed(outcome);
                    }

                    Location targetLocation = Location.fromEntity(this.targetToRepair, false);
                    SoundRegistry.REPAIR_IRON_GOLEM.playGlobally(targetLocation, SoundSource.NEUTRAL);
                    ParticleRegistry.repairIronGolem(targetLocation);

                    if (--this.remainingRepairAttempts <= 0) {
                        return StepResult.complete();
                    }
                    return StepResult.transition(RepairStage.REPAIR_GOLEM);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(repairTick)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        Optional<IronGolem> targetCandidate = this.findClosestDamagedIronGolem(villager);
        if (targetCandidate.isEmpty()) {
            this.requestStop("No damaged iron golem found within range");
            return;
        }

        this.targetToRepair = targetCandidate.get();
        Expertise expertise = villager.getMinecraftEntity().getExpertise();
        int maxAttempts = this.config.expertiseRepairLimit().getOrDefault(expertise.getConfigName(), DEFAULT_REPAIR_ATTEMPTS);
        int ingotsAvailable = villager.getSettlementsInventory().count(Items.IRON_INGOT);
        this.remainingRepairAttempts = GeneralConfig.bypassInventoryRequirements
                ? maxAttempts
                : Math.min(maxAttempts, ingotsAvailable);
        if (this.remainingRepairAttempts <= 0) {
            this.requestStop("No iron ingots available at behavior start");
            return;
        }
        this.shouldRewardExperience = false;
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.targetToRepair)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.targetToRepair != null
                && this.targetToRepair.isAlive()
                && this.targetToRepair.getHealth() < this.targetToRepair.getMaxHealth() * this.config.repairHpPercentage();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.clearOffhandItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.targetToRepair = null;
        this.remainingRepairAttempts = 0;
        this.shouldRewardExperience = false;
    }

    private Optional<IronGolem> findClosestDamagedIronGolem(@Nonnull BaseVillager villager) {
        return this.getPerceivedEntities(villager)
                .closest(IronGolem.class, this::isSufficientlyDamaged, villager);
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

    private boolean isSufficientlyDamaged(@Nonnull IronGolem ironGolem) {
        return ironGolem.isAlive()
                && ironGolem.getHealth() < ironGolem.getMaxHealth() * this.config.repairHpPercentage();
    }

}
