package dev.breezes.settlements.application.ai.threat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A villager's current combat selection: which option it fights with, and which entity it is engaging.
 *
 * @param option   the selected option
 * @param targetId the nearest engageable sighted hostile the selecting assessment found, or null when the
 *                 option is continuing with none currently in sight
 */
public record CombatSelection(@Nonnull CombatOption option, @Nullable UUID targetId) {

}
