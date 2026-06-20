package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

@Builder
public record DumpFacet(
        int above
) {

    public DumpFacet {
        if (above < 0) {
            throw new IllegalArgumentException("Dump above must be non-negative");
        }
    }

}
