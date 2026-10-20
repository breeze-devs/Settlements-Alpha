package dev.breezes.settlements.application.settlement.query;

import dev.breezes.settlements.domain.settlement.model.Settlement;

/**
 * A building of a generated settlement.
 *
 * @param buildingDefinitionId the id of the building definition it was generated from
 * @param displayName          the definition's display name, or its id when no definition with that id is loaded
 * @param settlement           the settlement it belongs to
 */
public record BuildingContext(String buildingDefinitionId, String displayName, Settlement settlement) {
}
