package dev.breezes.settlements.domain.ai.perception;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record PerceivedEntities(@Nonnull List<SensedEntity> entities) {

    private static final PerceivedEntities EMPTY = new PerceivedEntities(List.of());

    public PerceivedEntities {
        entities = List.copyOf(entities);
    }

    public static PerceivedEntities empty() {
        return EMPTY;
    }

    public <E extends Entity> Stream<E> ofType(@Nonnull Class<E> type, @Nonnull Predicate<E> filter) {
        return this.entities.stream()
                .map(SensedEntity::entity)
                .filter(type::isInstance)
                .map(type::cast)
                .filter(filter);
    }

    public <E extends Entity> Optional<E> closest(@Nonnull Class<E> type,
                                                  @Nonnull Predicate<E> filter,
                                                  @Nonnull Entity from) {
        // Sort by distance before applying the predicate so an expensive predicate (e.g. a reachability
        // pathfind) is evaluated nearest-first and short-circuits at the first match. Stream#min would
        // instead force the predicate against every candidate.
        return this.entities.stream()
                .map(SensedEntity::entity)
                .filter(type::isInstance)
                .map(type::cast)
                .sorted(Comparator.comparingDouble(from::distanceToSqr))
                .filter(filter)
                .findFirst();
    }

}
