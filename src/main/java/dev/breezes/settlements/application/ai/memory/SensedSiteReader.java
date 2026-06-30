package dev.breezes.settlements.application.ai.memory;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.SensedSites;
import dev.breezes.settlements.domain.ai.memory.SiteCoord;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read facade over a villager's spatial site memory stores.
 * <p>
 * Must be called on the server thread. {@code DecayingSpatialSiteStore} is not thread-safe:
 * the {@code getMemory} live-view path performs lazy-expiry removal (a mutation) during the read,
 * so calling this off-thread risks concurrent modification. All current consumers — Phase 3
 * day-planner and Phase 4 snapshot assembler — run on the server thread before any worker
 * hand-off, so no synchronization is introduced here.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SensedSiteReader {

    /**
     * Reads all spatial site memories from the given brain and projects them to {@link SiteCoord} data.
     * The dimension key carried by {@code GlobalPos} is discarded at the boundary.
     * TODO: future multi-dimension support is a later concern.
     * <p>
     * Types with no live sites are omitted from the returned {@link SensedSites}; querying
     * them via {@code count}/{@code coords}/{@code isPresent} returns {@code 0}/{@code []}/{@code false}.
     */
    public SensedSites read(@Nonnull IBrain brain) {
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> sites = new HashMap<>();
        for (MemoryType<List<GlobalPos>> type : MemoryTypeRegistry.spatialSiteTypes()) {
            List<GlobalPos> positions = brain.getMemory(type).orElse(List.of());
            if (!positions.isEmpty()) {
                List<SiteCoord> coords = positions.stream()
                        .map(globalPos -> new SiteCoord(globalPos.pos().getX(), globalPos.pos().getY(), globalPos.pos().getZ()))
                        .toList();
                sites.put(type, coords);
            }
        }
        return new SensedSites(sites);
    }

}
