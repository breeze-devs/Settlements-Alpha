package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
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
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyMilkableCowExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class MilkCowBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation BUCKET_ID = ResourceLocation.withDefaultNamespace("bucket");

    private enum MilkStage implements StageKey {
        MILK_COW,
        END;
    }

    private final MilkCowConfig config;
    private final NearbyMilkableCowExistsCondition<BaseVillager> nearbyMilkableCowExistsCondition;

    @Nullable
    private Cow target;
    private int milkCountRemaining;
    private boolean shouldRewardExperience;

    public MilkCowBehavior(@Nonnull MilkCowConfig config,
                           @Nonnull HungerConfig hungerConfig,
                           @Nonnull DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.config = config;

        this.nearbyMilkableCowExistsCondition = NearbyMilkableCowExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyMilkableCowExistsCondition);
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(BUCKET_ID), 1, 50, this.getClass().getSimpleName()));

        this.target = null;
        this.milkCountRemaining = 0;
        this.shouldRewardExperience = false;

        this.initializeStateMachine(this.createControlStep(), MilkStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("MilkCowBehavior")
                .initialStage(MilkStage.MILK_COW)
                .stageStepMap(Map.of(MilkStage.MILK_COW, this.createMilkCowStep()))
                .nextStage(MilkStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createMilkCowStep() {
        TimeBasedStep<BaseVillager> milkStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(2).asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.BUCKET.getDefaultInstance());
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.seconds(1), this::performMilk)
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();

                    if (this.milkCountRemaining <= 0) {
                        return StepResult.complete();
                    }

                    VillagerInventory inventory = context.getInitiator().getMinecraftEntity().getSettlementsInventory();
                    if (!inventory.containsOrBypassed(Items.BUCKET, GeneralConfig.bypassInventoryRequirements)) {
                        return StepResult.complete();
                    }

                    if (this.target == null || !this.target.isAlive() || this.target.isBaby()) {
                        return StepResult.complete();
                    }

                    context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    return StepResult.transition(MilkStage.MILK_COW);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(milkStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        Expertise expertise = villager.getExpertise();
        this.milkCountRemaining = this.config.expertiseMilkLimit().getOrDefault(expertise.getConfigName(), 1);
        this.shouldRewardExperience = false;

        List<Cow> targets = this.nearbyMilkableCowExistsCondition.getTargets();
        if (targets.isEmpty()) {
            this.requestStop("No milkable cows found");
            return;
        }

        if (!villager.getSettlementsInventory().containsOrBypassed(Items.BUCKET, GeneralConfig.bypassInventoryRequirements)) {
            this.requestStop("Not enough buckets in inventory");
            return;
        }

        this.target = targets.getFirst();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.target != null && this.target.isAlive();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.target = null;
        this.milkCountRemaining = 0;
        this.shouldRewardExperience = false;
    }

    private StepResult performMilk(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.target == null || !this.target.isAlive() || this.target.isBaby()) {
            this.milkCountRemaining = 0;
            return StepResult.noOp();
        }

        VillagerInventory inventory = context.getInitiator().getMinecraftEntity().getSettlementsInventory();
        ItemStack milkBucketStack = new ItemStack(Items.MILK_BUCKET);

        if (!inventory.consumeIfRequired(Items.BUCKET, 1, GeneralConfig.bypassInventoryRequirements)) {
            this.milkCountRemaining = 0;
            return StepResult.noOp();
        }

        SoundRegistry.MILK_COW.playGlobally(Location.fromEntity(this.target, false), SoundSource.NEUTRAL);
        context.getInitiator().setHeldItem(Items.MILK_BUCKET.getDefaultInstance());

        inventory.add(milkBucketStack);
        this.milkCountRemaining--;
        this.shouldRewardExperience = true;

        return StepResult.noOp();
    }

}
