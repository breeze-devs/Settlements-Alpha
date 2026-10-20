package dev.breezes.settlements.domain.generation.model;

import dev.breezes.settlements.domain.generation.history.HistoryEventResult;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;

/**
 * A settlement's character, decided from its site before anything is built for it.
 * The profile's adjusted weights are the weights its drawn history left.
 */
public record SettlementCharacter(
        SiteReport siteReport,
        SettlementProfile profile,
        HistoryEventResult history
) {
}
