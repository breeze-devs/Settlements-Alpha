package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.application.ai.threat.IThreatResponse;
import dev.breezes.settlements.domain.ai.brain.IBrain;

/**
 * A villager's brain: the shared brain contract plus the cognition only villagers have.
 */
public interface IVillagerBrain extends IBrain {

    IThreatResponse threats();

}
