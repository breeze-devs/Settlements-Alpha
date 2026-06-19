package dev.breezes.settlements.application.ai.inference.monologue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Capability seam for generating offline ambient monologue packs.
 */
public interface MonologueGateway {

    CompletableFuture<MonologueBatchResponse> generate(MonologueBatchRequest request, Duration deadline);

}
