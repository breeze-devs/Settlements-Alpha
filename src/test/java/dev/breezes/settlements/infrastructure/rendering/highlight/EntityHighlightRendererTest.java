package dev.breezes.settlements.infrastructure.rendering.highlight;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class EntityHighlightRendererTest {

    @Test
    void orderedByPrecedence_higherPriorityProviderSortsFirst() {
        // Arrange
        FakeProvider lowPriority = new FakeProvider(10);
        FakeProvider highPriority = new FakeProvider(100);

        // Act
        List<EntityHighlightProvider> ordered = EntityHighlightRenderer.orderedByPrecedence(List.of(lowPriority, highPriority));

        // Assert — Set iteration order does not reliably place either provider first, so this depends
        // on the sort here rather than on incidental collection order.
        assertSame(highPriority, ordered.get(0));
        assertSame(lowPriority, ordered.get(1));
    }

    @Test
    void orderedByPrecedence_equalPriorityBreaksTieByClassName() {
        // Arrange
        FakeProvider zProvider = new ZTieProvider(50);
        FakeProvider aProvider = new ATieProvider(50);

        // Act
        List<EntityHighlightProvider> ordered = EntityHighlightRenderer.orderedByPrecedence(List.of(zProvider, aProvider));

        // Assert — sorting on priority alone leaves this pair's order undefined.
        assertSame(aProvider, ordered.get(0));
        assertSame(zProvider, ordered.get(1));
    }

    private static class FakeProvider implements EntityHighlightProvider {

        private final int priority;

        private FakeProvider(int priority) {
            this.priority = priority;
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public void contribute(@Nonnull EntityHighlightSink sink, @Nonnull EntityHighlightFrame frame) {
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
