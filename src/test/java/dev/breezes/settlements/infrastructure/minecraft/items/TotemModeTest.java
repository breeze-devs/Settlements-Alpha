package dev.breezes.settlements.infrastructure.minecraft.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotemModeTest {

    @Test
    void fromSerializedIdReturnsMatchingMode() {
        // Arrange, Act, Assert
        assertEquals(TotemMode.VANILLA, TotemMode.fromSerializedId(0));
        assertEquals(TotemMode.SETTLEMENTS, TotemMode.fromSerializedId(1));
        assertEquals(TotemMode.STASIS, TotemMode.fromSerializedId(2));
    }

    @Test
    void fromSerializedIdReturnsDefaultForUnknownId() {
        // Arrange, Act, Assert
        assertEquals(TotemMode.SETTLEMENTS, TotemMode.fromSerializedId(-1));
        assertEquals(TotemMode.SETTLEMENTS, TotemMode.fromSerializedId(99));
    }

    @Test
    void nextCyclesThroughAllModes() {
        // Arrange, Act, Assert
        assertEquals(TotemMode.SETTLEMENTS, TotemMode.VANILLA.next());
        assertEquals(TotemMode.STASIS, TotemMode.SETTLEMENTS.next());
        assertEquals(TotemMode.VANILLA, TotemMode.STASIS.next());
    }

    @Test
    void translationKeyBelongsToMode() {
        // Arrange, Act, Assert
        assertEquals("item.settlements.villager_totem.mode.vanilla", TotemMode.VANILLA.getTranslationKey());
        assertEquals("item.settlements.villager_totem.mode.settlements", TotemMode.SETTLEMENTS.getTranslationKey());
        assertEquals("item.settlements.villager_totem.mode.stasis", TotemMode.STASIS.getTranslationKey());
    }

    @Test
    void alreadyTargetTypeUsesModeIntent() {
        // Arrange, Act, Assert
        assertTrue(TotemMode.VANILLA.isAlreadyTargetType(false, false));
        assertFalse(TotemMode.VANILLA.isAlreadyTargetType(false, true));
        assertFalse(TotemMode.VANILLA.isAlreadyTargetType(true, false));

        assertTrue(TotemMode.SETTLEMENTS.isAlreadyTargetType(true, false));
        assertFalse(TotemMode.SETTLEMENTS.isAlreadyTargetType(false, false));
        assertFalse(TotemMode.SETTLEMENTS.isAlreadyTargetType(false, true));

        assertFalse(TotemMode.STASIS.isAlreadyTargetType(true, false));
        assertFalse(TotemMode.STASIS.isAlreadyTargetType(false, false));
        assertTrue(TotemMode.STASIS.isAlreadyTargetType(false, true));
    }
}
