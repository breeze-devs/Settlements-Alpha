package dev.breezes.settlements.application.ai.catalog;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.catalog.PoolEntry;
import dev.breezes.settlements.domain.ai.catalog.ProfessionBehaviorPool;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a profession's available behaviors for plan generation by joining
 * the profession's {@link ProfessionBehaviorPool} with the universal pool,
 * then looking up each key's metadata from the {@link IBehaviorCatalog}.
 * <p>
 * This is the single place that combines pool (availability) + catalog (metadata) +
 * day-type filtering (scheduling policy). The heuristic and LLM planners receive a
 * clean {@code List<WeightedBehavior>} and never touch profession pools directly.
 */
@ServerScope
public class BehaviorPoolResolver {

    // Behaviors every profession inherits regardless of their pool definition.
    private static final List<PoolEntry> UNIVERSAL_ENTRIES = List.of(
            PoolEntry.of(BehaviorKey.EAT_FOOD),
            PoolEntry.of(BehaviorKey.TRADE_INITIATE),
            PoolEntry.of(BehaviorKey.TRADE_ACCEPT),
            PoolEntry.of(BehaviorKey.TAKE_FROM_CHEST)
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
    public List<WeightedBehavior> resolve(VillagerProfessionKey profession) {
        Map<BehaviorKey, Integer> weightsByKey = new LinkedHashMap<>();
        for (PoolEntry entry : this.getProfessionEntries(profession)) {
            weightsByKey.put(entry.key(), entry.weight());
        }
        for (PoolEntry entry : UNIVERSAL_ENTRIES) {
            weightsByKey.putIfAbsent(entry.key(), entry.weight());
        }

        List<WeightedBehavior> resolved = new ArrayList<>();
        for (Map.Entry<BehaviorKey, Integer> entry : weightsByKey.entrySet()) {
            this.catalog.getDescriptor(entry.getKey())
                    .map(descriptor -> new WeightedBehavior(descriptor, entry.getValue()))
                    .ifPresent(resolved::add);
        }
        return List.copyOf(resolved);
    }

    private List<PoolEntry> getProfessionEntries(VillagerProfessionKey profession) {
        ProfessionBehaviorPool pool = this.pools.get(profession);
        if (pool == null) {
            return List.of();
        }
        return pool.getEntries();
    }

}
