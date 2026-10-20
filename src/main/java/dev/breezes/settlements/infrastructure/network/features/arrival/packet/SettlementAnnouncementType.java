package dev.breezes.settlements.infrastructure.network.features.arrival.packet;

import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementRegionCause;

import javax.annotation.Nonnull;

public enum SettlementAnnouncementType {

    // Sent by ordinal: reordering the constants changes the wire format, which needs a network version bump

    /**
     * The player entered a settlement not discovered before.
     */
    DISCOVERY,

    /**
     * A settlement was founded around the player.
     */
    FOUNDED,

    /**
     * The player renamed the settlement.
     */
    RENAMED,

    /**
     * The player entered a settlement discovered before.
     */
    ENTRY,

    /**
     * The player left a settlement.
     */
    EXIT,

    /**
     * The settlement the player was in disbanded.
     */
    DISBANDED;

    /**
     * Returns the moment of a player entering a settlement.
     *
     * @param discovered whether the player had discovered the settlement before entering it
     */
    public static SettlementAnnouncementType ofEntry(@Nonnull SettlementRegionCause cause, boolean discovered) {
        if (cause == SettlementRegionCause.FOUNDED) {
            return FOUNDED;
        }
        return discovered ? ENTRY : DISCOVERY;
    }

    /**
     * Returns the moment of a player leaving a settlement.
     */
    public static SettlementAnnouncementType ofExit(@Nonnull SettlementRegionCause cause) {
        return cause == SettlementRegionCause.DISBANDED ? DISBANDED : EXIT;
    }

}
