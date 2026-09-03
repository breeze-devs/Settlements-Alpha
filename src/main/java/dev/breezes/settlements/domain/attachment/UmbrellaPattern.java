package dev.breezes.settlements.domain.attachment;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * The umbrella skins a villager can carry.
 */
public enum UmbrellaPattern {

    BLACK,
    BLUE,
    BROWN,
    CYAN,
    GRAY,
    GREEN,
    LIGHT_BLUE,
    LIGHT_GRAY,
    LIME,
    MAGENTA,
    ORANGE,
    PINK,
    PURPLE,
    RED,
    WHITE,
    YELLOW;

    private static final UmbrellaPattern[] VALUES = values();

    /**
     * Picks the skin a villager carries. Stable for a given villager.
     */
    public static UmbrellaPattern forVillager(@Nonnull UUID uuid) {
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        return VALUES[Math.floorMod(mixed, VALUES.length)];
    }

}
