package dev.breezes.settlements.models.behaviors;

import com.google.common.collect.Lists;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbySoulSandExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
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
import java.util.List;

@CustomLog
public class HarvestSoulSandBehavior extends AbstractInteractAtTargetBehavior {

    private final HarvestSoulSandConfig config;
    private BlockPos netherWartPos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSoulSandAroundVillager = Lists.newArrayList();
    private final NearbySoulSandExistsCondition<BaseVillager> nearbySoulSandExistsCondition;

    public HarvestSoulSandBehavior(HarvestSoulSandConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())),
                Tickable.of(Ticks.seconds(0)));
        this.config = config;

        this.nearbySoulSandExistsCondition = NearbySoulSandExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbySoulSandExistsCondition);
        this.netherWartPos = null;
    }

    @Override
    public void doStart(@Nonnull Level level, @Nonnull BaseVillager villager) {
        this.validSoulSandAroundVillager = nearbySoulSandExistsCondition.getTargets();
        this.netherWartPos = getRandomPosition(level);
        while (level.getBlockState(netherWartPos).isAir() && !villager.hasItemInInventory(Items.NETHER_WART)) {
            validSoulSandAroundVillager.remove(netherWartPos);
            if (validSoulSandAroundVillager.isEmpty()) {
                requestStop();
            }
            netherWartPos = getRandomPosition(level);
        }
    }

    private BlockPos getRandomPosition(Level level) {
        return this.validSoulSandAroundVillager.get(level.getRandom().nextInt(this.validSoulSandAroundVillager.size())).above();
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.netherWartPos), 0.5F, 0));
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.netherWartPos));
    }

    @Override
    public void doStop(@Nonnull Level level, @Nonnull BaseVillager entity) {
        entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.netherWartPos = null;
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        BlockState wartBlockState = level.getBlockState(this.netherWartPos);
        Block wartBlock = wartBlockState.getBlock();
        if (wartBlock instanceof NetherWartBlock) {
            level.destroyBlock(this.netherWartPos, true, villager);
        }

        // plant the nether wart if the villager has one on its inventory
        if (villager.hasItemInInventory(Items.NETHER_WART)) {
            SimpleContainer simplecontainer = villager.getInventory();

            for (int i = 0; i < simplecontainer.getContainerSize(); ++i) {
                ItemStack itemstack;
                itemstack = simplecontainer.getItem(i);
                if (!itemstack.isEmpty() && itemstack.is(Items.NETHER_WART)) {
                    Item wartItem = itemstack.getItem();
                    if (wartItem instanceof BlockItem wartBlockItem) {
                        BlockState defaultWartState = wartBlockItem.getBlock().defaultBlockState();
                        level.setBlockAndUpdate(this.netherWartPos, defaultWartState);
                        level.gameEvent(GameEvent.BLOCK_PLACE, this.netherWartPos, GameEvent.Context.of(villager, defaultWartState));
                        level.playSound(null, this.netherWartPos.getX(), this.netherWartPos.getY(), this.netherWartPos.getZ(), SoundEvents.NETHER_WART_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            simplecontainer.setItem(i, ItemStack.EMPTY);
                        }
                        break;
                    }
                }
            }
        }
        requestStop();
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        this.timeWorkedSoFar++;
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.netherWartPos != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.netherWartPos.closerToCenterThan(villager.position(), 1.0);
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level level, @Nonnull BaseVillager entity) {
        return this.timeWorkedSoFar < 400;
    }
}
