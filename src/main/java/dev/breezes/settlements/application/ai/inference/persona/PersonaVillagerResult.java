package dev.breezes.settlements.application.ai.inference.persona;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * PERSONA response payload for one villager, streamed as one NDJSON line per villager.
 * <p>
 * A Gson-populated wire DTO, not persisted state — unlike {@code VillagerPersonality},
 * {@code speechStyle} is legitimately nullable here (SIS may omit it).
 */
@Builder
@Getter
public final class PersonaVillagerResult {

    private final UUID villagerId;

    @Singular
    private final List<String> adjectives;

    private final String characterSketch;

    @Nullable
    private final String speechStyle;

}
