package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.NearbySoulSandExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.util.Ticks;
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
public class HarvestSoulSandBehavior extends BaseVillagerStagedBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_SOUL_SAND,
        END;
    }

    private final HarvestSoulSandConfig config;
    private final StagedStep controlStep;

    @Nullable
    private BlockPos netherWartPos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSoulSandAroundVillager;
    private final NearbySoulSandExistsCondition<BaseVillager> nearbySoulSandExistsCondition;

    @Nullable
    private BehaviorContext context;

    public HarvestSoulSandBehavior(HarvestSoulSandConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));
        this.config = config;

        this.nearbySoulSandExistsCondition = NearbySoulSandExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbySoulSandExistsCondition);

        this.netherWartPos = null;
        this.timeWorkedSoFar = 0;
        this.validSoulSandAroundVillager = new ArrayList<>();
        this.context = null;

        this.controlStep = StagedStep.builder()
                .name("HarvestSoulSandBehavior")
                .initialStage(HarvestStage.HARVEST_SOUL_SAND)
                .stageStepMap(Map.of(
                        HarvestStage.HARVEST_SOUL_SAND, this.createHarvestStep()
                ))
                .nextStage(HarvestStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep createHarvestStep() {
        return StayCloseStep.builder()
                .closeEnoughDistance(1.0)
                .navigateStep(new NavigateToTargetStep(0.5f, 0))
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
                    }

                    if (villager.hasItemInInventory(Items.NETHER_WART)) {
                        ItemStack netherWart = Items.NETHER_WART.getDefaultInstance();
                        Item wartItem = netherWart.getItem();
                        if (wartItem instanceof BlockItem wartBlockItem && villager.getSettlementsInventory().consume(Items.NETHER_WART, 1) == 1) {
                            BlockState defaultWartState = wartBlockItem.getBlock().defaultBlockState();
                            level.setBlockAndUpdate(this.netherWartPos, defaultWartState);
                            level.gameEvent(GameEvent.BLOCK_PLACE, this.netherWartPos, GameEvent.Context.of(villager, defaultWartState));
                            level.playSound(null, this.netherWartPos.getX(), this.netherWartPos.getY(), this.netherWartPos.getZ(), SoundEvents.NETHER_WART_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    }

                    return StepResult.complete();
                })
                .build();
    }

    @Override
    public void doStart(@Nonnull Level level, @Nonnull BaseVillager villager) {
        this.context = new BehaviorContext(villager);
        this.timeWorkedSoFar = 0;

        this.validSoulSandAroundVillager = new ArrayList<>(this.nearbySoulSandExistsCondition.getTargets());
        if (this.validSoulSandAroundVillager.isEmpty()) {
            this.requestStop();
            return;
        }

        this.netherWartPos = this.getRandomPosition(level);
        while (level.getBlockState(this.netherWartPos).isAir() && !villager.hasItemInInventory(Items.NETHER_WART)) {
            this.validSoulSandAroundVillager.remove(this.netherWartPos);
            if (this.validSoulSandAroundVillager.isEmpty()) {
                this.requestStop();
                return;
            }
            this.netherWartPos = this.getRandomPosition(level);
        }

        this.context.setState(
                BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.netherWartPos, level), level.getBlockState(this.netherWartPos))))
        );
    }

    private BlockPos getRandomPosition(Level level) {
        return this.validSoulSandAroundVillager.get(level.getRandom().nextInt(this.validSoulSandAroundVillager.size())).above();
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }

        this.timeWorkedSoFar += delta;
        StepResult result = this.controlStep.tick(this.context);
        this.handleStepResult(result, HarvestStage.END, "HarvestSoulSandBehavior");
    }

    @Override
    public void doStop(@Nonnull Level level, @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        this.timeWorkedSoFar = 0;
        this.netherWartPos = null;
        this.validSoulSandAroundVillager = new ArrayList<>();
        this.context = null;
        this.controlStep.reset();
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level level, @Nonnull BaseVillager entity) {
        return super.tickContinueConditions(delta, level, entity) && this.timeWorkedSoFar < 400;
    }
}
