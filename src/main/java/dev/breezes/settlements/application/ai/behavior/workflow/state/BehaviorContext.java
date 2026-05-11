package dev.breezes.settlements.application.ai.behavior.workflow.state;

import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the context of a behavior, including:
 * - the initiator (entity) of the behavior
 * - the states (facts) for the behavior
 */
public class BehaviorContext<T extends ISettlementsBrainEntity> {

    @Getter
    private final T initiator;

    private final Map<BehaviorStateType, BehaviorState> states;

    public BehaviorContext(@Nonnull T initiator) {
        this.initiator = initiator;
        this.states = new EnumMap<>(BehaviorStateType.class);
    }

    public <T extends BehaviorState> Optional<T> getState(@Nonnull BehaviorStateType type, @Nonnull Class<T> castTo) {
        if (type.getClazz() != castTo) {
            throw new IllegalArgumentException("The type of the state does not match the cast type");
        }
        return Optional.ofNullable(castTo.cast(this.states.get(type)));
    }

    public void setState(@Nonnull BehaviorStateType type, @Nonnull BehaviorState state) {
        this.states.put(type, state);
    }

    public void clearState(@Nonnull BehaviorStateType type) {
        this.states.remove(type);
    }

    public ServerLevel getLevel() {
        return (ServerLevel) this.initiator.getMinecraftEntity().level();
    }

}
