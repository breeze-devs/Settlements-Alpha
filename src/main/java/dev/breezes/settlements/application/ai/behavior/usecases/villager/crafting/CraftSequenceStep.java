package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.crafting.catalog.CraftIngredient;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The generic craft action, run once the villager has arrived at its workstation.
 * <p>
 * A fixed {@code SequencedStep} of {@code TimeBasedStep}s cannot express this cleanly because the number
 * of input beats is dynamic (it depends on how many distinct inputs the selected recipe has). So the
 * whole commit → animate-inputs → present-output sequence is localized here, driven by a manual per-beat
 * tick counter. The batch is recomputed live on the first tick after arrival (inventory may have changed
 * since the precondition), and the economic transaction — consume inputs and bank the output — is committed
 * atomically up front, so the cosmetic beats that follow can never strand consumed inputs if interrupted.
 */
public class CraftSequenceStep extends AbstractStep<BaseVillager> {

    private static final int INPUT_BEAT_TICKS = ClockTicks.seconds(0.7).getTicksAsInt();
    private static final int OUTPUT_BEAT_TICKS = ClockTicks.seconds(0.8).getTicksAsInt();

    /**
     * At most two "fit the part in" gestures per distinct input, so a 3-stick recipe reads as work
     * without dragging the animation out proportionally to the ingredient count.
     */
    private static final int MAX_BEATS_PER_INPUT = 2;

    private static final int HELD_STACK_DISPLAY_CAP = 64;

    private final Supplier<CraftRecipe> currentRecipeSupplier;
    private final CraftBatchCalculator batchCalculator;
    private final Consumer<BaseVillager> experienceRewarder;
    private final StageKey endStage;

    private boolean prepared;
    private List<Beat> beats;
    private int beatIndex;
    private int ticksInBeat;

    @Nullable
    private Item outputItem;
    private int outputTotal;

    public CraftSequenceStep(@Nonnull String name,
                             @Nonnull Supplier<CraftRecipe> currentRecipeSupplier,
                             @Nonnull CraftBatchCalculator batchCalculator,
                             @Nonnull Consumer<BaseVillager> experienceRewarder,
                             @Nonnull StageKey endStage) {
        super(name);
        this.currentRecipeSupplier = currentRecipeSupplier;
        this.batchCalculator = batchCalculator;
        this.experienceRewarder = experienceRewarder;
        this.endStage = endStage;
        this.resetRunState();
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
        if (!this.prepared) {
            StepResult preparation = this.prepare(context);
            if (preparation != null) {
                return preparation;
            }
            this.prepared = true;
        }

        if (this.beatIndex >= this.beats.size()) {
            return this.finish(context);
        }

        Beat beat = this.beats.get(this.beatIndex);
        if (this.ticksInBeat == 0) {
            beat.action().accept(context);
        }

        this.ticksInBeat++;
        if (this.ticksInBeat >= beat.holdTicks()) {
            this.beatIndex++;
            this.ticksInBeat = 0;
        }
        return StepResult.noOp();
    }

    /**
     * Recomputes the batch live, consumes the inputs, and lays out the beat plan. Returns a terminal
     * {@link StepResult} to short-circuit (missing recipe or no-longer-craftable), or {@code null} to
     * proceed into the beats.
     */
    @Nullable
    private StepResult prepare(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        CraftRecipe recipe = this.currentRecipeSupplier.get();
        if (recipe == null) {
            return StepResult.transition(this.endStage);
        }

        VillagerInventory inventory = villager.getSettlementsInventory();

        // Capture a representative held-item stack per input before draining, so the input beats can
        // display the actual item (including the concrete variant chosen for a tag input). Must happen
        // before the commit below, which drains the inputs.
        List<ItemStack> inputVisuals = new ArrayList<>(recipe.inputs().size());
        for (CraftIngredient input : recipe.inputs()) {
            inputVisuals.add(inventory.findFirst(stack -> ItemMatches.test(input.match(), stack)).orElse(ItemStack.EMPTY));
        }

        Optional<RecipeCommit.Result> committed = RecipeCommit.commit(context, recipe, this.batchCalculator,
                WorldEventType.GOODS_CRAFTED, this.experienceRewarder);
        if (committed.isEmpty()) {
            // Inventory changed since the precondition check — end gracefully rather than crafting nothing.
            return StepResult.transition(this.endStage);
        }
        this.outputItem = committed.get().outputItem();
        this.outputTotal = committed.get().outputTotal();

        this.beats = new ArrayList<>();
        for (int i = 0; i < recipe.inputs().size(); i++) {
            CraftIngredient input = recipe.inputs().get(i);
            ItemStack visual = inputVisuals.get(i);
            int beatCount = Math.min(input.count(), MAX_BEATS_PER_INPUT);
            for (int b = 0; b < beatCount; b++) {
                this.beats.add(new Beat(INPUT_BEAT_TICKS, ctx -> this.playInputBeat(ctx, visual)));
            }
        }
        this.beats.add(new Beat(OUTPUT_BEAT_TICKS, this::playOutputBeat));
        return null;
    }

    private void playInputBeat(@Nonnull BehaviorContext<BaseVillager> context, @Nonnull ItemStack visual) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        if (!visual.isEmpty()) {
            villager.setHeldItem(visual);
        }
        villager.triggerMotion(AnimationArchetype.INTERACT);
        Location.fromEntity(villager, true).displayParticles(ParticleTypes.CRIT, 4, 0.2, 0.2, 0.2, 0.05);
    }

    private void playOutputBeat(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        villager.setHeldItem(new ItemStack(this.requireOutputItem(), Math.min(this.outputTotal, HELD_STACK_DISPLAY_CAP)));
        villager.triggerMotion(AnimationArchetype.INTERACT);

        Location origin = Location.fromEntity(villager, true);
        origin.displayParticles(ParticleTypes.HAPPY_VILLAGER, 7, 0.3, 0.3, 0.3, 0.1);
        SoundRegistry.ITEM_POP_OUT.playGlobally(origin, SoundSource.BLOCKS);
    }

    private StepResult finish(@Nonnull BehaviorContext<BaseVillager> context) {
        // The craft was already committed in prepare(); the beats are cosmetic, so this just ends cleanly.
        context.getInitiator().getMinecraftEntity().clearHeldItem();
        return StepResult.complete();
    }

    private Item requireOutputItem() {
        if (this.outputItem == null) {
            throw new IllegalStateException("CraftSequenceStep output item accessed before preparation");
        }
        return this.outputItem;
    }

    @Override
    protected void doOnEnter() {
        // A behavior run enters this step exactly once; clear all run state so a reused instance starts
        // fresh (per-run reset routes through onEnter, so this covers both entry and run boundaries).
        this.resetRunState();
    }

    private void resetRunState() {
        this.prepared = false;
        this.beats = List.of();
        this.beatIndex = 0;
        this.ticksInBeat = 0;
        this.outputItem = null;
        this.outputTotal = 0;
    }

    private record Beat(int holdTicks, Consumer<BehaviorContext<BaseVillager>> action) {
    }

}
