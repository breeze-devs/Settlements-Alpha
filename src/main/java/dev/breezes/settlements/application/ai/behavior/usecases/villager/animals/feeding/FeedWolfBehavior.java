package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.feeding;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.OwnedWolfExistsCondition;
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
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import lombok.CustomLog;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class FeedWolfBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.5D;
    private static final float NAVIGATION_SPEED = 0.55F;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 2;
    private static final int REGEN_DURATION_TICKS = ClockTicks.minutes(10).getTicksAsInt();

    private enum FeedStage implements StageKey {
        FEED_WOLF,
        END;
    }

    private final FeedWolfConfig config;
    private final OwnedWolfExistsCondition ownedWolfCondition;

    @Nullable
    private SettlementsWolf targetWolf;
    @Nullable
    private ItemStack selectedMeat;
    private int selectedMeatSlot;

    public FeedWolfBehavior(@Nonnull FeedWolfConfig config, @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);
        this.config = config;

        this.targetWolf = null;
        this.selectedMeat = null;
        this.selectedMeatSlot = -1;

        this.ownedWolfCondition = new OwnedWolfExistsCondition(config.scanRangeHorizontal(), config.scanRangeVertical(), ignored -> true);
        this.preconditions.add(ICondition.named("HasMeat", villager -> findFirstMeatSlot(villager.getSettlementsInventory()) >= 0));
        this.preconditions.add(this.ownedWolfCondition);

        this.initializeStateMachine(this.createControlStep(), FeedStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("FeedWolfBehavior")
                .initialStage(FeedStage.FEED_WOLF)
                .stageStepMap(Map.of(FeedStage.FEED_WOLF, this.createFeedWolfStep()))
                .nextStage(FeedStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createFeedWolfStep() {
        TimeBasedStep<BaseVillager> interactStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(this::performFeed)
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NAVIGATION_SPEED, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(interactStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.targetWolf = null;
        this.selectedMeat = null;
        this.selectedMeatSlot = -1;

        // Re-validate: conditions may have changed since the last precondition check
        if (!this.ownedWolfCondition.test(villager)) {
            this.requestStop("No owned wolves within range");
            return;
        }

        this.targetWolf = this.ownedWolfCondition.getTargets().stream().findFirst().orElse(null);
        if (this.targetWolf == null) {
            this.requestStop("Chosen hurt wolf is null");
            return;
        }

        this.selectedMeatSlot = findFirstMeatSlot(villager.getSettlementsInventory());
        if (this.selectedMeatSlot < 0) {
            this.requestStop("No meat found in inventory at behavior start");
            return;
        }

        this.selectedMeat = villager.getSettlementsInventory()
                .getBackpack().getItem(this.selectedMeatSlot).copyWithCount(1);
        villager.setHeldItem(this.selectedMeat.copy());
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.targetWolf)));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();

        this.targetWolf = null;
        this.selectedMeat = null;
        this.selectedMeatSlot = -1;
    }

    private StepResult performFeed(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.targetWolf == null || !this.targetWolf.isAlive()) {
            return StepResult.complete();
        }

        this.targetWolf.heal(this.config.healAmount());
        this.targetWolf.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, 0));

        Location wolfLoc = Location.fromEntity(this.targetWolf, false);
        ParticleRegistry.breedHearts(wolfLoc);
        if (this.selectedMeat != null && !this.selectedMeat.isEmpty()) {
            wolfLoc.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, this.selectedMeat.copy()), 6, 0.2, 0.2, 0.2, 0.02);
        }
        wolfLoc.playSound(SoundEvents.GENERIC_EAT, 0.8f, 1.0f, SoundSource.NEUTRAL);

        this.consumeMeat(context.getInitiator());
        return StepResult.noOp();
    }

    private void consumeMeat(@Nonnull BaseVillager villager) {
        if (this.selectedMeat == null || this.selectedMeatSlot < 0) {
            return;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        ItemStack slotNow = inventory.getBackpack().getItem(this.selectedMeatSlot);

        // Guard against inventory churn between onBehaviorStart and the keyframe firing
        if (!slotNow.isEmpty() && slotNow.getItem() == this.selectedMeat.getItem()) {
            inventory.consumeFromSlot(this.selectedMeatSlot, 1);
        } else {
            // Cached slot changed; fall back to any available meat once
            int fallback = findFirstMeatSlot(inventory);
            if (fallback >= 0) {
                inventory.consumeFromSlot(fallback, 1);
            }
            // If no meat remains, the healing still applies
        }
    }

    private static int findFirstMeatSlot(@Nonnull VillagerInventory inventory) {
        for (int i = 0; i < inventory.getBackpackSize(); i++) {
            ItemStack stack = inventory.getBackpack().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Tags.Items.FOODS_RAW_MEAT) || stack.is(Tags.Items.FOODS_COOKED_MEAT)) {
                return i;
            }
        }
        return -1;
    }

}
