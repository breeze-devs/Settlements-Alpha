package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
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
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyMilkableCowExistsCondition;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import lombok.Getter;
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
public class MilkCowBehavior extends StateMachineBehavior {

    private enum MilkStage implements StageKey {
        MILK_COW,
        END;
    }

    private final MilkCowConfig config;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;
    private final NearbyMilkableCowExistsCondition<BaseVillager> nearbyMilkableCowExistsCondition;

    @Nullable
    private Cow target;
    private int milkCountRemaining;

    public MilkCowBehavior(@Nonnull MilkCowConfig config,
                           @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.config = config;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.milk_cow")
                .iconItemId(ResourceLocation.withDefaultNamespace("milk_bucket"))
                .displaySuffix(null)
                .build();

        this.nearbyMilkableCowExistsCondition = NearbyMilkableCowExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyMilkableCowExistsCondition);
        this.preconditions.add(entity -> entity.getSettlementsInventory().containsItem(Items.BUCKET));
        this.preconditions.add(entity -> entity.getSettlementsInventory().canAddItem(new ItemStack(Items.MILK_BUCKET)));

        this.target = null;
        this.milkCountRemaining = 0;

        this.initializeStateMachine(this.createControlStep(), MilkStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("MilkCowBehavior")
                .initialStage(MilkStage.MILK_COW)
                .stageStepMap(Map.of(MilkStage.MILK_COW, this.createMilkCowStep()))
                .nextStage(MilkStage.END)
                .build();
    }

    private BehaviorStep createMilkCowStep() {
        TimeBasedStep milkStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(2).asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.BUCKET.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(Ticks.seconds(1), this::performMilk)
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();

                    if (this.milkCountRemaining <= 0) {
                        return StepResult.complete();
                    }

                    VillagerInventory inventory = context.getInitiator().getMinecraftEntity().getSettlementsInventory();
                    if (!inventory.containsItem(Items.BUCKET) || !inventory.canAddItem(new ItemStack(Items.MILK_BUCKET))) {
                        return StepResult.complete();
                    }

                    if (this.target == null || !this.target.isAlive() || this.target.isBaby()) {
                        return StepResult.complete();
                    }

                    context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    return StepResult.transition(MilkStage.MILK_COW);
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep(0.55f, 1))
                .actionStep(milkStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        Expertise expertise = entity.getExpertise();
        this.milkCountRemaining = this.config.expertiseMilkLimit().getOrDefault(expertise.getConfigName(), 1);

        List<Cow> targets = this.nearbyMilkableCowExistsCondition.getTargets();
        if (targets.isEmpty()) {
            this.requestStop();
            return;
        }

        if (!entity.getSettlementsInventory().containsItem(Items.BUCKET)
                || !entity.getSettlementsInventory().canAddItem(new ItemStack(Items.MILK_BUCKET))) {
            this.requestStop();
            return;
        }

        this.target = targets.getFirst();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return this.target != null && this.target.isAlive();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        entity.clearHeldItem();

        this.target = null;
        this.milkCountRemaining = 0;
    }

    private StepResult performMilk(@Nonnull BehaviorContext context) {
        if (this.target == null || !this.target.isAlive() || this.target.isBaby()) {
            this.milkCountRemaining = 0;
            return StepResult.noOp();
        }

        VillagerInventory inventory = context.getInitiator().getMinecraftEntity().getSettlementsInventory();
        ItemStack milkBucketStack = new ItemStack(Items.MILK_BUCKET);

        if (!inventory.canAddItem(milkBucketStack) || inventory.consume(Items.BUCKET, 1) != 1) {
            this.milkCountRemaining = 0;
            return StepResult.noOp();
        }

        SoundRegistry.MILK_COW.playGlobally(Location.fromEntity(this.target, false), SoundSource.NEUTRAL);
        context.getInitiator().setHeldItem(Items.MILK_BUCKET.getDefaultInstance());

        inventory.addItem(milkBucketStack);
        this.milkCountRemaining--;

        return StepResult.noOp();
    }

}
