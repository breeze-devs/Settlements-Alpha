package dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting;

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
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyBlockExistsCondition;
import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
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
public class EnchantItemBehavior extends StateMachineBehavior {

    private enum EnchantStage implements StageKey {
        ENCHANT_ITEM,
        END
    }

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;
    private static final int MAX_BOOKSHELF_COUNT = 15;

    private final EnchantItemConfig config;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;
    private final NearbyBlockExistsCondition<BaseVillager> nearbyEnchantingTableCondition;
    private final EnchantmentEngine enchantmentEngine;

    @Nullable
    private BlockPos enchantingTablePos;
    private int targetSlot;

    public EnchantItemBehavior(@Nonnull EnchantItemConfig config) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable());

        this.config = config;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.enchant_item")
                .iconItemId(ResourceLocation.withDefaultNamespace("enchanting_table"))
                .displaySuffix(null)
                .build();

        this.nearbyEnchantingTableCondition = new NearbyBlockExistsCondition<>(
                config.scanRangeHorizontal(),
                config.scanRangeVertical(),
                Blocks.ENCHANTING_TABLE,
                null,
                1);
        this.preconditions.add(this::hasEnchantableNonEnchantedItem);
        this.preconditions.add(this.nearbyEnchantingTableCondition);
        this.enchantmentEngine = new EnchantmentEngine(EnchantmentCostDataManager.getInstance());

        this.enchantingTablePos = null;
        this.targetSlot = -1;

        this.initializeStateMachine(this.createControlStep(), EnchantStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("EnchantItemBehavior")
                .initialStage(EnchantStage.ENCHANT_ITEM)
                .stageStepMap(Map.of(EnchantStage.ENCHANT_ITEM, this.createEnchantStep()))
                .nextStage(EnchantStage.END)
                .build();
    }

    private BehaviorStep createEnchantStep() {
        // TODO: we can make the animations more detailed (e.g. more ench particles) as villager expertise level goes up
        // TODO: we can also make 'golden' particles/animations if rolling a max level enchants or something good
        // TODO: to do this, we need to determine the enchantment result at the start of the behavior, so we can animate accordingly
        TimeBasedStep actionStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(10).asTickable())
                .addPeriodicStep(Ticks.of(10).getTicksAsInt(), context -> {
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
                .addKeyFrame(Ticks.seconds(8), this::performEnchant)
                .onEnd(context -> StepResult.complete())
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep(0.5f, 1))
                .actionStep(actionStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        this.enchantingTablePos = this.nearbyEnchantingTableCondition.getTargets().getFirst();
        if (this.enchantingTablePos == null) {
            log.behaviorError("Unable to find enchanting table position");
            this.requestStop();
            return;
        }

        this.targetSlot = this.findFirstEnchantableNonEnchantedSlot(entity.getSettlementsInventory()).orElse(-1);
        if (this.targetSlot < 0) {
            log.behaviorError("Invalid enchanting slot: {}", this.targetSlot);
            this.requestStop();
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                PhysicalBlock.of(Location.of(this.enchantingTablePos, world), world.getBlockState(this.enchantingTablePos)))));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
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
        this.targetSlot = -1;
    }

    private StepResult performEnchant(@Nonnull BehaviorContext context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        Level world = villager.level();
        VillagerInventory inventory = villager.getSettlementsInventory();

        if (this.enchantingTablePos == null || !isValidEnchantTarget(inventory, this.targetSlot)) {
            log.behaviorError("Failed to enchant item: enchanting setup or target is invalid");
            return StepResult.noOp();
        }

        ItemStack originalItem = inventory.getBackpack().getItem(this.targetSlot);

        if (originalItem.getOrDefault(DataComponentRegistry.VILLAGER_ENCHANT_ATTEMPTED.get(), false)) {
            log.behaviorStatus("Will not enchant: item has already been villager-enchanted");
            return StepResult.noOp();
        }

        Expertise expertise = villager.getExpertise();
        double intelligence = villager.getGenetics().getGeneValue(GeneType.INTELLIGENCE);
        int bookshelfCount = this.countNearbyBookshelves(world, this.enchantingTablePos);
        SpecializationProfile specialization = null; // TODO: implement specialization

        ItemStack enchantedItem = this.enchantmentEngine.enchant(originalItem, expertise, intelligence, bookshelfCount,
                specialization, world.registryAccess());

        log.behaviorStatus("Enchanted item from {} to {}", originalItem, enchantedItem);
        originalItem.shrink(1);
        inventory.addItem(enchantedItem);

        world.playSound(null, this.enchantingTablePos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return StepResult.noOp();
    }

    private boolean hasEnchantableNonEnchantedItem(@Nullable BaseVillager villager) {
        if (villager == null) {
            return false;
        }
        return this.findFirstEnchantableNonEnchantedSlot(villager.getSettlementsInventory()).isPresent();
    }

    private Optional<Integer> findFirstEnchantableNonEnchantedSlot(@Nonnull VillagerInventory inventory) {
        for (int i = 0; i < inventory.getBackpackSize(); i++) {
            if (isValidEnchantTarget(inventory, i)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static boolean isValidEnchantTarget(@Nonnull VillagerInventory inventory, int slot) {
        ItemStack stack = inventory.getBackpack().getItem(slot);
        if (stack.isEmpty() || stack.getOrDefault(DataComponentRegistry.VILLAGER_ENCHANT_ATTEMPTED.get(), false)) {
            return false;
        }
        return stack.isEnchantable() || stack.is(Items.BOOK);
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
