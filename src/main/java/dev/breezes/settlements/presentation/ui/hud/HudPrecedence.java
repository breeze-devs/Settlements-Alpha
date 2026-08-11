package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Orders a layer's providers by declared priority, and resolves the ordered set down to a single winner.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class HudPrecedence {

    static <T extends HudSurfaceProvider> List<T> orderedByPriority(@Nonnull Collection<T> providers) {
        return providers.stream()
                .sorted(Comparator.comparingInt(HudSurfaceProvider::priority)
                        .reversed()
                        .thenComparing(provider -> provider.getClass().getName()))
                .toList();
    }

    /**
     * The first source resolver produces a result for, leaving every later source unasked.
     * <p>
     * Taking a resolver rather than a list of already-resolved results is what makes that laziness possible, and
     * resolving is the expensive half — a losing source must not pay for a result nothing will draw.
     */
    static <T, R> Optional<R> firstPresent(@Nonnull List<T> orderedSources, @Nonnull Function<T, Optional<R>> resolver) {
        for (T source : orderedSources) {
            Optional<R> result = resolver.apply(source);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

}
