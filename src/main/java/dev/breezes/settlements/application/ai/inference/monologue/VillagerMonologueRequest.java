package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.UUID;

/**
 * MONOLOGUE request payload for one villager sent to SIS.
 * <p>
 * Shape matches SIS VillagerRequestDTO (extra="forbid"): villagerId, persona (which carries the
 * facets and position anchors), snapshot, buckets, and the structured episodic array. Phrasing is
 * owned by SIS — this DTO ships raw semantic slots, never pre-rendered English strings.
 * <p>
 * snapshot is required by SIS even when empty and must serialize as {"sites":{}}.
 */
@Builder
@Getter
public final class VillagerMonologueRequest {

    private final UUID villagerId;
    private final PersonaBundle persona;

    /**
     * Spatial snapshot of the villager's sensed environment. Required by SIS.
     */
    private final Snapshot snapshot;

    @Singular
    private final List<OccasionBucketSpec> buckets;

    /**
     * Structured episodic memory entries derived from the villager's knowledge store.
     * Each entry carries raw semantic slots; SIS owns all phrasing.
     */
    @Singular("episodic")
    private final List<EpisodicEntryDTO> episodic;

}
