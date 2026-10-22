package dev.breezes.settlements.application.ai.threat;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A villager's current combat selection: which option it fights with, and which entity it is engaging.
 *
 * @param option   the selected option
 * @param targetId the hostile the selecting assessment chose to engage, or null when the option is continuing with none
 */
public record CombatSelection(CombatOption option, @Nullable UUID targetId) {

}
