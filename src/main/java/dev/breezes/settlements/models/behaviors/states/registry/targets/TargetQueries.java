package dev.breezes.settlements.models.behaviors.states.registry.targets;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.conditions.ICondition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public final class TargetQueries {

    public static Optional<Targetable> firstMatching(@Nonnull BehaviorContext context,
                                                     @Nonnull ICondition<Targetable> condition) {
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .map(targetState -> targetState.match(condition))
                .flatMap(Stream::findFirst);
    }

    public static <E extends Entity> Optional<E> firstEntity(@Nonnull BehaviorContext context,
                                                             @Nonnull EntityType<E> type,
                                                             @Nonnull Class<E> castTo) {
        ICondition<Targetable> matchesType = target -> target != null
                && target.getType() == TargetableType.ENTITY
                && target.getAsEntity().getType() == type;

        return firstMatching(context, matchesType)
                .map(Targetable::getAsEntity)
                .filter(castTo::isInstance)
                .map(castTo::cast);
    }

}
