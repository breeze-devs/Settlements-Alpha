package dev.breezes.settlements.application.ai.dialogue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Low-cardinality scripted dialogue facets that may earn hand-authored line pools.
 */
@AllArgsConstructor
@Getter
public enum DialogueFacet {

    WAS_CURED("cured");

    private final String keySegment;

}
