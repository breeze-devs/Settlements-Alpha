package dev.breezes.settlements.application.ai.catalog;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.catalog.PoolEntry;
import dev.breezes.settlements.domain.ai.catalog.ProfessionBehaviorPool;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves a profession's available behaviors for plan generation by joining
 * the profession's {@link ProfessionBehaviorPool} with the universal pool,
 * then looking up each key's metadata from the {@link IBehaviorCatalog}.
 * <p>
 * This is the single place that combines pool (availability) + catalog (metadata) +
 * day-type filtering (scheduling policy). The heuristic and LLM planners receive a
 * clean {@code List<BehaviorPlanningMetadata>} and never touch profession pools directly.
 */
@ServerScope
public class BehaviorPoolResolver {

    // Behaviors every profession inherits regardless of their pool definition.
    private static final List<BehaviorKey> UNIVERSAL_KEYS = List.of(
            BehaviorKey.EAT_FOOD,
            BehaviorKey.TRADE_INITIATE,
            BehaviorKey.TRADE_ACCEPT
    );

    private final IBehaviorCatalog catalog;
    private final Map<VillagerProfessionKey, ProfessionBehaviorPool> pools;

    @Inject
    public BehaviorPoolResolver(IBehaviorCatalog catalog, Set<ProfessionBehaviorPool> pools) {
        this.catalog = catalog;
        this.pools = pools.stream()
                .collect(Collectors.toUnmodifiableMap(ProfessionBehaviorPool::getProfession, p -> p));
    }

    /**
     * Returns all behaviors available to the given profession — profession-specific pool merged
     * with universals, resolved to descriptors. No day-type filtering is applied here; the
     * planner's {@code RestDayPolicy} multipliers handle rest-day weighting downstream.
     */
    public List<BehaviorPlanningMetadata> resolve(VillagerProfessionKey profession) {
        List<BehaviorKey> professionKeys = this.getProfessionKeys(profession);
        List<BehaviorKey> allKeys = Stream.concat(professionKeys.stream(), UNIVERSAL_KEYS.stream())
                .distinct()
                .toList();

        List<BehaviorPlanningMetadata> resolved = new ArrayList<>();
        for (BehaviorKey key : allKeys) {
            this.catalog.getDescriptor(key).ifPresent(resolved::add);
        }
        return List.copyOf(resolved);
    }

    private List<BehaviorKey> getProfessionKeys(VillagerProfessionKey profession) {
        ProfessionBehaviorPool pool = this.pools.get(profession);
        if (pool == null) {
            return List.of();
        }
        return pool.getEntries().stream()
                .map(PoolEntry::key)
                .toList();
    }

}
