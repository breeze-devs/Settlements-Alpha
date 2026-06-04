package dev.breezes.settlements.application.ai.courtship;

/**
 * Shared physical constants for the courtship system.
 * Distances are hardcoded per project convention — these are NOT config knobs.
 */
public final class CourtshipConstants {

    /**
     * How close the two participants must be before approach is considered complete.
     */
    public static final double CLOSE_ENOUGH_DISTANCE = 1.0;

    /**
     * Maximum separation tolerated during the courtship dance before re-approach triggers.
     */
    public static final double DRIFT_DISTANCE = 2.0;

    /**
     * Ticks the receiver waits after a beat before mirroring, so the response reads as
     * an answer rather than a simultaneous lock-step copy.
     */
    public static final long RECEIVER_REACTION_DELAY_TICKS = 8L;

    /**
     * Breed cooldown applied to both parents after a successful birth (6000 ticks = 5 minutes).
     */
    public static final int BREED_COOLDOWN_TICKS = 6000;

}
