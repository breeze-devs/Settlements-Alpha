package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Structured episodic memory entry sent to SIS in MONOLOGUE requests.
 * <p>
 * SIS owns all phrasing — this DTO ships raw semantic slots (who, what perspective, outcome)
 * so the language model can render them in first or third person as appropriate.
 * <p>
 * Null fields are omitted from the wire representation by Gson's default serializer behavior,
 * matching the SIS VillagerEpisodicEntryDTO (extra="forbid") contract. The primitive
 * {@link #ageTicks} is always present.
 */
@Builder
@Getter
public final class EpisodicEntryDTO {

    /**
     * The raw WorldEventType name, e.g. "RESOURCE_HARVESTED". SIS owns all phrasing.
     */
    private final String eventType;

    /**
     * One of "FIRST_HAND_PARTICIPANT" or "FIRST_HAND_BYSTANDER".
     * Controls whether SIS renders first-person or third-person prose for this entry.
     */
    private final String perspective;

    /**
     * Resolved display name of the actor. Null for FIRST_HAND_PARTICIPANT because SIS
     * renders "I" from the villager's own persona — sending a name would conflict.
     */
    @Nullable
    private final String actor;

    /**
     * Resolved display name of the target villager. Present only for social events where
     * relatedEntity is confirmed to be a nameable villager partner (trade/courtship).
     * <p>
     * Null for all other event types — relatedEntity may be a non-person UUID.
     */
    @Nullable
    private final String target;

    /**
     * The EventOutcome name ("SUCCESS" or "FAILURE") if recorded by the originating behavior.
     * Null when no structured outcome was set.
     */
    @Nullable
    private final String outcome;

    /**
     * Free-text reason fragment set by the behavior, meaningful when outcome is FAILURE.
     * Null when no reason was recorded.
     */
    @Nullable
    private final String reason;

    /**
     * Structured detail map for LLM phrasing. Keys are camelCase slot names from the stable
     * vocabulary (e.g. {@code "item"}, {@code "count"}, {@code "price"}).
     * Null when no structured detail was recorded — Gson's default serializer omits null fields,
     * keeping the wire payload compact and matching the SIS {@code extra="forbid"} contract.
     */
    @Nullable
    private final Map<String, String> detail;

    /**
     * Floored block coordinates of the event origin as {@code [x, y, z]} in Minecraft world axes
     * (+x east, +z south, y up), mirroring the snapshot's site-coord representation so SIS can
     * render spatial direction ("a zombie to the south-east").
     * <p>
     * Null — and thus omitted by Gson — only when position metadata is absent; never sent partial
     * or zeroed.
     */
    @Nullable
    private final int[] pos;

    /**
     * Age of this entry in game ticks (currentTick - admittedAtTick), clamped to >= 0.
     * SIS uses this to compute recency adverbs ("moments ago", "earlier today").
     */
    private final long ageTicks;

}
