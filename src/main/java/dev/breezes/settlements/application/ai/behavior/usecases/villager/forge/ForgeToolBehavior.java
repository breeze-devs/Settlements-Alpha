package dev.breezes.settlements.application.ai.behavior.usecases.villager.forge;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CraftBatchCalculator;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CraftRecipeAvailableCondition;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.RecipeCommit;
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
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.ForgeAnimations;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.forge.catalog.ForgeCatalogRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * The tool-smith's dedicated forge behavior: the villager prefers a known anvil, falling back to its
 * own workstation (the vanilla smithing table) when no anvil is known or reachable. The forge
 * transaction commits up front when the behavior starts; the villager then walks to the station and
 * plays a looping cosmetic hammering animation for a fixed number of strikes — hammer in the main hand,
 * an iron ingot worked in the off hand. Distinct from {@code CraftGoodsBehavior} — the two behaviors
 * produce disjoint item sets for the tool smith (forged metal tools here, everything else there) — so
 * this behavior owns its own station preference and gesture rather than being a flavor of generic crafting.
 */
@CustomLog
public class ForgeToolBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;
    private static final int JOB_SITE_COMPLETION_RANGE = 1;
    private static final int ANVIL_COMPLETION_RANGE = 1;
    private static final ClockTicks APPROACH_TIMEOUT = ClockTicks.seconds(20);

    private static final int STRIKE_COUNT = 5;

    private enum ForgeStage implements StageKey {
        FORGE,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final CraftRecipeAvailableCondition craftRecipeAvailableCondition;
    private final CraftBatchCalculator batchCalculator;
    private final BlockMemoryTargetResolver blockMemoryTargetResolver;

    @Nullable
    private PhysicalBlock jobSite;

    public ForgeToolBehavior(ForgeToolConfig config,
                             BehaviorSupport support,
                             ForgeCatalogRegistry forgeCatalog,
                             TradeCatalogRegistry tradeCatalog) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.batchCalculator = new CraftBatchCalculator(tradeCatalog);
        this.blockMemoryTargetResolver = support.getBlockMemoryTargetResolver();

        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.SMITHING_TABLE), JOB_SITE_COMPLETION_RANGE);
        this.craftRecipeAvailableCondition = new CraftRecipeAvailableCondition(forgeCatalog, this.batchCalculator);
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.craftRecipeAvailableCondition);

        this.jobSite = null;

        this.initializeStateMachine(this.createControlStep(), ForgeStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ForgeToolBehavior")
                .initialStage(ForgeStage.FORGE)
                .stageStepMap(Map.of(
                        ForgeStage.FORGE, this.createForgeStep()
                ))
                .nextStage(ForgeStage.END)
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
            this.requestStop("No forgeable recipes available");
            return;
        }

        // Commit the forge transaction the moment the smith commits to the work
        CraftRecipe recipe = this.batchCalculator.selectPreferred(entity, validRecipes);
        RecipeCommit.commit(context, recipe, this.batchCalculator, WorldEventType.TOOL_FORGED, this::rewardExperience);

        boolean resolvedAnvil = this.blockMemoryTargetResolver.resolveBlockTarget(context, MemoryTypeRegistry.ANVIL_SITES,
                BlockMatchers.ANVIL, BlockScanBox.confirm(), BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS, ANVIL_COMPLETION_RANGE);
        if (!resolvedAnvil) {
            context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.jobSite)));
        }
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
        villager.clearOffhandItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.jobSite = null;
    }

    private BehaviorStep<BaseVillager> createForgeStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(this.createHammerStep())
                .timeoutTicks(APPROACH_TIMEOUT.getTicksAsInt())
                .timeoutTransition(ForgeStage.END)
                .build();
    }

    /**
     * The cosmetic hammering, run once the villager has arrived at its anvil (or workstation fallback).
     */
    private BehaviorStep<BaseVillager> createHammerStep() {
        int sessionTicks = STRIKE_COUNT * ForgeAnimations.STRIKE_DURATION_TICKS;
        return TimeBasedStep.<BaseVillager>builder()
                .name("ForgeToolBehavior.forge")
                .withTickable(ClockTicks.of(sessionTicks).asTickable())
                .onStart(this::startForging)
                .addPeriodicStep(ForgeAnimations.STRIKE_DURATION_TICKS, ForgeAnimations.STRIKE_PEAK_TICK, this::playStrikeBeat)
                .build();
    }

    private StepResult startForging(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        villager.setHeldItem(ItemRegistry.HAMMER.get().getDefaultInstance());
        villager.setOffhandItem(new ItemStack(Items.IRON_INGOT));
        villager.setMotion(AnimationArchetype.FORGE);
        return StepResult.noOp();
    }

    private StepResult playStrikeBeat(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        Location location = Location.fromEntity(villager, true);
        location.displayParticles(ParticleTypes.CRIT, 4, 0.2, 0.2, 0.2, 0.05);
        location.playSound(SoundEvents.ANVIL_LAND, 0.6f, 1.4f, SoundSource.BLOCKS);
        return StepResult.noOp();
    }

}
