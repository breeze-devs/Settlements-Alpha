package dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting;

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
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.conditions.NearbyBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class EnchantItemBehavior extends VillagerStateMachineBehavior {

    private enum EnchantStage implements StageKey {
        ENCHANT_ITEM,
        END
    }

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;
    private static final int MAX_BOOKSHELF_COUNT = 15;

    private final EnchantItemConfig config;
    private final NearbyBlockExistsCondition<BaseVillager> nearbyEnchantingTableCondition;
    private final EnchantmentEngine enchantmentEngine;

    @Nullable
    private BlockPos enchantingTablePos;
    @Nullable
    private ItemStack targetRepresentative;

    public EnchantItemBehavior(@Nonnull EnchantItemConfig config,
                               @Nonnull HungerConfig hungerConfig,
                               @Nonnull EnchantmentEngine enchantmentEngine) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.config = config;

        this.nearbyEnchantingTableCondition = new NearbyBlockExistsCondition<>(
                config.scanRangeHorizontal(),
                config.scanRangeVertical(),
                Blocks.ENCHANTING_TABLE,
                null,
                1);
        this.preconditions.add(ICondition.named("HasEnchantableNonEnchantedItem", this::hasEnchantableNonEnchantedItem));
        this.preconditions.add(this.nearbyEnchantingTableCondition);
        this.enchantmentEngine = enchantmentEngine;

        this.enchantingTablePos = null;
        this.targetRepresentative = null;

        this.initializeStateMachine(this.createControlStep(), EnchantStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("EnchantItemBehavior")
                .initialStage(EnchantStage.ENCHANT_ITEM)
                .stageStepMap(Map.of(EnchantStage.ENCHANT_ITEM, this.createEnchantStep()))
                .nextStage(EnchantStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createEnchantStep() {
        // TODO: we can make the animations more detailed (e.g. more ench particles) as villager expertise level goes up
        // TODO: we can also make 'golden' particles/animations if rolling a max level enchants or something good
        // TODO: to do this, we need to determine the enchantment result at the start of the behavior, so we can animate accordingly
        TimeBasedStep<BaseVillager> actionStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(10).asTickable())
                .addPeriodicStep(ClockTicks.of(10).getTicksAsInt(), context -> {
                    if (this.enchantingTablePos == null) {
                        return StepResult.noOp();
                    }

                    Location targetLocation = Location.of(this.enchantingTablePos, context.getInitiator().getMinecraftEntity().level())
                            .center(false);
                    targetLocation.displayParticles(ParticleTypes.ENCHANT, 50, 2, 1, 2, 1);
                    targetLocation.displayParticles(ParticleTypes.PORTAL, 3, 2, 1, 2, 1);
                    ParticleRegistry.displayCircle(ParticleTypes.END_ROD, targetLocation.add(0, -0.2, 0, true), 1.25, 16);

                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.seconds(8), this::performEnchant)
                .onEnd(context -> StepResult.complete())
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(actionStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.enchantingTablePos = this.nearbyEnchantingTableCondition.getTargets().getFirst();
        if (this.enchantingTablePos == null) {
            log.behaviorError("Unable to find enchanting table position");
            this.requestStop("Unable to find enchanting table position");
            return;
        }

        this.targetRepresentative = this.findFirstEnchantableNonEnchantedRepresentative(entity.getSettlementsInventory()).orElse(null);
        if (this.targetRepresentative == null) {
            log.behaviorError("No enchantable non-enchanted item found");
            this.requestStop("No enchantable non-enchanted item found");
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                PhysicalBlock.of(Location.of(this.enchantingTablePos, world), world.getBlockState(this.enchantingTablePos)))));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.enchantingTablePos == null || !(world.getBlockState(this.enchantingTablePos).is(Blocks.ENCHANTING_TABLE))) {
            log.behaviorError("Invalid enchanting table position: {}", this.enchantingTablePos);
            return false;
        }

        return true;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        entity.clearHeldItem();
        this.enchantingTablePos = null;
        this.targetRepresentative = null;
    }

    private StepResult performEnchant(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        Level world = villager.level();
        VillagerInventory inventory = villager.getSettlementsInventory();

        if (this.enchantingTablePos == null || this.targetRepresentative == null) {
            log.behaviorError("Failed to enchant item: enchanting setup or target is invalid");
            return StepResult.noOp();
        }

        Expertise expertise = villager.getExpertise();
        double intelligence = villager.getGenetics().getGeneValue(GeneType.INTELLIGENCE);
        int bookshelfCount = this.countNearbyBookshelves(world, this.enchantingTablePos);
        SpecializationProfile specialization = null; // TODO: implement specialization

        ItemStack enchantedItem = this.enchantmentEngine.enchant(this.targetRepresentative, expertise, intelligence, bookshelfCount,
                specialization, world.registryAccess());

        if (inventory.consume(this.targetRepresentative, 1) != 1) {
            return StepResult.fail("Failed to enchant item because item cannot be consumed");
        }

        log.behaviorStatus("Enchanted item from {} to {}", this.targetRepresentative, enchantedItem);
        inventory.add(enchantedItem);

        world.playSound(null, this.enchantingTablePos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        this.rewardExperience(villager);

        return StepResult.noOp();
    }

    private boolean hasEnchantableNonEnchantedItem(@Nullable BaseVillager villager) {
        if (villager == null) {
            return false;
        }
        return this.findFirstEnchantableNonEnchantedRepresentative(villager.getSettlementsInventory()).isPresent();
    }

    private Optional<ItemStack> findFirstEnchantableNonEnchantedRepresentative(@Nonnull VillagerInventory inventory) {
        return inventory.findFirst(stack -> !stack.getOrDefault(DataComponentRegistry.VILLAGER_ENCHANT_ATTEMPTED.get(), false)
                && (stack.isEnchantable() || stack.is(Items.BOOK)));
    }

    private int countNearbyBookshelves(@Nonnull Level world,
                                       @Nonnull BlockPos enchantingTablePosition) {
        int count = 0;

        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            // Early return if we already reached max
            if (count >= MAX_BOOKSHELF_COUNT) {
                return MAX_BOOKSHELF_COUNT;
            }

            // Check bookshelf existence
            BlockPos bookshelfPos = enchantingTablePosition.offset(offset);
            if (!world.getBlockState(bookshelfPos).is(Blocks.BOOKSHELF)) {
                continue;
            }

            // Check if anything is blocking the bookshelf
            BlockPos middlePos = enchantingTablePosition.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2);
            if (!world.getBlockState(middlePos).isAir()) {
                continue;
            }

            count++;
        }

        return count;
    }

}
