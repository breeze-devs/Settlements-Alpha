package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueBatchRequest;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueGateway;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.application.ai.inference.monologue.VillagerMonologueResult;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles the SIS round-trip for the evening rehearsal sweep.
 * <p>
 * Responsibility: given a collection of villagers, produce ready-to-install {@link VillagerPack}s
 * by calling the monologue backend once. This class is the only place that touches
 * {@link MonologueGateway} for the sweep — it encapsulates occasion-set resolution, batch
 * assembly, the deadline-bounded gateway call, and response sanitization.
 * <p>
 * {@link RehearsedDialogueProvider} owns the pack store and lifecycle; this service owns the
 * backend round-trip. The pure conversion path ({@link #toPack}) is kept Minecraft-free so it
 * can be unit-tested without an MC environment.
 */
@CustomLog
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MonologueRequestService {

    private final MonologueRequestAssembler assembler;
    private final MonologueGateway gateway;
    private final OccasionSetResolver occasionSetResolver;
    private final DialogueConfig config;
    private final RehearsedDialogueConfig rehearsedDialogueConfig;

    /**
     * Dispatches a streaming monologue request for the given villagers and invokes
     * {@code onPack} per villager as their result arrives from the backend.
     * <p>
     * The callback runs on the gateway's thread pool — never on the server tick thread.
     * Empty input returns a no-op handle immediately. Any gateway failure is absorbed; villagers
     * whose results are absent fall back to the scripted floor through the provider.
     *
     * @param villagers villagers that need new packs
     * @param onPack    called per surviving villager pack as the stream progresses
     * @return a handle for cancellation and completion observation; already-complete for empty input
     */
    public InferenceStreamHandle requestPacksStreaming(Collection<BaseVillager> villagers,
                                                       Consumer<VillagerPack> onPack) {
        log.debug("Requesting monologue packs (streaming) for {} villagers", villagers.size());

        Map<BaseVillager, Collection<Occasion>> villagersWithOccasions = new HashMap<>();
        for (BaseVillager villager : villagers) {
            villagersWithOccasions.put(villager, this.occasionSetResolver.resolve(villager.getProfession()));
        }

        if (villagersWithOccasions.isEmpty()) {
            return InferenceStreamHandle.noOp();
        }

        MonologueBatchRequest batch = this.assembler.assembleForVillagers(villagersWithOccasions);
        Duration deadline = Duration.ofSeconds(this.rehearsedDialogueConfig.packSweepDeadlineSeconds());

        return this.gateway.generate(batch, deadline,
                result -> this.toPack(result).ifPresent(onPack));
    }

    /**
     * Converts one raw villager result to an installable pack, applying sanitization and pruning.
     * Returns empty if no occasions survive sanitization (all lines blank or all occasions empty).
     */
    Optional<VillagerPack> toPack(VillagerMonologueResult villagerResult) {
        VillagerPack.VillagerPackBuilder packBuilder = VillagerPack.builder()
                .villagerId(villagerResult.getVillagerId());

        int bubbleCharCap = this.config.bubbleCharCap();
        boolean hasAnyLines = false;

        for (Map.Entry<Occasion, List<String>> entry : villagerResult.getBuckets().entrySet()) {
            Occasion occasion = entry.getKey();
            List<String> sanitized = new ArrayList<>();

            for (String line : entry.getValue()) {
                DialogueResponseSanitizer.sanitize(line, bubbleCharCap)
                        .ifPresent(sanitized::add);
            }

            if (!sanitized.isEmpty()) {
                // Only include occasions that have at least one surviving line after sanitization.
                packBuilder.linesByOccasion(occasion, sanitized);
                hasAnyLines = true;
            }
        }

        if (!hasAnyLines) {
            return Optional.empty();
        }

        return Optional.of(packBuilder.build());
    }

}
