package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Persona position anchors sent to SIS so it can ground spatial references in the
 * generated lines ("the wheat patch west of you").
 * <p>
 * Represented as int[] so Gson serializes each coordinate as a JSON array [x,y,z].
 * jobSite and home are nullable: Gson's default behavior omits null fields, which is
 * exactly what the SIS AnchorsDTO expects for optional positions.
 */
@Builder
@Getter
public final class Anchors {

    /**
     * The villager's current block position. Always present.
     */
    private final int[] body;

    /**
     * The villager's job-site block position. Absent when the villager has no job site.
     */
    @Nullable
    private final int[] jobSite;

    /**
     * The villager's bed/home block position. Absent when the villager has no assigned bed.
     */
    @Nullable
    private final int[] home;

}
