package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;

/**
 * Spatial snapshot of a villager's sensed environment, sent to SIS so it can ground
 * site references in the generated lines ("the melon patch to your west").
 * <p>
 * sites maps a site-type wire token (e.g. "RIPE_MELON") to a list of [x,y,z] coord arrays.
 * Gson serializes this as {"sites":{...}}, matching SIS's SnapshotDTO exactly.
 * <p>
 * Phase 0 always sends an empty map; Phase 4 populates it from the decaying spatial memory.
 * SIS requires this field even when empty — it must serialize as {"sites":{}} not be absent.
 */
@Builder
@Getter
public final class Snapshot {

    /**
     * Site-type token → list of [x,y,z] coords arrays. Empty map is valid.
     */
    @Singular("site")
    private final Map<String, List<int[]>> sites;

}
