package dev.breezes.settlements.application.ai.dialogue;

import java.util.Optional;
import java.util.UUID;

/**
 * No-op {@link DialogueProvider} used when {@link DialogueMode#OFF} is configured
 * <p>
 * Always yields empty results. Callers should skip building a {@link DialogueContext} by
 * checking {@link #isEnabled()} first when context assembly has any non-trivial cost.
 */
public final class OffDialogueProvider implements DialogueProvider {

    @Override
    public Optional<String> sampleAmbientLine(UUID villagerUuid, DialogueContext context) {
        return Optional.empty();
    }

    @Override
    public void runEveningPackSweep(Iterable<VillagerDialogueContext> villagerContexts) {
        // Intentionally a no-op: no backend is configured in OFF mode.
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

}
