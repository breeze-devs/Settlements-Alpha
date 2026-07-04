package dev.breezes.settlements.application.ai.inference.persona;

import dev.breezes.settlements.domain.genetics.GeneSignal;
import dev.breezes.settlements.domain.personality.OriginType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * PERSONA request payload for one villager sent to SIS.
 * <p>
 * Ships the raw substrate a persona characterSketch is generated from — birth provenance, un-rendered gene
 * signals, and (for bred villagers only) parent lineage — never pre-rendered English. SIS owns
 * all phrasing.
 * <p>
 * {@code spawnType} reuses domain {@link OriginType} directly as the wire element (Gson serializes
 * it as its enum name) rather than duplicating a parallel wire enum. Likewise {@code geneSignals}
 * reuses domain {@link GeneSignal} directly.
 */
@Builder
@Getter
public final class PersonaVillagerRequest {

    private final UUID villagerId;
    private final OriginType spawnType;
    private final boolean isNitwit;

    @Singular
    private final List<GeneSignal> geneSignals;

    /**
     * Present only for bred villagers; {@code null} (and thus omitted by Gson's default
     * null-omission) for worldgen, zombie-converted, and unknown origins.
     */
    @Nullable
    private final PersonaLineage lineage;

}
