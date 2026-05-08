package dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class EatFoodBehavior extends StateMachineBehavior {

    private enum EatStage implements StageKey {
        EATING,
        END;
    }

    private static final ClockTicks EATING_DURATION = ClockTicks.seconds(3);
    private static final ClockTicks PARTICLE_MAX_INTERVAL = ClockTicks.of(12);
    private static final ClockTicks PARTICLE_MIN_INTERVAL = ClockTicks.of(8);

    @Nullable
    private ItemStack selectedFood;
    private int selectedFoodSlot;
    @Nullable
    private RandomRangeTickable particleTimer;

    public EatFoodBehavior(@Nonnull HungerConfig hungerConfig) {
        super(log, ClockTicks.seconds(10).asTickable(), ClockTicks.seconds(20).asTickable(), hungerConfig);

        this.preconditions.add(villager -> villager.getHunger() < hungerConfig.eatPriorityThreshold());
        this.preconditions.add(villager -> this.findFirstFoodSlot(villager.getSettlementsInventory()) >= 0);
        this.preconditions.add(villager -> !villager.isSleeping());

        this.selectedFoodSlot = -1;
        this.selectedFood = null;
        this.particleTimer = null;

        this.initializeStateMachine(this.createControlStep(), EatStage.END);
    }

    private StagedStep createControlStep() {
        return StagedStep.builder()
                .name("EatFoodBehavior")
                .initialStage(EatStage.EATING)
                .stageStepMap(Map.of(EatStage.EATING, this.createEatingStep()))
                .nextStage(EatStage.END)
                .build();
    }

    private TimeBasedStep createEatingStep() {
        return TimeBasedStep.builder()
                .withTickable(EATING_DURATION.asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    this.selectedFoodSlot = this.findFirstFoodSlot(villager.getSettlementsInventory());
                    if (this.selectedFoodSlot < 0) {
                        return StepResult.complete();
                    }

                    this.selectedFood = villager.getSettlementsInventory().getBackpack().getItem(this.selectedFoodSlot).copyWithCount(1);
                    villager.setHeldItem(this.selectedFood.copy());
                    this.particleTimer = RandomRangeTickable.of(PARTICLE_MAX_INTERVAL, PARTICLE_MIN_INTERVAL);
                    return StepResult.noOp();
                })
                .everyTick(context -> {
                    if (this.selectedFood == null || this.selectedFood.isEmpty()) {
                        return StepResult.complete();
                    }

                    if (this.particleTimer == null) {
                        this.particleTimer = RandomRangeTickable.of(PARTICLE_MAX_INTERVAL, PARTICLE_MIN_INTERVAL);
                    }
                    if (!this.particleTimer.tickAndCheck(1)) {
                        return StepResult.noOp();
                    }

                    this.particleTimer.reset();

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    Location villagerHead = Location.fromEntity(villager, true);
                    villagerHead.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.0f, SoundSource.NEUTRAL);
                    villagerHead.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, this.selectedFood.copy()), 6, 0.2, 0.2, 0.2, 0.02);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    this.finishEating(villager);
                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        this.selectedFood = null;
        this.selectedFoodSlot = -1;
        this.particleTimer = null;
    }

    private void finishEating(@Nonnull BaseVillager villager) {
        if (this.selectedFood == null || this.selectedFoodSlot < 0) {
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
            float healAmount = 2.0f + (nutrition * 0.5f);

            villager.setHunger(villager.getHunger() + hungerRestored);
            villager.heal(healAmount);
        }

        villager.getSettlementsInventory().consumeFromSlot(this.selectedFoodSlot, 1);
        villager.clearHeldItem();
        this.selectedFood = null;
        this.selectedFoodSlot = -1;
    }

    private int findFirstFoodSlot(@Nonnull VillagerInventory inventory) {
        for (int i = 0; i < inventory.getBackpackSize(); i++) {
            ItemStack stack = inventory.getBackpack().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }

}
