package dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.EatingAnimations;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * A villager's meal, which always plays the animation.
 * <p>
 * Only consumes food if the villager requires satiation or healing.
 */
@CustomLog
public class EatFoodBehavior extends VillagerStateMachineBehavior {

    private enum EatStage implements StageKey {
        EATING,
        END;
    }

    private static final ClockTicks EATING_DURATION = ClockTicks.of(EatingAnimations.EAT_DURATION_TICKS);
    private static final ClockTicks PARTICLE_INTERVAL = ClockTicks.of(2);
    private static final ClockTicks CHEW_SOUND_INTERVAL = ClockTicks.of(4);

    /**
     * The default meal item where {@link GeneralConfig#bypassInventoryRequirements} waives food the
     * villager does not own.
     */
    private static final Item CANONICAL_BYPASS_FOOD = Items.BREAD;

    private final MealPresenter presenter;

    @Nullable
    private ItemStack selectedFood;
    private boolean shouldConsume;
    @Nullable
    private Tickable particleTimer;
    @Nullable
    private Tickable chewSoundTimer;

    public EatFoodBehavior(@Nonnull BehaviorSupport support, @Nonnull MealPresenter presenter) {
        super(log, ClockTicks.seconds(10).asTickable(), ClockTicks.seconds(20).asTickable(), support);

        this.presenter = presenter;
        this.selectedFood = null;
        this.shouldConsume = false;
        this.particleTimer = null;
        this.chewSoundTimer = null;

        this.initializeStateMachine(this.createControlStep(), EatStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("EatFoodBehavior")
                .initialStage(EatStage.EATING)
                .stageStepMap(Map.of(EatStage.EATING, this.createEatingStep()))
                .nextStage(EatStage.END)
                .build();
    }

    private TimeBasedStep<BaseVillager> createEatingStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(EATING_DURATION.asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    this.selectedFood = this.selectMeal(villager).orElse(null);
                    this.shouldConsume = this.selectedFood != null && this.eatingWouldHelp(villager);

                    if (this.selectedFood != null) {
                        villager.setHeldItem(this.selectedFood.copy());
                        villager.triggerMotion(AnimationArchetype.EAT);
                    } else {
                        // Announced now rather than at the meal's close: a player watching needs to
                        // see the villager wanting food while it is still standing there wanting it.
                        this.presenter.presentMissedMeal(villager, villager.level().getGameTime());
                    }
                    this.particleTimer = PARTICLE_INTERVAL.asTickable();
                    this.chewSoundTimer = CHEW_SOUND_INTERVAL.asTickable();
                    return StepResult.noOp();
                })
                .everyTick(context -> {
                    // The meal still plays out for its full duration with nothing to eat, just
                    // without the crumbs and chewing.
                    if (this.selectedFood == null) {
                        return StepResult.noOp();
                    }

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    Location villagerHead = Location.fromEntity(villager, true);

                    if (this.particleTimer.tickCheckAndReset(1)) {
                        villagerHead.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, this.selectedFood.copy()), 2, 0.2, 0.2, 0.2, 0.02);
                    }

                    if (this.chewSoundTimer.tickCheckAndReset(1)) {
                        villagerHead.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.0f, SoundSource.NEUTRAL);
                    }

                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    villager.setMotion(AnimationArchetype.IDLE);
                    this.finishEating(villager);
                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.setMotion(AnimationArchetype.IDLE);
        villager.clearHeldItem();
        this.selectedFood = null;
        this.shouldConsume = false;
        this.particleTimer = null;
        this.chewSoundTimer = null;
    }

    /**
     * Whether eating right now would satiate or heal the villager.
     */
    private boolean eatingWouldHelp(@Nonnull BaseVillager villager) {
        HungerConfig hungerConfig = this.getSupport().getHungerConfig();
        return villager.getHunger() < hungerConfig.eatPriorityThreshold() || villager.getHealth() < villager.getMaxHealth();
    }

    private void finishEating(@Nonnull BaseVillager villager) {
        if (this.selectedFood != null && this.shouldConsume) {
            this.swallowSelectedFood(villager);
        }

        villager.clearHeldItem();
        this.selectedFood = null;
    }

    /**
     * Applies the meal's nutrition effects.
     */
    private void swallowSelectedFood(@Nonnull BaseVillager villager) {
        if (!villager.getSettlementsInventory().consumeIfRequired(this.selectedFood, 1, GeneralConfig.bypassInventoryRequirements)) {
            log.behaviorTrace("Meal for villager {} found nothing left of its chosen food to swallow",
                    villager.getUUID());
            return;
        }

        FoodProperties food = this.selectedFood.get(DataComponents.FOOD);
        if (food != null) {
            int nutrition = food.nutrition();
            float hungerRestored = Math.min(nutrition / 20.0f, 1.0f - villager.getHunger());

            villager.setHunger(villager.getHunger() + hungerRestored);
            villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ClockTicks.seconds(nutrition * 5).getTicksAsInt(), 0, true, true));
        }

        Location villagerHead = Location.fromEntity(villager, true);
        villagerHead.playSound(SoundEvents.PLAYER_BURP, 0.5f, 1.0f, SoundSource.NEUTRAL);
        villagerHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 6, 0.2, 0.2, 0.2, 0.01);
    }

    private Optional<ItemStack> selectMeal(@Nonnull BaseVillager villager) {
        Optional<ItemStack> owned = this.findFirstFoodRepresentative(villager.getSettlementsInventory());
        if (owned.isPresent() || !GeneralConfig.bypassInventoryRequirements) {
            return owned;
        }
        return Optional.of(new ItemStack(CANONICAL_BYPASS_FOOD));
    }

    private Optional<ItemStack> findFirstFoodRepresentative(@Nonnull VillagerInventory inventory) {
        return inventory.findFirst(stack -> stack.has(DataComponents.FOOD));
    }

}
