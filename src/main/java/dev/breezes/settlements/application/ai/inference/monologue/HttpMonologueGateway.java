package dev.breezes.settlements.application.ai.inference.monologue;

import com.google.gson.Gson;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceProtocol;
import dev.breezes.settlements.application.ai.inference.InferenceResponseEnvelope;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * HTTP-backed MONOLOGUE gateway. Transport failures resolve to an empty capability response.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class HttpMonologueGateway implements MonologueGateway {

    private static final Gson GSON = new Gson();

    private final InferenceTransport transport;

    @Override
    public CompletableFuture<MonologueBatchResponse> generate(@Nonnull MonologueBatchRequest request,
                                                              @Nonnull Duration deadline) {
        return this.transport.post(InferenceCapability.MONOLOGUE, request, deadline)
                .thenApply(response -> response.body()
                        .map(HttpMonologueGateway::parse)
                        .orElseGet(HttpMonologueGateway::emptyResponse));
    }

    private static MonologueBatchResponse parse(String body) {
        try {
            InferenceResponseEnvelope envelope = GSON.fromJson(body, InferenceResponseEnvelope.class);
            if (envelope == null || envelope.getPayload() == null || envelope.getPayload().isJsonNull()) {
                return emptyResponse();
            }

            if (envelope.getProtocolVersion() != InferenceProtocol.VERSION) {
                log.error("MONOLOGUE response protocol version {} is unsupported", envelope.getProtocolVersion());
                return emptyResponse();
            }

            MonologueBatchResponse response = GSON.fromJson(envelope.getPayload(), MonologueBatchResponse.class);
            return response == null ? emptyResponse() : normalize(response);
        } catch (RuntimeException e) {
            log.error("MONOLOGUE response parse failed: {}", e.getMessage());
            return emptyResponse();
        }
    }

    private static MonologueBatchResponse normalize(MonologueBatchResponse response) {
        return MonologueBatchResponse.builder()
                .villagers(normalizeVillagers(response.getVillagers()))
                .build();
    }

    private static List<VillagerMonologueResult> normalizeVillagers(List<VillagerMonologueResult> villagers) {
        if (villagers == null) {
            return List.of();
        }

        return villagers.stream()
                .map(HttpMonologueGateway::normalizeVillager)
                .toList();
    }

    private static VillagerMonologueResult normalizeVillager(VillagerMonologueResult villager) {
        if (villager == null) {
            return VillagerMonologueResult.builder().build();
        }

        return VillagerMonologueResult.builder()
                .villagerId(villager.getVillagerId())
                .buckets(normalizeBuckets(villager.getBuckets()))
                .build();
    }

    private static Map<Occasion, List<GeneratedLine>> normalizeBuckets(Map<Occasion, List<GeneratedLine>> buckets) {
        if (buckets == null) {
            return Map.of();
        }

        return buckets.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> normalizeLines(entry.getValue())));
    }

    private static List<GeneratedLine> normalizeLines(List<GeneratedLine> lines) {
        if (lines == null) {
            return List.of();
        }

        return lines.stream()
                .map(HttpMonologueGateway::normalizeLine)
                .toList();
    }

    private static GeneratedLine normalizeLine(GeneratedLine line) {
        if (line == null) {
            return GeneratedLine.builder().status(LineStatus.OK).build();
        }

        return GeneratedLine.builder()
                .text(line.getText())
                .status(line.getStatus() == null ? LineStatus.OK : line.getStatus())
                .build();
    }

    private static MonologueBatchResponse emptyResponse() {
        return MonologueBatchResponse.builder()
                .villagers(List.of())
                .build();
    }

}
