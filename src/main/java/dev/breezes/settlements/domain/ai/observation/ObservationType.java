package dev.breezes.settlements.domain.ai.observation;

/**
 * Semantic classification of an {@link Observation}, carried onto the knowledge entry it is
 * promoted into.
 */
public enum ObservationType {

    /**
     * A hostile entity, dangerous condition, or survival-critical event.
     */
    THREAT,

    /**
     * A notable item, block, or environmental resource (e.g. ripe crops, ore deposit).
     */
    RESOURCE,

    /**
     * A social event or interaction with another entity.
     */
    SOCIAL,

    /**
     * A notable mishap or secondary incident arising from an otherwise completed deed.
     */
    INCIDENT,

    /**
     * A passive environmental change (weather, time of day, nearby structure).
     */
    ENVIRONMENT,
    ;

}
