package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class NavigateToTargetStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    /**
     * Default nav clamp: a navigation that has not arrived within this window is treated as a
     * failed approach. This catches the dynamic case where a target becomes blocked mid-route
     * or the villager oscillates just outside the arrival threshold
     */
    private static final int DEFAULT_NAV_TIMEOUT_TICKS = ClockTicks.seconds(30).getTicksAsInt();

    private final NavigationType navigationType;
    private final int completionDistance;

    /**
     * Optional recovery stage to transition to when the target is detected unreachable or the
     * per-step nav timeout expires. {@code null} = clean fail (behavior stops)
     */
    @Nullable
    private final StageKey unreachableTransition;

    /**
     * When {@code true}, a reachability check is performed before issuing navigation.
     * <p>
     * Disabled for best-effort wander steps where we want the villager to attempt the move
     * even if pathfinding cannot guarantee success
     */
    private final boolean reachabilityGated;

    /**
     * Convenience constructor with the default clamp, reachability check, and a clean fail on timeout
     */
    public NavigateToTargetStep(@Nonnull NavigationType navigationType, int completionDistance) {
        this(navigationType, completionDistance, null, null);
    }

    @Builder
    public NavigateToTargetStep(@Nonnull NavigationType navigationType,
                                int completionDistance,
                                @Nullable StageKey unreachableTransition,
                                @Nullable Boolean reachabilityGated) {
        super("NavigateToTargetStep", DEFAULT_NAV_TIMEOUT_TICKS, unreachableTransition);
        this.navigationType = navigationType;
        this.completionDistance = completionDistance;
        this.unreachableTransition = unreachableTransition;
        // Gating is enabled by default
        this.reachabilityGated = reachabilityGated == null || reachabilityGated;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        T initiator = context.getInitiator();

        // Navigation already in progress — nothing to do this tick
        if (initiator.getNavigationManager().isNavigating()) {
            return StepResult.noOp();
        }

        Optional<Location> target = context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation);
        if (target.isEmpty()) {
            return StepResult.noOp();
        }

        // Proactive abort: reject statically-unreachable targets before navigating
        // This runs only once per acquisition (guarded by the isNavigating() check above),
        // so we spend at most one pathfind per navigation start, never per tick
        if (this.reachabilityGated && !initiator.getNavigationManager().canReach(target.get(), this.completionDistance)) {
            return this.unreachableTransition != null
                    ? StepResult.transition(this.unreachableTransition)
                    : StepResult.fail("unreachable");
        }

        initiator.getNavigationManager().navigateTo(target.get(), this.navigationType, this.completionDistance);
        return StepResult.noOp();
    }

}
