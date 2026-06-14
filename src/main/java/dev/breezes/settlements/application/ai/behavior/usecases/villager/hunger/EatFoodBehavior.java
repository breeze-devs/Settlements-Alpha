package dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.EatingAnimations;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class EatFoodBehavior extends VillagerStateMachineBehavior {

    private enum EatStage implements StageKey {
        EATING,
        END;
    }

    private static final ClockTicks EATING_DURATION = ClockTicks.of(EatingAnimations.EAT_DURATION_TICKS);
    private static final ClockTicks PARTICLE_INTERVAL = ClockTicks.of(2);
    private static final ClockTicks CHEW_SOUND_INTERVAL = ClockTicks.of(4);

    @Nullable
    private ItemStack selectedFood;
    @Nullable
    private Tickable particleTimer;
    @Nullable
    private Tickable chewSoundTimer;

    public EatFoodBehavior(@Nonnull HungerConfig hungerConfig) {
        super(log, ClockTicks.seconds(10).asTickable(), ClockTicks.seconds(20).asTickable(), hungerConfig);

        this.preconditions.add(ICondition.named("HungerBelowEatThreshold",
                villager -> villager.getHunger() < hungerConfig.eatPriorityThreshold()));
        this.preconditions.add(ICondition.named("HasFood",
                villager -> villager.getSettlementsInventory().anyMatch(stack -> stack.has(DataComponents.FOOD))));
        this.preconditions.add(ICondition.named("NotSleeping", villager -> !villager.isSleeping()));

        this.selectedFood = null;
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
                    this.selectedFood = this.findFirstFoodRepresentative(villager.getSettlementsInventory()).orElse(null);
                    if (this.selectedFood == null) {
                        return StepResult.complete();
                    }

                    villager.setHeldItem(this.selectedFood.copy());
                    villager.triggerMotion(AnimationArchetype.EAT);
                    this.particleTimer = PARTICLE_INTERVAL.asTickable();
                    this.chewSoundTimer = CHEW_SOUND_INTERVAL.asTickable();
                    return StepResult.noOp();
                })
                .everyTick(context -> {
                    if (this.selectedFood == null || this.selectedFood.isEmpty()) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    Location villagerHead = Location.fromEntity(villager, true);

                    // Particle timer fires every 2 ticks for a dense item-crumb spray.
                    if (this.particleTimer.tickCheckAndReset(1)) {
                        villagerHead.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, this.selectedFood.copy()), 2, 0.2, 0.2, 0.2, 0.02);
                    }

                    // Chew sound fires every 8 ticks so it doesn't machine-gun.
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
        this.particleTimer = null;
        this.chewSoundTimer = null;
    }

    private void finishEating(@Nonnull BaseVillager villager) {
        if (this.selectedFood == null) {
            villager.clearHeldItem();
            return;
        }

        Location villagerHead = Location.fromEntity(villager, true);
        villagerHead.playSound(SoundEvents.PLAYER_BURP, 0.5f, 1.0f, SoundSource.NEUTRAL);
        villagerHead.playSound(SoundEvents.VILLAGER_YES, 0.8f, 1.0f, SoundSource.NEUTRAL);
        villagerHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 6, 0.2, 0.2, 0.2, 0.01);

        FoodProperties food = this.selectedFood.get(DataComponents.FOOD);
        if (food != null) {
            int nutrition = food.nutrition();
            float hungerRestored = Math.min(nutrition / 20.0f, 1.0f - villager.getHunger());

            villager.setHunger(villager.getHunger() + hungerRestored);
            villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ClockTicks.seconds(nutrition * 5).getTicksAsInt(), 0, true, true));
        }

        villager.getSettlementsInventory().consume(this.selectedFood, 1);
        villager.clearHeldItem();
        this.selectedFood = null;
    }

    private Optional<ItemStack> findFirstFoodRepresentative(@Nonnull VillagerInventory inventory) {
        return inventory.findFirst(stack -> stack.has(DataComponents.FOOD));
    }

}
