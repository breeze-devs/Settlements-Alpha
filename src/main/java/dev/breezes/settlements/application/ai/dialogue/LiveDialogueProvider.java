package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.llm.DialogRequest;
import dev.breezes.settlements.application.ai.dialogue.llm.DialogServiceHttpClient;
import lombok.CustomLog;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO:CONFIRM -- llm specifics
 * {@link DialogueProvider} implementation for {@link DialogueMode#LIVE}.
 * <p>
 * Fires a per-utterance dialog request at speak time. The request is queued with priority
 * semantics and routed through {@link DialogueRequestQueue} so it never blocks the server tick thread.
 * <p>
 * Fallback ladder: LIVE response → PACKS line → canned line.
 * The {@link PacksDialogueProvider} delegate handles the last two rungs.
 * <p>
 * Results arrive asynchronously. {@link #sampleAmbientLine} returns empty immediately and
 * delivers the result to the villager via a side-channel callback once inference returns.
 */
@CustomLog
public final class LiveDialogueProvider implements DialogueProvider {

    private final DialogueRequestQueue requestQueue;
    private final PacksDialogueProvider packsDelegate;
    private final DialogueConfig config;

    /**
     * Pending results keyed by villager UUID. The request queue writes via callbacks;
     * the next call to {@link #sampleAmbientLine} for the same villager claims the result.
     * <p>
     * A ConcurrentHashMap is used because writes arrive on async completion threads while
     * reads happen on the server tick thread.
     */
    private final ConcurrentHashMap<UUID, String> pendingResults;


    public LiveDialogueProvider(DialogServiceHttpClient httpClient, DialogueConfig config) {
        this.requestQueue = new DialogueRequestQueue(httpClient, config.maxConcurrentRequests());
        this.packsDelegate = new PacksDialogueProvider(httpClient, config);
        this.config = config;
        this.pendingResults = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<String> sampleAmbientLine(UUID villagerUuid, DialogueContext context) {
        // If a prior async call already resolved, claim and return it immediately.
        String ready = this.pendingResults.remove(villagerUuid);
        if (ready != null) {
            return Optional.of(ready);
        }

        // Fire a new async request; the result will be available on the next call
        // (or whenever inference returns, whichever is later).
        DialogRequest request = PromptAssembler.buildSingleRequest(context, this.config);
        Duration deadline = resolveDeadline(context.getPriority());

        this.requestQueue.submit(
                villagerUuid,
                request,
                context.getPriority(),
                deadline,
                optResult -> optResult.ifPresent(line -> this.pendingResults.put(villagerUuid, line)),
                this.config.bubbleCharCap());

        // While we wait for inference, the packs delegate provides an immediate line.
        // This ensures the villager always says something within the same tick if a pack
        // is available, without stalling on the network round-trip.
        return this.packsDelegate.sampleAmbientLine(villagerUuid, context);
    }

    @Override
    public void runEveningPackSweep(Iterable<VillagerDialogueContext> villagerContexts) {
        // Delegate to the packs provider so LIVE mode also refreshes its pack fallbacks.
        this.packsDelegate.runEveningPackSweep(villagerContexts);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Duration resolveDeadline(DialoguePriority priority) {
        return switch (priority) {
            case PLAYER -> Duration.ofMillis(this.config.liveDeadlineMillisPlayer());
            case VILLAGER, AMBIENT -> Duration.ofMillis(this.config.liveDeadlineMillisAmbient());
        };
    }

}
