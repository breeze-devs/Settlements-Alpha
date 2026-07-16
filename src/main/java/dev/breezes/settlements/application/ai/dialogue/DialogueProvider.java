package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The single seam between the villager AI and whatever inference path is configured.
 * <p>
 * All dialog output flows through this interface. Game-state decisions (knowledge transfer,
 * relationship deltas, credibility moves) are made deterministically before calling
 * this provider. The provider only renders words — it never mutates game state.
 * <p>
 * Implementations must be thread-safe: the provider may schedule async HTTP calls internally,
 * but it is constructed on the server thread and its methods may be called from it.
 */
public interface DialogueProvider {

    /**
     * Samples a single ambient utterance line for the given villager to display in a FLAVOR
     * bubble. The call returns immediately: SCRIPTED returns a translatable key, while
     * REHEARSED returns a literal model line when available and otherwise falls back to SCRIPTED.
     * <p>
     * The result, if present, has already been sanitized by {@link DialogueResponseSanitizer}.
     *
     * @param villagerUuid the unique id of the speaking villager
     * @param context      structured prompt context assembled by the caller
     * @return a sanitized, displayable line, or empty when the configured floor is silent
     */
    Optional<DialogueLine> sampleAmbientLine(UUID villagerUuid, DialogueContext context);

    /**
     * Kicks off the evening batch sweep for the given loaded villagers. Called once per in-game
     * evening from the server-tick event, after the caller enumerates all loaded BaseVillagers.
     * <p>
     * This is a no-op in SCRIPTED mode. In REHEARSED mode the sweep dispatches async generation
     * within the configured deadline — it must never block the calling (tick) thread.
     *
     * @param villagers all currently loaded villagers; the provider may filter internally
     */
    void runEveningPackSweep(Collection<BaseVillager> villagers);

    /**
     * Cancels any inference exchange this provider currently has in flight on the shared transport.
     * <p>
     * Called during server shutdown before the transport is closed: {@code InferenceTransport#close}
     * blocks until in-flight exchanges finish, and an evening sweep can legitimately stream for up to
     * {@link RehearsedDialogueConfig#packSweepDeadlineSeconds}, so an uncancelled sweep would stall the stop.
     * No-op for providers that never dispatch async inference (e.g. SCRIPTED).
     */
    default void cancelInflightSweep() {
        // No in-flight inference to cancel by default.
    }

    /**
     * Returns {@code true} if this provider is effectively enabled — i.e. will ever produce
     * non-empty results. Callers can skip building the context object when the provider is
     * disabled, avoiding any wasted work.
     */
    boolean isEnabled();

    /**
     * Drops any per-villager state this provider holds for {@code villagerUuid}.
     *
     * @param villagerUuid the id of the villager being removed
     */
    default void evict(UUID villagerUuid) {
        // No per-villager state to drop by default.
    }

    /**
     * Returns true for providers that can use evening batch context collection.
     */
    default boolean supportsRehearsedDialogSweep() {
        return false;
    }

}
