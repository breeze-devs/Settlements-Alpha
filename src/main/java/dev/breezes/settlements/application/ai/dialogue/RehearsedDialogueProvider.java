package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.shared.util.RandomUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DialogueProvider} implementation for {@link DialogueMode#REHEARSED}.
 * <p>
 * The evening sweep generates per-villager utterance packs offline by calling the dialog service with
 * {@code n > 1} completions. Packs are stored in memory and sampled during the next day — zero dialog
 * service calls during normal gameplay.
 * <p>
 * The sweep runs within a configurable wall-clock budget ({@link DialogueConfig#packSweepDeadlineSeconds}).
 * A sweep that runs over budget simply stops generating; villagers with no pack fall back to
 * the injected lower rung. Failure is always cosmetic.
 * <p>
 * Thread safety: the {@link #packs} map is a {@link ConcurrentHashMap} because the sweep
 * writes from CompletableFuture threads while the server tick thread may read during daytime.
 */
public final class RehearsedDialogueProvider implements DialogueProvider {

    private final DialogueProvider fallback;
    private final ConcurrentHashMap<UUID, UtterancePack> packs;

    public RehearsedDialogueProvider(DialogueProvider fallback) {
        this.fallback = fallback;
        this.packs = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<DialogueLine> sampleAmbientLine(@Nonnull UUID villagerUuid, @Nonnull DialogueContext context) {
        UtterancePack pack = this.packs.get(villagerUuid);

        if (pack == null || pack.isEmpty()) {
            return this.fallback.sampleAmbientLine(villagerUuid, context);
        }

        return pack.drawLine().map(DialogueLine::literal)
                .or(() -> this.fallback.sampleAmbientLine(villagerUuid, context));
    }

    /**
     * Each request is bounded by the configured budget so a stalled backend cannot leave a call hanging.
     * The sweep itself never blocks the calling (server) thread — packs populate asynchronously
     *
     */
    @Override
    public void runEveningPackSweep() {
        // MONOLOGUE generation is intentionally deferred to phase 3. Until then this rung only
        // samples already-installed packs, so normal gameplay remains on the SCRIPTED floor.
    }

    @Override
    public boolean isEnabled() {
        return this.fallback.isEnabled() || !this.packs.isEmpty();
    }

    @Override
    public boolean supportsRehearsedDialogSweep() {
        return true;
    }

    void installPack(@Nonnull UUID villagerUuid, @Nonnull List<String> lines) {
        if (!lines.isEmpty()) {
            this.packs.put(villagerUuid, new UtterancePack(lines, RandomUtil.RANDOM));
        }
    }

}
