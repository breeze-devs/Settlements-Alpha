package dev.breezes.settlements.domain.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeOfDayCivilAccessorTest {

    @Test
    void midnight_civilTickIsZero_minecraftTickUnchanged() {
        // Arrange, Act, Assert
        assertEquals(0, TimeOfDay.AT_00_00.getCivilTick());
        assertEquals(18_000, TimeOfDay.AT_00_00.getMinecraftTick());
    }

    @Test
    void dawn_civilTickIsSixThousand_minecraftTickUnchanged() {
        // Arrange, Act, Assert
        assertEquals(6_000, TimeOfDay.AT_06_00.getCivilTick());
        assertEquals(0, TimeOfDay.AT_06_00.getMinecraftTick());
    }

    @Test
    void sunset_civilTickIsEighteenThousand_minecraftTickUnchanged() {
        // Arrange, Act, Assert
        assertEquals(18_000, TimeOfDay.AT_18_00.getCivilTick());
        assertEquals(12_000, TimeOfDay.AT_18_00.getMinecraftTick());
    }

}
