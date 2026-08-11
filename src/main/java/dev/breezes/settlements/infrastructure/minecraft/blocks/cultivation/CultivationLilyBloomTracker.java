package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

/**
 * Detects the moment a Cultivation Lily's validity transitions from invalid to valid, so the bloom
 * animation fires once per event rather than once per render of a lily that was already blooming.
 * <p>
 * {@code valid} — and the {@code LIT} state it drives — arrives already {@code true} on chunk load, on
 * world join, and on re-entering render distance, because those are synced-state deliveries rather
 * than server-side changes this client witnessed. Keying the animation off the raw flag would replay
 * the bloom every time a player merely walks back into view of an already-blooming lily. Treating the
 * very first observation as already caught up, whatever it reports, and animating only a later
 * false→true edge, is what confines the animation to a change the player did not already see happen.
 */
final class CultivationLilyBloomTracker {

    private boolean hasObserved;
    private boolean lastValid;

    /**
     * Records this observation and reports whether it is a bloom-worthy transition.
     */
    boolean observe(boolean currentlyValid) {
        boolean isBloomTransition = this.hasObserved && !this.lastValid && currentlyValid;
        this.hasObserved = true;
        this.lastValid = currentlyValid;
        return isBloomTransition;
    }

}
