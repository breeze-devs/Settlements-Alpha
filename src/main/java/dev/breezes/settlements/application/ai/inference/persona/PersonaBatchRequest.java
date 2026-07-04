package dev.breezes.settlements.application.ai.inference.persona;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Capability-specific PERSONA request body. Transport envelope fields are added elsewhere.
 */
@Builder
@Getter
public final class PersonaBatchRequest {

    @Singular
    private final List<PersonaVillagerRequest> villagers;

}
