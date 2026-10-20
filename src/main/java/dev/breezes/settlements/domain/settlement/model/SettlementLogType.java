package dev.breezes.settlements.domain.settlement.model;

/**
 * What a settlement log entry records.
 */
public enum SettlementLogType {

    /**
     * The settlement was founded.
     */
    FOUNDED,

    /**
     * The settlement took a new name.
     */
    RENAMED,

    /**
     * A villager joined or rejoined the living roster.
     * <p>
     * A founding member's entry has the detail{@link #FOUNDER_DETAIL}.
     */
    JOINED,

    /**
     * A member was born.
     */
    BORN,

    /**
     * A member died.
     */
    DIED,

    /**
     * A member was turned into a zombie villager, ceasing to be a living member.
     */
    ZOMBIFIED,

    /**
     * A zombified member was cured and became a living member again.
     */
    CURED,

    /**
     * A member was struck by lightning and became a witch, ceasing to be a living member.
     */
    BECAME_WITCH,

    /**
     * A villager left the living roster, including conversion into a vanilla villager.
     */
    LEFT,

    /**
     * The settlement became unhealthy and began to fade.
     */
    BEGAN_FADING,

    /**
     * A fading settlement became healthy again.
     */
    REVIVED;

    /**
     * Marks a {@link #JOINED} entry as that of a villager the settlement was founded with.
     */
    public static final String FOUNDER_DETAIL = "founder";

}
