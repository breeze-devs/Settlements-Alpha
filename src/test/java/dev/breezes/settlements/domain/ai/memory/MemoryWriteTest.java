package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MemoryWriteTest {

    @Test
    void applyToSetsPresentValue() {
        // Arrange
        MemoryType<String> type = MemoryType.<String>builder()
                .identifier("test")
                .moduleTypeSupplier(() -> null)
                .build();
        IBrain brain = mock(IBrain.class);

        // Act
        MemoryWrite.of(type, "hello").applyTo(brain);

        // Assert
        verify(brain).setMemory(type, "hello");
    }

    @Test
    void applyToClearsMissingValue() {
        // Arrange
        MemoryType<String> type = MemoryType.<String>builder()
                .identifier("test")
                .moduleTypeSupplier(() -> null)
                .build();
        IBrain brain = mock(IBrain.class);

        // Act
        MemoryWrite.clear(type).applyTo(brain);

        // Assert
        verify(brain).clearMemory(type);
    }

}
