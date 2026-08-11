package dev.breezes.settlements.infrastructure.rendering.highlight;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

import javax.annotation.Nonnull;

/**
 * Contributes zero or more highlighted entities for the current frame, sourced from wherever this provider's
 * own domain lives — a local predicate, a cached world scan, or state pushed down from the server.
 */
@ClientSide
public interface EntityHighlightProvider {

    /**
     * Cross-provider precedence when two providers highlight the same entity with different colors. Larger
     * values win.
     */
    int priority();

    void contribute(@Nonnull EntityHighlightSink sink, @Nonnull EntityHighlightFrame frame);

}
