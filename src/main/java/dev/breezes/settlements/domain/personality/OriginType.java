package dev.breezes.settlements.domain.personality;

/**
 * How a villager came into existence.
 * <p>
 * Deliberately survival-canonical only. {@link #UNKNOWN} is the fallback for every genesis path a
 * survival player cannot produce normally (e.g. spawn egg, commands) and for
 * villagers loaded from old saves that never had provenance captured in the first place.
 */
public enum OriginType {

    BRED,
    WORLDGEN,
    ZOMBIE_CONVERTED,
    UNKNOWN,

}
