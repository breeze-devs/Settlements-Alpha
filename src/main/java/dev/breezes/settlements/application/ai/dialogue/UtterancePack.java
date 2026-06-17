package dev.breezes.settlements.application.ai.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * A per-villager set of pre-generated utterance lines, produced during the evening
 * PACKS sweep and sampled during the next day.
 * <p>
 * Lines are popped in a random order to avoid repetition within a day. When the pack
 * is exhausted the villager falls back to canned lines until the next evening sweep
 * refreshes the pack.
 * <p>
 * Thread safety: this class is accessed from the server tick thread only. No external
 * synchronization is needed; it is not shared across threads.
 */
public final class UtterancePack {

    private final List<String> remaining;
    private final Random random;

    public UtterancePack(List<String> lines, Random random) {
        this.remaining = new ArrayList<>(lines);
        this.random = random;
    }

    /**
     * Returns and removes a random line from the pack.
     * Returns empty when the pack has been fully consumed.
     */
    public Optional<String> drawLine() {
        if (this.remaining.isEmpty()) {
            return Optional.empty();
        }
        int index = this.random.nextInt(this.remaining.size());
        // Swap-remove: O(1) remove without shifting the whole list.
        int lastIndex = this.remaining.size() - 1;
        Collections.swap(this.remaining, index, lastIndex);
        return Optional.of(this.remaining.remove(lastIndex));
    }

    public boolean isEmpty() {
        return this.remaining.isEmpty();
    }

    public int size() {
        return this.remaining.size();
    }

}
