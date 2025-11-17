package dev.breezes.settlements.models.behaviors.states;

import dev.breezes.settlements.entities.villager.ISettlementsVillager;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the context of a behavior, including:
 * - the initiator (entity) of the behavior
 * - the states (facts) for the behavior
 */
public class BehaviorContext {

    @Getter
    private final ISettlementsVillager initiator;

    private final Map<BehaviorStateType, BehaviorState> states;

    public BehaviorContext(@Nonnull ISettlementsVillager initiator) {
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

}
