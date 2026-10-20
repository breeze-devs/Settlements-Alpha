package dev.breezes.settlements.domain.generation.history;

import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The history events a settlement's generation drew, and how they changed its trait weights and its look.
 *
 * @param events the events drawn, earliest era first
 */
@Builder
public record HistoryEventResult(
        List<DrawnHistoryEvent> events,
        Map<TraitId, Float> modifiedWeights,
        VisualMarkerSet visualMarkers
) {

    public HistoryEventResult {
        events = events == null ? List.of() : List.copyOf(events);
        modifiedWeights = modifiedWeights == null ? Map.of() : new LinkedHashMap<>(modifiedWeights);
        visualMarkers = visualMarkers == null ? VisualMarkerSet.EMPTY : visualMarkers;
    }

}
