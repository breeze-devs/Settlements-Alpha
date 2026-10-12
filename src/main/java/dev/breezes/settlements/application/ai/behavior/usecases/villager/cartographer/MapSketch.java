package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.CartographerAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * The cartographer's map sketch as one timed step.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MapSketch {

    /**
     * Builds a behavior step where the cartographer writes content to a blank map.
     * <p>
     * A behavior stopped before the sketch ends is left holding the map in the WRITE_TO_MAP motion, so its stop hook
     * must clear both.
     *
     * @param onSketched runs once the sketch has ended and returns what follows it, as a TimeBasedStep's end hook does
     */
    static TimeBasedStep<BaseVillager> step(@Nonnull Function<BehaviorContext<BaseVillager>, StepResult> onSketched) {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(CartographerAnimations.MARK_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator();
                    villager.getNavigationManager().stop();
                    villager.setHeldItem(Items.MAP.getDefaultInstance());
                    SoundRegistry.MAP_FLAP.playGlobally(Location.fromEntity(villager, false), SoundSource.NEUTRAL);

                    villager.triggerMotion(AnimationArchetype.WRITE_TO_MAP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(CartographerAnimations.MARK_DURATION_TICKS / 2), context -> {
                    context.getInitiator().setHeldItem(Items.FILLED_MAP.getDefaultInstance());
                    SoundRegistry.MAP_SCRIBBLE.playGlobally(Location.fromEntity(context.getInitiator(), false), SoundSource.NEUTRAL);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return onSketched.apply(context);
                })
                .build();
    }

}
