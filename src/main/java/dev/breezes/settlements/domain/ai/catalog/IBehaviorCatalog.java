package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorUiDisplayInfo;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import java.util.List;
import java.util.Optional;

/**
 * Queryable registry of all behaviors available to the planning system.
 * <p>
 * Each entry pairs a {@link BehaviorPlanningMetadata} (metadata for filtering and prompt
 * building) with a factory that produces a fresh {@link IBehavior} instance on demand.
 * The plan runner resolves behavior keys to live instances through this catalog at
 * execution time; the heuristic and LLM planners query descriptors to decide what
 * behaviors belong in a plan.
 * <p>
 * This catalog is profession-agnostic. Profession → behavior mapping lives in
 * {@link ProfessionBehaviorPool}, resolved by the application-layer {@code BehaviorPoolResolver}.
 */
public interface IBehaviorCatalog {

    /**
     * Returns the descriptor for the given key, or empty if not registered.
     */
    Optional<BehaviorPlanningMetadata> getDescriptor(BehaviorKey key);

    Optional<BehaviorUiDisplayInfo> getDisplayInfo(BehaviorKey key);

    /**
     * Creates a fresh {@link IBehavior} instance for the given behavior key.
     * <p>
     * Each call produces a new instance — behaviors are not shared between plan slots
     * or between villagers. Returns empty if the key is not registered.
     */
    Optional<IBehavior<BaseVillager>> createBehavior(BehaviorKey key);

    /**
     * Returns {@code true} if the given behavior key is registered in this catalog.
     */
    boolean exists(BehaviorKey key);

    /**
     * Returns all registered descriptors, unfiltered.
     * Use {@code BehaviorPoolResolver} to get profession-filtered lists for planning.
     */
    List<BehaviorPlanningMetadata> getAllDescriptors();

}
