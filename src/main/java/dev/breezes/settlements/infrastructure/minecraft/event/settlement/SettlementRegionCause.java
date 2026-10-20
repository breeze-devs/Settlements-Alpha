package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

/**
 * Why a player's settlement region changed.
 */
public enum SettlementRegionCause {

    /**
     * The player moved: a step, a teleport, a respawn, a dimension change, or a login.
     */
    MOVED,

    /**
     * A settlement was founded around the player.
     */
    FOUNDED,

    /**
     * A generated settlement's record was stored around the player.
     */
    RECORDED,

    /**
     * The settlement the player was in disbanded.
     */
    DISBANDED

}
