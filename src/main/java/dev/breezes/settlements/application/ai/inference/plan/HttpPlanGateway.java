package dev.breezes.settlements.application.ai.inference.plan;

import com.google.gson.Gson;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * HTTP-backed PLAN gateway. Transport errors are absorbed and produce no villager results; the
 * caller sees an empty stream, and the heuristic day-plan floor takes over for all villagers.
 * <p>
 * The backend streams NDJSON: one {@link VillagerPlanResult} JSON object per line emitted as
 * soon as that villager's upstream generation finishes. Each line is parsed independently so a
 * single malformed line cannot discard the rest of the stream.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class HttpPlanGateway implements PlanGateway {

    private static final Gson GSON = new Gson();

    private final InferenceTransport transport;

    @Override
    public InferenceStreamHandle generate(@Nonnull PlanBatchRequest request,
                                          @Nonnull Duration deadline,
                                          @Nonnull Consumer<VillagerPlanResult> onVillager) {
        return this.transport.postStreaming(InferenceCapability.PLAN, request, deadline,
                line -> this.parseLine(line, onVillager));
    }

    /**
     * Parses one NDJSON line and, if valid, invokes {@code onVillager}.
     * A bad line (malformed JSON, missing id, null result) is logged and skipped — the stream
     * continues and remaining villagers are unaffected.
     */
    private void parseLine(String line, Consumer<VillagerPlanResult> onVillager) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            VillagerPlanResult result = GSON.fromJson(trimmed, VillagerPlanResult.class);
            if (result != null && result.getVillagerId() != null) {
                onVillager.accept(normalizeVillager(result));
            }
        } catch (RuntimeException e) {
            log.error("PLAN response line parse failed: {}", e.getMessage());
        }
    }

    private static VillagerPlanResult normalizeVillager(VillagerPlanResult villager) {
        return VillagerPlanResult.builder()
                .villagerId(villager.getVillagerId())
                .selections(normalizeSelections(villager.getSelections()))
                .build();
    }

    /**
     * Gson bypasses the {@code @Singular} builder, so an omitted {@code selections} object arrives
     * as a null map (and a window with a null array as a null value) rather than the empty defaults
     * the builder would supply. Left unnormalized, "the model chose nothing" — a legitimate sparse
     * day — would NPE downstream in {@code LlmOverlayPlanGenerator#toIntent} and the whole villager's
     * overlay would be silently dropped for the cycle. Coalesce to non-null here, at the wire seam.
     */
    private static Map<String, List<PlanSelection>> normalizeSelections(Map<String, List<PlanSelection>> selections) {
        if (selections == null) {
            return Map.of();
        }

        return selections.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(Objects::nonNull)
                                .toList()));
    }

}
