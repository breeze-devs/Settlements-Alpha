package dev.breezes.settlements.application.ai.inference;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Shared wire envelope wrapped around capability-specific payloads by the transport layer.
 */
@Builder
@Getter
final class InferenceRequestEnvelope {

    private final int protocolVersion;
    private final UUID requestId;
    private final InferenceCapability capability;
    private final long deadlineMillis;
    private final int deadlineSlackMillis;
    private final Object payload;

}
