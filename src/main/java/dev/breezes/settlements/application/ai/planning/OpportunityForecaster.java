package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.memory.SensedSiteReader;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.SensedSites;
import dev.breezes.settlements.domain.ai.planning.OpportunityProbe;
import dev.breezes.settlements.domain.ai.planning.OpportunityRequirement;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Server-thread service that evaluates each behavior's declared {@link OpportunityRequirement}s
 * against the villager's live world state and returns the set of behavior keys that lack
 * the opportunity to be productive.
 * <p>
 * Must be called on the server thread. The decaying spatial sites are read once through the shared
 * {@link SensedSiteReader}, whose live-view read performs lazy expiry (a mutation), so calling this
 * off-thread risks a ConcurrentModificationException. The result is a plain, immutable {@link Set}
 * so the generator can safely consume it from any thread.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class OpportunityForecaster {

    private final SensedSiteReader sensedSiteReader;

    /**
     * Returns the set of behavior keys from {@code pool} that lack the opportunity to be productive.
     * <p>
     * A behavior is "lacking" when ANY of its declared requirements is unavailable (AND-combined).
     * Behaviors with no declared requirements are never added to the returned set — they receive
     * the full base weight regardless of world state.
     */
    public Set<BehaviorKey> forecastLackingOpportunity(BaseVillager villager, List<WeightedBehavior> pool) {
        SensedSites sensedSites = this.sensedSiteReader.read(villager.getSettlementsBrain());
        OpportunityProbe probe = buildProbe(villager, sensedSites);
        Set<BehaviorKey> lacking = new HashSet<>();

        for (WeightedBehavior behavior : pool) {
            Set<OpportunityRequirement> requirements = behavior.descriptor().getOpportunities();
            if (requirements.isEmpty()) {
                continue;
            }

            // AND-combine: a behavior is lacking when the first unsatisfied requirement is found
            for (OpportunityRequirement requirement : requirements) {
                if (!requirement.isAvailable(probe)) {
                    lacking.add(behavior.key());
                    break;
                }
            }
        }

        return Set.copyOf(lacking);
    }

    /**
     * Constructs a live probe over the villager's inventory and job site plus the pre-read
     * {@code sensedSites} snapshot of decaying spatial memories.
     * <p>
     * All Minecraft access is scoped to this method — the probe itself is a value object
     * that can be passed to requirement logic without exposing the BaseVillager reference.
     */
    private static OpportunityProbe buildProbe(BaseVillager villager, SensedSites sensedSites) {
        return new OpportunityProbe() {

            @Override
            public boolean hasAnySite(Set<MemoryType<List<GlobalPos>>> memories) {
                for (MemoryType<List<GlobalPos>> type : memories) {
                    if (sensedSites.isPresent(type)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean holdsAnyItem(Set<Item> items) {
                for (Item item : items) {
                    if (villager.getSettlementsInventory().contains(item)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasJobSiteBlock(Block block) {
                Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                if (jobSite.isEmpty()) {
                    return false;
                }

                // Reading the block state at an unloaded job site would force a synchronous chunk load
                // during planning. Treat an unloaded (or absent) site as lacking instead.
                BlockPos jobSitePos = jobSite.get().pos();
                if (!villager.level().isLoaded(jobSitePos)) {
                    return false;
                }

                Location location = Location.of(jobSitePos, villager.level());
                return location.getBlock()
                        .map(physicalBlock -> physicalBlock.is(block))
                        .orElse(false);
            }

        };
    }

}
