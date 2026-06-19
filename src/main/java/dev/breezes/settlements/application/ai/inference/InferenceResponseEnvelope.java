package dev.breezes.settlements.application.ai.inference;

import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Shared wire envelope wrapped around capability-specific response payloads by the inference service.
 */
@Builder
@Getter
public final class InferenceResponseEnvelope {

    private final int protocolVersion;
    private final UUID requestId;
    private final JsonElement payload;

}
