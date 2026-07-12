package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.planning.WindowPacker.PackingCandidate;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Chooses one {@link PackingCandidate} among the eligible set at a single cursor step of
 * {@link WindowPacker#pack}.
 */
@FunctionalInterface
public interface SlotSelectionStrategy {

    /**
     * @param eligible    candidates that are cooldown-clear and fit in the remaining window;
     *                    never empty — {@link WindowPacker#pack} only calls this when non-empty
     * @param emittedKeys keys already placed earlier in this window's walk
     * @param random      the walk's single seeded RNG instance; implementations that draw
     *                    randomly must draw from this rather than any shared/static source so the
     *                    whole plan's draw order stays reproducible from one seed
     * @return the chosen candidate; must be one of {@code eligible}
     */
    PackingCandidate select(List<PackingCandidate> eligible, Set<BehaviorKey> emittedKeys, Random random);

}
