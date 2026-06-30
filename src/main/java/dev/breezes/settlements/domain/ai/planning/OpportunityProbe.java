package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

/**
 * Thin, server-thread-bound accessor over a villager's current world state.
 * <p>
 * Exists as a seam so the requirement-evaluation logic inside {@link OpportunityRequirement}
 * can be tested against a fake without loading any Minecraft objects.
 * The live implementation in {@link dev.breezes.settlements.application.ai.planning.OpportunityForecaster}
 * is the only Minecraft-bound part.
 */
public interface OpportunityProbe {

    /**
     * Returns true if any of the named memories holds at least one live site.
     * <p>
     * Implementations may touch live world state, so call only on the server thread.
     */
    boolean hasAnySite(Set<MemoryType<List<GlobalPos>>> memories);

    /**
     * Returns true if the villager's inventory contains at least one of the given items.
     */
    boolean holdsAnyItem(Set<Item> items);

    /**
     * Returns true if the villager's JOB_SITE memory points at a block matching the given type.
     * <p>
     * A leveled villager retains its profession when its station is removed, so this can
     * legitimately flip — making it a meaningful, flippable signal for plan-time forecasting.
     */
    boolean hasJobSiteBlock(Block block);

}
