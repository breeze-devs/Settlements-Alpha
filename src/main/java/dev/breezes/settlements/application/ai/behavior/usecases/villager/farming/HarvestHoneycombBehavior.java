package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

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
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyFullHiveExistsCondition;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class HarvestHoneycombBehavior extends StateMachineBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_HONEYCOMB,
        END;
    }

    private final HarvestHoneycombConfig config;
    private final HarvestHoneycombYieldDataManager yieldData;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;
    private final NearbyFullHiveExistsCondition<BaseVillager> nearbyFullHiveExistsCondition;

    @Nullable
    private BlockPos targetHivePos;
    private int harvestsRemaining;

    public HarvestHoneycombBehavior(@Nonnull HarvestHoneycombConfig config,
                                    @Nonnull HarvestHoneycombYieldDataManager yieldData) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable());
        this.config = config;
        this.yieldData = yieldData;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.harvest_honeycomb")
                .iconItemId(ResourceLocation.withDefaultNamespace("honeycomb"))
                .displaySuffix(null)
                .build();
        this.nearbyFullHiveExistsCondition = NearbyFullHiveExistsCondition.<BaseVillager>builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();

        this.preconditions.add(this.nearbyFullHiveExistsCondition);
        this.preconditions.add(entity -> entity.getSettlementsInventory().containsItem(Items.SHEARS));
        this.preconditions.add(entity -> entity.getSettlementsInventory().canAddItem(new ItemStack(Items.HONEYCOMB)));

        this.initializeStateMachine(this.createControlStep(), HarvestStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("HarvestHoneycombBehavior")
                .initialStage(HarvestStage.HARVEST_HONEYCOMB)
                .stageStepMap(Map.of(HarvestStage.HARVEST_HONEYCOMB, this.createHarvestStep()))
                .nextStage(HarvestStage.END)
                .build();
    }

    private BehaviorStep createHarvestStep() {
        TimeBasedStep harvestStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(Ticks.seconds(0.5), this::performHarvest)
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    if (this.harvestsRemaining > 0 && this.selectFreshTarget(context.getInitiator().getMinecraftEntity(), context.getInitiator().getMinecraftEntity().level(), context)) {
                        return StepResult.transition(HarvestStage.HARVEST_HONEYCOMB);
                    }
                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep(0.55f, 1))
                .actionStep(harvestStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        Expertise expertise = entity.getExpertise();
        this.harvestsRemaining = this.config.expertiseHarvestLimit().getOrDefault(expertise.getConfigName(), 1);

        // Precondition scan already ran, pull from its cached results
        List<BlockPos> targets = this.nearbyFullHiveExistsCondition.getTargets();
        VillagerInventory inventory = entity.getSettlementsInventory();
        if (targets.isEmpty()
                || !inventory.containsItem(Items.SHEARS)
                || !inventory.canAddItem(new ItemStack(Items.HONEYCOMB))) {
            this.requestStop();
            return;
        }

        this.targetHivePos = targets.getFirst();
        context.setState(BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.targetHivePos, world), world.getBlockState(this.targetHivePos)))));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return this.targetHivePos != null;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager entity) {
        entity.clearHeldItem();
        entity.getNavigationManager().stop();
        this.targetHivePos = null;
        this.harvestsRemaining = 0;
    }

    private boolean selectFreshTarget(@Nonnull BaseVillager villager,
                                      @Nonnull Level world,
                                      @Nonnull BehaviorContext context) {
        if (!this.nearbyFullHiveExistsCondition.test(villager)) {
            return false;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        if (!inventory.containsItem(Items.SHEARS) || !inventory.canAddItem(new ItemStack(Items.HONEYCOMB))) {
            return false;
        }

        List<BlockPos> targets = this.nearbyFullHiveExistsCondition.getTargets();
        if (targets.isEmpty()) {
            return false;
        }

        this.targetHivePos = targets.getFirst();
        context.setState(BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.targetHivePos, world), world.getBlockState(this.targetHivePos)))));
        return true;
    }

    private StepResult performHarvest(@Nonnull BehaviorContext context) {
        if (this.targetHivePos == null) {
            this.harvestsRemaining = 0;
            return StepResult.noOp();
        }

        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        Level level = villager.level();
        BlockState state = level.getBlockState(this.targetHivePos);
        boolean isReady = state.getOptionalValue(BeehiveBlock.HONEY_LEVEL)
                .map(levelValue -> levelValue == BeehiveBlock.MAX_HONEY_LEVELS)
                .orElse(false);
        if (!isReady) {
            this.harvestsRemaining = 0;
            return StepResult.noOp();
        }

        String harvestedBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String expertiseName = villager.getExpertise().getConfigName();
        for (ItemStack drop : this.yieldData.rollDrops(expertiseName, harvestedBlockId)) {
            if (!drop.isEmpty()) {
                villager.getSettlementsInventory().addOrDropItem(drop, level, villager.getX(), villager.getY(), villager.getZ());
            }
        }

        level.setBlockAndUpdate(this.targetHivePos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
        SoundRegistry.HARVEST_HONEYCOMB.playGlobally(Location.of(this.targetHivePos, level), SoundSource.BLOCKS);
        this.harvestsRemaining--;
        return StepResult.noOp();
    }

}
