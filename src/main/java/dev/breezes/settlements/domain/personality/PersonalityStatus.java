package dev.breezes.settlements.domain.personality;

/**
 * Lifecycle status of a villager's LLM-authored persona card ({@code VillagerPersonality}).
 */
public enum PersonalityStatus {

    /**
     * Not yet generated. The default for every old save and freshly spawned villager, and the
     * target the later retro-sweep wave scans for.
     */
    PENDING,

    /**
     * LLM characterSketch present: adjectives, characterSketch, and optional speech style were generated successfully.
     */
    READY,

    /**
     * A transient failure occurred (e.g. backend unreachable, malformed response) and the
     * villager remains eligible for retry.
     */
    FAILED,

}
