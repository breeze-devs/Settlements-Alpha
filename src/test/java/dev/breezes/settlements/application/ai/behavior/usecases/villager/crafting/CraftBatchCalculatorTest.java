package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the pure batch-clamp arithmetic (no Minecraft objects). Covers the error-prone divisor
 * terms: each headroom is divided by its own per-unit cost, and a non-positive headroom reports 0.
 */
class CraftBatchCalculatorTest {

    @Test
    void expertiseIsTheCeilingWhenSuppliesAreAmple() {
        // Arrange: plenty of every input, output far below its ceiling
        int expertise = 3;
        int outputHeadroom = 200;
        int outputCount = 1;
        int[] inputHeadrooms = {64, 64};
        int[] inputCounts = {3, 2};

        // Act
        int batch = CraftBatchCalculator.clampBatch(expertise, outputHeadroom, outputCount, inputHeadrooms, inputCounts);

        // Assert
        assertEquals(3, batch);
    }

    @Test
    void inputDivisorFloorsBatchByPerUnitCost() {
        // Arrange: 7 usable sticks at 3 per craft floors the batch to 2 despite high expertise
        int expertise = 5;
        int outputHeadroom = 200;
        int outputCount = 1;
        int[] inputHeadrooms = {7};
        int[] inputCounts = {3};

        // Act
        int batch = CraftBatchCalculator.clampBatch(expertise, outputHeadroom, outputCount, inputHeadrooms, inputCounts);

        // Assert
        assertEquals(2, batch);
    }

    @Test
    void outputCountDividesTheCeilingHeadroom() {
        // Arrange: only 20 units of ceiling headroom for an 8-per-craft output caps the batch at 2
        int expertise = 5;
        int outputHeadroom = 20;
        int outputCount = 8;
        int[] inputHeadrooms = {64};
        int[] inputCounts = {1};

        // Act
        int batch = CraftBatchCalculator.clampBatch(expertise, outputHeadroom, outputCount, inputHeadrooms, inputCounts);

        // Assert
        assertEquals(2, batch);
    }

    @Test
    void reserveEatingIntoStockYieldsZero() {
        // Arrange: input headroom below one craft's cost (reserve consumed the surplus)
        int expertise = 4;
        int outputHeadroom = 100;
        int outputCount = 1;
        int[] inputHeadrooms = {2};
        int[] inputCounts = {3};

        // Act
        int batch = CraftBatchCalculator.clampBatch(expertise, outputHeadroom, outputCount, inputHeadrooms, inputCounts);

        // Assert
        assertEquals(0, batch);
    }

    @Test
    void outputAtOrAboveCeilingYieldsZero() {
        // Arrange: negative ceiling headroom (already at/over the overflow line) is not craftable
        int expertise = 5;
        int outputHeadroom = -4;
        int outputCount = 1;
        int[] inputHeadrooms = {64};
        int[] inputCounts = {1};

        // Act
        int batch = CraftBatchCalculator.clampBatch(expertise, outputHeadroom, outputCount, inputHeadrooms, inputCounts);

        // Assert
        assertEquals(0, batch);
    }

}
