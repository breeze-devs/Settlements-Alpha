package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.crafting.catalog.CraftCatalogRegistry;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The data-driven catch-all for generic crafting-grid recipes with no dedicated station behavior.
 * The villager paths to its own profession job-site block and turns raw materials into finished goods
 * there, gated by the trade {@code StockPolicy} ladder so it only crafts what the village needs.
 * <p>
 * A single catalog entry and {@code BehaviorKey} serve every profession: the behavior self-resolves the
 * villager's profession recipes at runtime.
 */
@CustomLog
public class CraftGoodsBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;
    private static final int JOB_SITE_COMPLETION_RANGE = 1;
    private static final ClockTicks APPROACH_TIMEOUT = ClockTicks.seconds(20);

    private enum CraftStage implements StageKey {
        CRAFT,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final CraftRecipeAvailableCondition craftRecipeAvailableCondition;
    private final CraftBatchCalculator batchCalculator;

    @Nullable
    private PhysicalBlock jobSite;
    @Nullable
    private CraftRecipe currentRecipe;

    public CraftGoodsBehavior(CraftGoodsConfig config,
                              BehaviorSupport support,
                              CraftCatalogRegistry craftCatalog,
                              TradeCatalogRegistry tradeCatalog) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.batchCalculator = new CraftBatchCalculator(tradeCatalog);

        // A null block predicate accepts the villager's job-site regardless of profession; the vanilla
        // brain guarantees the claimed POI already matches the profession, and the condition's canReach
        // check makes "workstation present AND reachable" the precondition for free.
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(null, JOB_SITE_COMPLETION_RANGE);
        this.craftRecipeAvailableCondition = new CraftRecipeAvailableCondition(craftCatalog, this.batchCalculator);
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.craftRecipeAvailableCondition);

        this.jobSite = null;
        this.currentRecipe = null;

        this.initializeStateMachine(this.createControlStep(), CraftStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("CraftGoodsBehavior")
                .initialStage(CraftStage.CRAFT)
                .stageStepMap(Map.of(
                        CraftStage.CRAFT, this.createCraftStep()
                ))
                .nextStage(CraftStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop("No job-site block found");
            return;
        }
        this.jobSite = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();

        List<CraftRecipe> validRecipes = this.craftRecipeAvailableCondition.getValidRecipes();
        if (validRecipes.isEmpty()) {
            this.requestStop("No craftable recipes available");
            return;
        }

        this.currentRecipe = this.selectRecipe(entity, validRecipes);
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.jobSite)));
    }

    /**
     * Prefers the output the village most lacks (largest headroom below its overflow ceiling), breaking
     * ties by the datapack {@code priority}.
     */
    private CraftRecipe selectRecipe(@Nonnull BaseVillager villager, @Nonnull List<CraftRecipe> validRecipes) {
        return validRecipes.stream()
                .max(Comparator
                        .comparingInt((CraftRecipe recipe) -> this.batchCalculator.ceilingHeadroom(villager, recipe))
                        .thenComparingInt(CraftRecipe::priority))
                .orElseThrow();
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.jobSite != null;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.jobSite = null;
        this.currentRecipe = null;
    }

    private BehaviorStep<BaseVillager> createCraftStep() {
        CraftSequenceStep craftSequence = new CraftSequenceStep("CraftGoodsBehavior.craft",
                () -> this.currentRecipe, this.batchCalculator, this::rewardExperience, CraftStage.END);

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(craftSequence)
                .timeoutTicks(APPROACH_TIMEOUT.getTicksAsInt())
                .timeoutTransition(CraftStage.END)
                .build();
    }

}
