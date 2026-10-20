package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;

/**
 * Whether this client shows settlement titles. Read on the client, so each player's own file decides.
 */
@BehaviorConfig(name = "settlement_titles", type = ConfigurationType.FEATURE)
public record SettlementTitleConfig(
        @BooleanConfig(
                type = ConfigurationType.FEATURE,
                identifier = "enabled",
                description = "Whether this client shows settlement titles and plays their sounds: discovering, " +
                        "entering, and leaving a settlement, founding or renaming one, and one falling into ruin " +
                        "around the player. Each player's own file decides; a dedicated server ignores this setting.",
                defaultValue = true)
        boolean enabled
) {

}
