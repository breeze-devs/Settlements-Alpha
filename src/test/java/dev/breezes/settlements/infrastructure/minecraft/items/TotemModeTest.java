package dev.breezes.settlements.infrastructure.minecraft.items;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void isAlreadyTargetType_exactlyOneModeMatches_forEveryVillagerState() {
        // Arrange
        boolean[] booleanStates = {true, false};

        // Act & Assert: a gap or an overlap in the decision table would report a villager as convertible by
        // every mode at once, or by none, so exactly one mode must claim any given state.
        for (boolean isSettlementsVillager : booleanStates) {
            for (boolean isVanillaVillagerInStasis : booleanStates) {
                long matchCount = Arrays.stream(TotemMode.values())
                        .filter(mode -> mode.isAlreadyTargetType(isSettlementsVillager, isVanillaVillagerInStasis))
                        .count();
                assertEquals(1, matchCount, "isSettlementsVillager=" + isSettlementsVillager
                        + ", isVanillaVillagerInStasis=" + isVanillaVillagerInStasis);
            }
        }
    }

    @Test
    void hue_isDistinctAcrossEveryMode() {
        // Arrange
        TotemMode[] modes = TotemMode.values();

        // Act & Assert: two modes sharing a hue would make cycling invisible in the world, which is the whole
        // point of a mode-colored outline.
        for (int i = 0; i < modes.length; i++) {
            for (int j = i + 1; j < modes.length; j++) {
                assertNotEquals(modes[i].hue(), modes[j].hue(), modes[i] + " and " + modes[j] + " share a hue");
            }
        }
    }

    @Test
    void hue_returnsIndependentInstancePerCall() {
        // Arrange
        Vector3f first = TotemMode.SETTLEMENTS.hue();

        // Act
        first.x = -1.0F;
        Vector3f second = TotemMode.SETTLEMENTS.hue();

        // Assert: a cached, shared Vector3f would let one caller's mutation corrupt every other reader of the
        // same mode's hue.
        assertNotEquals(first.x, second.x);
    }
}
