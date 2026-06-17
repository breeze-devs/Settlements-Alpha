package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.llm.DialogRequest;
import dev.breezes.settlements.application.ai.dialogue.llm.DialogServiceHttpClient;
import dev.breezes.settlements.application.ai.dialogue.llm.LlmResponse;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO:LLM -- llm stuff we'll need to touch later
 * {@link DialogueProvider} implementation for {@link DialogueMode#PACKS}.
 * <p>
 * The evening sweep generates per-villager utterance packs offline by calling the dialog service with
 * {@code n > 1} completions. Packs are stored in memory and sampled during the next day — zero dialog
 * service calls during normal gameplay.
 * <p>
 * The sweep runs within a configurable wall-clock budget ({@link DialogueConfig#packSweepDeadlineSeconds}).
 * A sweep that runs over budget simply stops generating; villagers with no pack fall back to
 * canned lines. Failure is always cosmetic.
 * <p>
 * Thread safety: the {@link #packs} map is a {@link ConcurrentHashMap} because the sweep
 * writes from CompletableFuture threads while the server tick thread may read during daytime.
 */
@CustomLog
public final class PacksDialogueProvider implements DialogueProvider {

    // TODO:CONFIRM -- we should check this into the en_us.json for i18n
    private static final List<String> CANNED_LINES = List.of(
            "Another fine day in the village.",
            "Have you seen the harvest this season?",
            "Stay safe out there.",
            "There's always more work to be done."
    );

    private final DialogServiceHttpClient client;
    private final DialogueConfig config;
    private final ConcurrentHashMap<UUID, UtterancePack> packs;

    public PacksDialogueProvider(DialogServiceHttpClient client, DialogueConfig config) {
        this.client = client;
        this.config = config;
        this.packs = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<String> sampleAmbientLine(@Nonnull UUID villagerUuid, @Nonnull DialogueContext context) {
        UtterancePack pack = this.packs.get(villagerUuid);

        if (pack == null || pack.isEmpty()) {
            // Pack exhausted or not generated yet, fall back to a canned line
            return sampleCannedLine();
        }

        return pack.drawLine();
    }

    /**
     * Each request is bounded by the configured budget so a stalled backend cannot leave a call hanging.
     * The sweep itself never blocks the calling (server) thread — packs populate asynchronously
     *
     * @param villagerContexts one context entry per villager that needs a refreshed pack
     */
    @Override
    public void runEveningPackSweep(Iterable<VillagerDialogueContext> villagerContexts) {
        Duration requestDeadline = Duration.ofSeconds(this.config.packSweepDeadlineSeconds());

        int dispatched = 0;
        // TODO:CONFIRM -- should we consider request batching here? or batch on the dialog service side
        for (VillagerDialogueContext villagerCtx : villagerContexts) {
            DialogRequest request = PromptAssembler.buildPacksRequest(villagerCtx, this.config, this.config.packLinesPerVillager());

            try {
                this.client.send(request, requestDeadline)
                        .thenAccept(response -> storePack(villagerCtx.getVillagerUuid(), response));
                dispatched++;
            } catch (RuntimeException e) {
                log.debug("Pack sweep dispatch failed for villager {}: {}", villagerCtx.getVillagerUuid(), e.getMessage());
            }
        }

        log.debug("Async evening pack sweep dispatched {} villager requests", dispatched);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private void storePack(@Nonnull UUID villagerUuid, @Nonnull LlmResponse response) {
        List<String> rawLines = response.allContents();
        if (rawLines.isEmpty()) {
            log.warn("Pack sweep produced no lines for villager {}", villagerUuid);
            return;
        }

        List<String> sanitizedLines = new ArrayList<>(rawLines.size());
        for (String raw : rawLines) {
            DialogueResponseSanitizer.sanitize(raw, this.config.bubbleCharCap())
                    .ifPresent(sanitizedLines::add);
        }

        if (sanitizedLines.isEmpty()) {
            log.debug("All pack lines sanitized to empty for villager {}", villagerUuid);
            return;
        }

        this.packs.put(villagerUuid, new UtterancePack(sanitizedLines, RandomUtil.RANDOM));
        log.debug("Stored pack of {} lines for villager {}", sanitizedLines.size(), villagerUuid);
    }

    private Optional<String> sampleCannedLine() {
        return Optional.of(RandomUtil.choice(CANNED_LINES));
    }

}
