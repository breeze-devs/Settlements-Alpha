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
import dev.breezes.settlements.domain.ai.conditions.NearbySoulSandExistsCondition;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class HarvestSoulSandBehavior extends VillagerStateMachineBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_SOUL_SAND,
        END;
    }

    @Nullable
    private BlockPos netherWartPos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSoulSandAroundVillager;
    private final NearbySoulSandExistsCondition<BaseVillager> nearbySoulSandExistsCondition;
    private boolean shouldRewardExperience;

    public HarvestSoulSandBehavior(HarvestSoulSandConfig config,
                                   HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.nearbySoulSandExistsCondition = NearbySoulSandExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbySoulSandExistsCondition);

        this.netherWartPos = null;
        this.timeWorkedSoFar = 0;
        this.validSoulSandAroundVillager = new ArrayList<>();
        this.shouldRewardExperience = false;

        this.initializeStateMachine(this.createControlStep(), HarvestStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("HarvestSoulSandBehavior")
                .initialStage(HarvestStage.HARVEST_SOUL_SAND)
                .stageStepMap(Map.of(HarvestStage.HARVEST_SOUL_SAND, this.createHarvestStep()))
                .nextStage(HarvestStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(1.0)
                .navigateStep(new NavigateToTargetStep<>(0.5f, 0))
                .actionStep(ctx -> {
                    if (this.netherWartPos == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    Level level = villager.level();

                    BlockState wartBlockState = level.getBlockState(this.netherWartPos);
                    Block wartBlock = wartBlockState.getBlock();
                    if (wartBlock instanceof NetherWartBlock) {
                        level.destroyBlock(this.netherWartPos, true, villager);
                        this.shouldRewardExperience = true;
                    }

                    if (villager.hasItemInInventory(Items.NETHER_WART)) {
                        ItemStack netherWart = Items.NETHER_WART.getDefaultInstance();
                        Item wartItem = netherWart.getItem();
                        if (wartItem instanceof BlockItem wartBlockItem && villager.getSettlementsInventory().consume(Items.NETHER_WART, 1) == 1) {
                            BlockState defaultWartState = wartBlockItem.getBlock().defaultBlockState();
                            level.setBlockAndUpdate(this.netherWartPos, defaultWartState);
                            level.gameEvent(GameEvent.BLOCK_PLACE, this.netherWartPos, GameEvent.Context.of(villager, defaultWartState));
                            level.playSound(null, this.netherWartPos.getX(), this.netherWartPos.getY(), this.netherWartPos.getZ(),
                                    SoundEvents.NETHER_WART_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                            this.shouldRewardExperience = true;
                        }
                    }

                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.timeWorkedSoFar = 0;
        this.shouldRewardExperience = false;

        this.validSoulSandAroundVillager = new ArrayList<>(this.nearbySoulSandExistsCondition.getTargets());
        if (this.validSoulSandAroundVillager.isEmpty()) {
            this.requestStop("No soul sand found within range");
            return;
        }

        this.netherWartPos = this.getRandomPosition(world);
        while (world.getBlockState(this.netherWartPos).isAir() && !villager.hasItemInInventory(Items.NETHER_WART)) {
            this.validSoulSandAroundVillager.remove(this.netherWartPos);
            if (this.validSoulSandAroundVillager.isEmpty()) {
                this.requestStop("No more soul sand to harvest");
                return;
            }
            this.netherWartPos = this.getRandomPosition(world);
        }

        context.setState(BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.netherWartPos, world), world.getBlockState(this.netherWartPos)))));
    }

    private BlockPos getRandomPosition(Level world) {
        return this.validSoulSandAroundVillager.get(world.getRandom().nextInt(this.validSoulSandAroundVillager.size())).above();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        this.timeWorkedSoFar = 0;
        this.netherWartPos = null;
        this.validSoulSandAroundVillager = new ArrayList<>();
        this.shouldRewardExperience = false;
    }

    @Override
    public boolean tickContinueConditions(int delta,
                                          @Nonnull Level world,
                                          @Nonnull BaseVillager villager) {
        this.timeWorkedSoFar += delta;
        return super.tickContinueConditions(delta, world, villager) && this.timeWorkedSoFar < 400;
    }

}
