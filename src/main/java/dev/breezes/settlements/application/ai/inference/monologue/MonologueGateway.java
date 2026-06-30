package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Capability seam for generating offline ambient monologue packs.
 * <p>
 * The backend streams its result as NDJSON — one {@link VillagerMonologueResult} per line —
 * so the gateway consumes the stream progressively via a per-villager callback rather than
 * waiting for the full body. This lets a slow villager's generation delay only itself, not
 * the whole batch.
 */
public interface MonologueGateway {

    /**
     * Starts a streaming monologue request and invokes {@code onVillager} each time a
     * complete villager result is received from the backend.
     * <p>
     * Malformed lines are logged and skipped; they do not abort the stream. The returned handle
     * lets the caller cancel the in-flight exchange and observe stream completion.
     *
     * @param request    batch payload describing all villagers and their occasion buckets
     * @param deadline   wall-clock budget for the entire stream
     * @param onVillager called on the HTTP client's executor thread per successfully-parsed villager
     * @return a handle for cancellation and completion observation
     */
    InferenceStreamHandle generate(MonologueBatchRequest request, Duration deadline, Consumer<VillagerMonologueResult> onVillager);

}
