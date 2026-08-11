package dev.breezes.settlements.presentation.ui.hud;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HudPrecedenceTest {

    @Test
    void orderedByPriority_higherPriorityProviderSortsFirst() {
        // Arrange
        FakeProvider lowPriority = new FakeProvider(10);
        FakeProvider highPriority = new FakeProvider(100);

        // Act
        List<FakeProvider> ordered = HudPrecedence.orderedByPriority(List.of(lowPriority, highPriority));

        // Assert — a comparator that sorted ascending, or that skipped priority entirely, would report
        // lowPriority first.
        assertSame(highPriority, ordered.get(0));
        assertSame(lowPriority, ordered.get(1));
    }

    @Test
    void orderedByPriority_equalPriorityBreaksTieByClassName() {
        // Arrange
        FakeProvider zProvider = new ZTieProvider(50);
        FakeProvider aProvider = new ATieProvider(50);

        // Act
        List<FakeProvider> ordered = HudPrecedence.orderedByPriority(List.of(zProvider, aProvider));

        // Assert — sorting on priority alone leaves this pair's order undefined, which is exactly the
        // Set-iteration-order defect this tiebreak exists to remove.
        assertSame(aProvider, ordered.get(0));
        assertSame(zProvider, ordered.get(1));
    }

    @Test
    void firstPresent_returnsFirstNonEmptyResult_ignoringLaterOnes() {
        // Arrange
        List<String> sources = List.of("empty", "first", "second");

        // Act
        Optional<String> winner = HudPrecedence.firstPresent(sources,
                source -> "empty".equals(source) ? Optional.empty() : Optional.of(source));

        // Assert — an implementation that returned the last present result instead would report "second".
        assertEquals(Optional.of("first"), winner);
    }

    @Test
    void firstPresent_stopsResolvingOnceOneSourceAnswers() {
        // Arrange
        List<String> sources = List.of("winner", "loser");
        List<String> resolved = new ArrayList<>();

        // Act
        HudPrecedence.firstPresent(sources, source -> {
            resolved.add(source);
            return Optional.of(source);
        });

        // Assert — resolving a source that cannot win is the cost this exists to avoid; an eager
        // implementation would have asked both and still returned the same winner, hiding the waste.
        assertEquals(List.of("winner"), resolved);
    }

    @Test
    void firstPresent_allEmpty_returnsEmpty() {
        // Arrange
        List<String> sources = List.of("a", "b");

        // Act
        Optional<String> winner = HudPrecedence.firstPresent(sources, source -> Optional.empty());

        // Assert
        assertEquals(Optional.empty(), winner);
    }

    private static class FakeProvider implements HudSurfaceProvider {

        private final int priority;

        private FakeProvider(int priority) {
            this.priority = priority;
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public Optional<HudContent> contentFor(@Nonnull HudFrame frame) {
            return Optional.empty();
        }

    }

    private static final class ATieProvider extends FakeProvider {
        private ATieProvider(int priority) {
            super(priority);
        }
    }

    private static final class ZTieProvider extends FakeProvider {
        private ZTieProvider(int priority) {
            super(priority);
        }
    }

}
