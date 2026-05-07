package dev.breezes.settlements.domain.ai.observation;

/**
 * Semantic classification of an {@link Observation}, used by the importance gate to
 * determine whether an observation should be promoted to episodic memory.
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
     * A behavior completed successfully.
     */
    TASK_COMPLETION,

    /**
     * A behavior failed or could not start.
     */
    TASK_FAILURE,

    /**
     * A passive environmental change (weather, time of day, nearby structure).
     */
    ENVIRONMENT,

    /**
     * A memory fragment received from another villager during a gossip exchange.
     */
    GOSSIP_RECEIVED,
    ;

}
