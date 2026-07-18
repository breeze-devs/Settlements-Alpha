package dev.breezes.settlements.application.ai.inference.persona;

import com.google.gson.Gson;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * HTTP-backed PERSONA gateway. Transport errors are absorbed and produce no villager results;
 * the caller sees an empty stream.
 * <p>
 * The backend streams NDJSON: one {@link PersonaVillagerResult} JSON object per line emitted as
 * soon as that villager's upstream generation finishes. Each line is parsed independently so a
 * single malformed line cannot discard the rest of the stream.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class HttpPersonaGateway implements PersonaGateway {

    private static final Gson GSON = new Gson();

    private final InferenceTransport transport;

    @Override
    public InferenceStreamHandle generate(@Nonnull PersonaBatchRequest request,
                                          @Nonnull Duration deadline,
                                          @Nonnull Consumer<PersonaVillagerResult> onVillager) {
        return this.transport.postStreaming(InferenceCapability.PERSONA, request, deadline,
                line -> this.parseLine(line, onVillager));
    }

    /**
     * Parses one NDJSON line and, if valid, invokes {@code onVillager}.
     * A bad line (malformed JSON, missing id, null result) is logged and skipped — the stream
     * continues and remaining villagers are unaffected.
     */
    private void parseLine(String line, Consumer<PersonaVillagerResult> onVillager) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            PersonaVillagerResult result = GSON.fromJson(trimmed, PersonaVillagerResult.class);
            if (result != null && result.getVillagerId() != null) {
                onVillager.accept(result);
            }
        } catch (RuntimeException e) {
            log.error("PERSONA response line parse failed: {}", e.getMessage());
        }
    }

}
