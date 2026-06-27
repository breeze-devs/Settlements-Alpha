package dev.breezes.settlements.application.ai.sensors;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Immutable snapshot of a single 16³ section's block resources at a given tick.
 */
public record SectionResourceScan(long scannedAtTick,
                                  @Nonnull Map<BlockResource, long[]> hitsByResource) {

    private static final long[] EMPTY = new long[0];

    /**
     * Returns the packed BlockPos longs for the given resource in this section.
     * Returns an empty array when the section was scanned and found nothing for that resource.
     */
    public long[] hitsFor(@Nonnull BlockResource resource) {
        return this.hitsByResource.getOrDefault(resource, EMPTY);
    }

}
