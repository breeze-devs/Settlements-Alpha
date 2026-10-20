package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.ClientBoundSettlementArrivalPacket;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.SettlementAnnouncementType;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Schedules settlement announcements as HUD titles, with at most one active title and one pending announcement.
 * <p>
 * An eligible pending announcement can replace an active title.
 * With titles switched off, every announcement is dropped.
 */
@ClientSide
@ClientScope
public final class SettlementAnnouncementQueue implements ClientSessionResettable {

    /**
     * Delay before an exit becomes eligible for display, allowing immediate returns to cancel it.
     */
    private static final ClockTicks EXIT_GRACE = ClockTicks.seconds(2);
    static final long EXIT_GRACE_MILLIS = ClientMonotonicClock.millisIn(EXIT_GRACE);

    static final Timing MAIN_TITLE = Timing.of(ClockTicks.seconds(0.5), ClockTicks.seconds(4), ClockTicks.seconds(1));

    /**
     * Animation timing for exits, with a shorter hold than other announcements.
     */
    static final Timing EXIT_TITLE = Timing.of(ClockTicks.seconds(0.3), ClockTicks.seconds(1.5), ClockTicks.seconds(0.7));

    private final boolean titlesEnabled;

    @Nullable
    private PendingTitle pendingTitle;
    @Nullable
    private Surface title;

    @Inject
    SettlementAnnouncementQueue(@Nonnull SettlementTitleConfig config) {
        this.titlesEnabled = config.enabled();
    }

    /**
     * Updates the pending announcement, leaving the active title as it is.
     * Exits wait through a grace period; other announcement types are immediately eligible for display.
     * <p>
     * Matching entry and exit announcements cancel each other while pending. An entry otherwise preserves a pending
     * announcement for the same settlement, and an exit otherwise leaves pending non-exit announcements unchanged.
     * All other offers replace any pending announcement.
     *
     * @param nowMillis current client monotonic time, in milliseconds
     */
    public void offer(@Nonnull ClientBoundSettlementArrivalPacket arrival, long nowMillis) {
        if (!this.titlesEnabled) {
            return;
        }

        PendingTitle pending = this.pendingTitle;
        boolean pendingForThis = pending != null && SettlementKey.of(pending.arrival()).equals(SettlementKey.of(arrival));
        switch (arrival.announcementType()) {
            case DISCOVERY, FOUNDED, RENAMED, DISBANDED -> this.pendingTitle = new PendingTitle(arrival, nowMillis);
            case ENTRY -> {
                // Routine reentry must not hide an unshown discovery or settlement change for the same settlement.
                if (!pendingForThis) {
                    this.pendingTitle = new PendingTitle(arrival, nowMillis);
                } else if (pending.arrival().announcementType() == SettlementAnnouncementType.EXIT) {
                    // No departure was shown, so the return does not need another entry announcement.
                    this.pendingTitle = null;
                }
            }
            case EXIT -> {
                // Discoveries and settlement changes take priority over departure announcements.
                if (pending == null || pending.arrival().announcementType() == SettlementAnnouncementType.EXIT) {
                    this.pendingTitle = new PendingTitle(arrival, nowMillis + EXIT_GRACE_MILLIS);
                } else if (pendingForThis && pending.arrival().announcementType() == SettlementAnnouncementType.ENTRY) {
                    // The entry was never shown, so an exit title would have no matching entry.
                    // Cancel both so a subsequent return can produce a fresh entry announcement.
                    this.pendingTitle = null;
                }
            }
        }
    }

    /**
     * Returns the title for this frame, starting an eligible pending announcement if present.
     * Call only on frames that draw titles, so that no announcement starts unseen.
     *
     * @param dimension the client's current dimension
     * @param nowMillis current client monotonic time, in milliseconds
     * @return the title for this frame, or null when none is active
     */
    @Nullable
    Showing title(@Nonnull ResourceKey<Level> dimension, long nowMillis) {
        // Ended titles are cleared before starting the next, so a title that ran out under a screen is never drawn.
        if (this.title != null && this.title.hasEnded(nowMillis)) {
            this.title = null;
        }

        boolean started = this.showDueTitle(dimension, nowMillis);
        return this.title == null ? null : this.title.showing(nowMillis, started);
    }

    /**
     * Discards active and pending announcements.
     */
    @Override
    public void onClientSessionEnded() {
        this.pendingTitle = null;
        this.title = null;
    }

    /**
     * Starts a due announcement if it is eligible for the current dimension.
     *
     * @return whether a new title started
     */
    private boolean showDueTitle(ResourceKey<Level> dimension, long nowMillis) {
        PendingTitle pending = this.pendingTitle;
        if (pending == null || nowMillis < pending.dueAtMillis()) {
            return false;
        }

        this.pendingTitle = null;
        ClientBoundSettlementArrivalPacket arrival = pending.arrival();
        boolean exit = arrival.announcementType() == SettlementAnnouncementType.EXIT;
        // A pending departure from another dimension is no longer relevant and must not replace the destination's title.
        if (exit && !arrival.dimension().equals(dimension)) {
            return false;
        }

        this.title = new Surface(arrival, exit ? EXIT_TITLE : MAIN_TITLE, nowMillis);
        return true;
    }

    /**
     * A title's presentation state for one frame.
     *
     * @param arrival       the announcement to display
     * @param opacity       opacity from zero to one
     * @param elapsedMillis display time the title has spent, in milliseconds
     * @param started       whether the title started on this frame
     */
    record Showing(ClientBoundSettlementArrivalPacket arrival, float opacity, long elapsedMillis, boolean started) {
    }

    /**
     * Title animation timing, measured in milliseconds of display time.
     */
    record Timing(long fadeInMillis, long holdMillis, long fadeOutMillis) {

        static Timing of(ClockTicks fadeIn, ClockTicks hold, ClockTicks fadeOut) {
            return new Timing(ClientMonotonicClock.millisIn(fadeIn), ClientMonotonicClock.millisIn(hold),
                    ClientMonotonicClock.millisIn(fadeOut));
        }

        long totalMillis() {
            return this.fadeInMillis + this.holdMillis + this.fadeOutMillis;
        }

        float opacityAt(long elapsedMillis) {
            if (elapsedMillis < this.fadeInMillis) {
                return (float) elapsedMillis / this.fadeInMillis;
            }
            if (elapsedMillis < this.fadeInMillis + this.holdMillis) {
                return 1.0F;
            }
            return Math.max(0.0F, (float) (this.totalMillis() - elapsedMillis) / this.fadeOutMillis);
        }

    }

    /**
     * Settlement identity scoped to a dimension, since different dimensions can contain the same settlement id.
     */
    private record SettlementKey(ResourceKey<Level> dimension, UUID settlementId) {

        static SettlementKey of(ClientBoundSettlementArrivalPacket arrival) {
            return new SettlementKey(arrival.dimension(), arrival.settlementId());
        }

    }

    /**
     * An announcement awaiting display.
     *
     * @param dueAtMillis earliest eligible display time on the client's monotonic clock, in milliseconds
     */
    private record PendingTitle(ClientBoundSettlementArrivalPacket arrival, long dueAtMillis) {
    }

    /**
     * An active title with animation progress measured from its start on the client's monotonic clock.
     */
    private record Surface(ClientBoundSettlementArrivalPacket arrival, Timing timing, long startedAtMillis) {

        private boolean hasEnded(long nowMillis) {
            return nowMillis - this.startedAtMillis >= this.timing.totalMillis();
        }

        private Showing showing(long nowMillis, boolean started) {
            long elapsedMillis = nowMillis - this.startedAtMillis;
            return new Showing(this.arrival, this.timing.opacityAt(elapsedMillis), elapsedMillis, started);
        }

    }

}
