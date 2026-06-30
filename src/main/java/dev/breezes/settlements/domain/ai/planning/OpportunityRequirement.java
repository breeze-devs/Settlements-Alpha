package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

/**
 * A sealed declaration of what a behavior needs to be forecast as "having opportunity".
 * <p>
 * Behaviors declare zero or more requirements in their catalog {@code @Provides} methods.
 * A behavior with no requirements is never down-weighted — most of the catalog.
 * A behavior with multiple requirements is "lacking" when ANY of them is unavailable (AND-combined).
 * <p>
 * Three kinds map to the three cheap, plan-time-readable signals:
 * <ul>
 *   <li>{@link KnownSiteOpportunity} — any named memory holds a live site</li>
 *   <li>{@link InventoryItemOpportunity} — the villager holds any of the listed items</li>
 *   <li>{@link JobSiteBlockOpportunity} — the JOB_SITE block matches the declared type</li>
 * </ul>
 * Each variant self-evaluates by delegating to {@link OpportunityProbe}, keeping the
 * Minecraft-bound read confined to the forecaster's live probe.
 */
public sealed interface OpportunityRequirement permits
        OpportunityRequirement.KnownSiteOpportunity,
        OpportunityRequirement.InventoryItemOpportunity,
        OpportunityRequirement.JobSiteBlockOpportunity {

    boolean isAvailable(OpportunityProbe probe);

    /**
     * Available when any of the named memories holds at least one live site.
     * <p>
     * Works for both decaying spatial memories and vanilla list memories.
     */
    record KnownSiteOpportunity(Set<MemoryType<List<GlobalPos>>> memories) implements OpportunityRequirement {

        // Copy for immutability and reject an empty declaration: a requirement with no memories can
        // never be satisfied, so it would silently force the behavior to read as permanently lacking.
        public KnownSiteOpportunity {
            memories = Set.copyOf(memories);
            if (memories.isEmpty()) {
                throw new IllegalArgumentException("KnownSiteOpportunity requires at least one memory type");
            }
        }

        @Override
        public boolean isAvailable(OpportunityProbe probe) {
            return probe.hasAnySite(this.memories);
        }

    }

    /**
     * Available when the villager's settlements inventory contains at least one of the listed items.
     */
    record InventoryItemOpportunity(Set<Item> items) implements OpportunityRequirement {

        // Copy for immutability and reject an empty declaration: an empty item set can never be
        // satisfied, so it would silently force the behavior to read as permanently lacking.
        public InventoryItemOpportunity {
            items = Set.copyOf(items);
            if (items.isEmpty()) {
                throw new IllegalArgumentException("InventoryItemOpportunity requires at least one item");
            }
        }

        @Override
        public boolean isAvailable(OpportunityProbe probe) {
            return probe.holdsAnyItem(this.items);
        }

    }

    /**
     * Available when the villager's JOB_SITE memory points at a block matching the declared type.
     */
    record JobSiteBlockOpportunity(Block block) implements OpportunityRequirement {

        // A null job-site block can never match a real station, so it would silently read as
        // permanently lacking — reject it as the declaration bug it is (mirrors the empty-set guards).
        public JobSiteBlockOpportunity {
            if (block == null) {
                throw new IllegalArgumentException("JobSiteBlockOpportunity requires a non-null block");
            }
        }

        @Override
        public boolean isAvailable(OpportunityProbe probe) {
            return probe.hasJobSiteBlock(this.block);
        }

    }

}
