package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LookQueries {

    /**
     * Resolves where the entity should look this tick: the explicit {@link LookState} if the behavior set
     * one, otherwise the active navigation target. This fallback is what lets most behaviors face whatever
     * they walk to without declaring a separate look target.
     */
    public static <T extends ISettlementsBrainEntity> Optional<Location> resolveLookLocation(@Nonnull BehaviorContext<T> context) {
        Optional<LookState> lookState = context.getState(BehaviorStateType.LOOK_TARGET, LookState.class);
        if (lookState.isPresent()) {
            return lookState.flatMap(LookState::resolveLocation);
        }
        return TargetQueries.firstTargetLocation(context);
    }

}
