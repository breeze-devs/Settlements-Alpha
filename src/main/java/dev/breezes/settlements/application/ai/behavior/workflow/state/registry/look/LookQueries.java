package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.application.ai.socialcue.SocialCueRuntimeState;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LookQueries {

    /**
     * Resolves where the entity should look this tick using a three-tier priority order:
     * <ol>
     *   <li>Behavior's explicit {@link LookState} — set when a step wants deliberate eye contact.</li>
     *   <li>SocialCue gaze — the expression lane's gaze request, so a waving villager faces the player
     *       even while the plan runner's auto-look would normally track the nav target.</li>
     *   <li>Active navigation target — the default fallback so most behaviors face where they walk.</li>
     * </ol>
     */
    public static <T extends ISettlementsBrainEntity> Optional<Location> resolveLookLocation(@Nonnull BehaviorContext<T> context) {
        Optional<LookState> lookState = context.getState(BehaviorStateType.LOOK_TARGET, LookState.class);
        if (lookState.isPresent()) {
            return lookState.flatMap(LookState::resolveLocation);
        }

        // SocialCue gaze — available when the initiator is a BaseVillager with an active cue.
        T initiator = context.getInitiator();
        if (initiator instanceof BaseVillager baseVillager) {
            SocialCueRuntimeState cueState = baseVillager.getSocialCueRuntimeState();
            if (cueState.isCueActive() && cueState.getGazeLookTarget() != null) {
                return Optional.of(cueState.getGazeLookTarget());
            }
        }

        return TargetQueries.firstTargetLocation(context);
    }

}
