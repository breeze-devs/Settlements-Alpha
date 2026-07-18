package dev.breezes.settlements.application.ai.inference.monologue;

import com.google.gson.Gson;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * HTTP-backed MONOLOGUE gateway. Transport errors are absorbed and produce no villager results;
 * the caller sees an empty stream, and the scripted dialogue floor takes over for all villagers.
 * <p>
 * The backend streams NDJSON: one {@link VillagerMonologueResult} JSON object per line emitted
 * as soon as that villager's upstream generation finishes. Each line is parsed independently so
 * a single malformed line cannot discard the rest of the stream.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class HttpMonologueGateway implements MonologueGateway {

    private static final Gson GSON = new Gson();

    private final InferenceTransport transport;

    @Override
    public InferenceStreamHandle generate(@Nonnull MonologueBatchRequest request,
                                          @Nonnull Duration deadline,
                                          @Nonnull Consumer<VillagerMonologueResult> onVillager) {
        return this.transport.postStreaming(InferenceCapability.MONOLOGUE, request, deadline,
                line -> this.parseLine(line, onVillager));
    }

    /**
     * Parses one NDJSON line and, if valid, invokes {@code onVillager}.
     * A bad line (malformed JSON, missing id, null result) is logged and skipped — the stream
     * continues and remaining villagers are unaffected.
     */
    private void parseLine(String line, Consumer<VillagerMonologueResult> onVillager) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            VillagerMonologueResult result = GSON.fromJson(trimmed, VillagerMonologueResult.class);
            if (result != null && result.getVillagerId() != null) {
                onVillager.accept(normalizeVillager(result));
            }
        } catch (RuntimeException e) {
            log.error("MONOLOGUE response line parse failed: {}", e.getMessage());
        }
    }

    private static VillagerMonologueResult normalizeVillager(VillagerMonologueResult villager) {
        return VillagerMonologueResult.builder()
                .villagerId(villager.getVillagerId())
                .buckets(normalizeBuckets(villager.getBuckets()))
                .build();
    }

    private static Map<Occasion, List<String>> normalizeBuckets(Map<Occasion, List<String>> buckets) {
        if (buckets == null) {
            return Map.of();
        }

        // Null-safety only: SIS returns bare, already-sanitized strings; the render-side cap and
        // formatting strip happen later in MonologueRequestService#toPack.
        return buckets.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(Objects::nonNull)
                                .toList()));
    }

}
