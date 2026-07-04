package dev.breezes.settlements.application.ai.inference.persona;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Capability seam for generating LLM-authored villager persona cards.
 */
public interface PersonaGateway {

    /**
     * Starts a streaming persona request and invokes {@code onVillager} each time a complete
     * villager result is received from the backend.
     * <p>
     * Malformed lines are logged and skipped; they do not abort the stream. The returned handle
     * lets the caller cancel the in-flight exchange and observe stream completion.
     *
     * @param request    batch payload describing all villagers to generate personas for
     * @param deadline   wall-clock budget for the entire stream
     * @param onVillager called on the HTTP client's executor thread per successfully-parsed villager
     * @return a handle for cancellation and completion observation
     */
    InferenceStreamHandle generate(PersonaBatchRequest request, Duration deadline, Consumer<PersonaVillagerResult> onVillager);

}
