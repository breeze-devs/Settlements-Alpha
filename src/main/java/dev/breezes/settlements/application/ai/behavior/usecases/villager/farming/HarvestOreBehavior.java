package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

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
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.NearbyOreExistsCondition;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class HarvestOreBehavior extends VillagerStateMachineBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_ORE,
        END
    }

    @Nullable
    private BlockPos orePos;
    private int timeWorkedSoFar;
    private List<BlockPos> validOresAroundVillager;
    private final NearbyOreExistsCondition<BaseVillager> nearbyOreExistsCondition;

    public HarvestOreBehavior(HarvestOreConfig config,
                              HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.nearbyOreExistsCondition = NearbyOreExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyOreExistsCondition);

        this.orePos = null;
        this.timeWorkedSoFar = 0;
        this.validOresAroundVillager = new ArrayList<>();

        this.initializeStateMachine(this.createControlStep(), HarvestOreBehavior.HarvestStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("HarvestOreBehavior")
                .initialStage(HarvestOreBehavior.HarvestStage.HARVEST_ORE)
                .stageStepMap(Map.of(HarvestOreBehavior.HarvestStage.HARVEST_ORE, this.createHarvestStep()))
                .nextStage(HarvestOreBehavior.HarvestStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(1.5)
                .navigateStep(new NavigateToTargetStep<>(0.5f, 2))
                .actionStep(context -> {
                    if (this.orePos == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();

                    Level level = villager.level();
                    BlockState replaceBlock = level.getBlockState(orePos);
                    replaceBlock = (replaceBlock.is(Tags.Blocks.ORES_IN_GROUND_STONE)) ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                    level.destroyBlock(this.orePos, true, villager);
                    level.setBlockAndUpdate(orePos, replaceBlock);
                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.timeWorkedSoFar = 0;

        this.validOresAroundVillager = new ArrayList<>(this.nearbyOreExistsCondition.getTargets());
        if (this.validOresAroundVillager.isEmpty()) {
            this.requestStop("No ores found within range");
            return;
        }

        this.orePos = getRandomPosition(world);
        context.setState(BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.orePos, world), world.getBlockState(this.orePos)))));
    }

    private BlockPos getRandomPosition(Level world) {
        return this.validOresAroundVillager
                .get(world.getRandom().nextInt(this.validOresAroundVillager.size()));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        this.timeWorkedSoFar = 0;
        this.orePos = null;
        this.validOresAroundVillager = new ArrayList<>();
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        this.timeWorkedSoFar += delta;
        return super.tickContinueConditions(delta, world, entity) && this.timeWorkedSoFar < 400;
    }
}
