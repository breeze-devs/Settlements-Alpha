package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Captures the deed-level result of one behavior run.
 * <p>
 * The state is intentionally passive: behavior steps record facts as side effects happen, while
 * publisher policy decides how those facts become world events. This keeps workflow state free of
 * bus side effects and lets future behaviors add detail without widening the state registry.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BehaviorOutcome implements BehaviorState {

    @Nullable
    private WorldEventType deedType;
    @Nullable
    private String unitNoun;
    private boolean success;
    private int magnitude;
    @Nullable
    private UUID partnerId;
    @Nullable
    private UUID registryId;
    @Nullable
    private EventOutcome eventOutcome;
    @Nullable
    private String detail;
    @Nullable
    private String failureReason;
    private boolean silent;

    @Builder(access = AccessLevel.PRIVATE)
    private BehaviorOutcome(@Nullable WorldEventType deedType,
                            @Nullable String unitNoun,
                            boolean success,
                            int magnitude,
                            @Nullable UUID partnerId,
                            @Nullable UUID registryId,
                            @Nullable EventOutcome eventOutcome,
                            @Nullable String detail,
                            @Nullable String failureReason) {
        this.deedType = deedType;
        this.unitNoun = unitNoun;
        this.success = success;
        this.magnitude = magnitude;
        this.partnerId = partnerId;
        this.registryId = registryId;
        this.eventOutcome = eventOutcome;
        this.detail = detail;
        this.failureReason = failureReason;
    }

    public static BehaviorOutcome blank() {
        return BehaviorOutcome.builder()
                .build();
    }

    /**
     * A terminal outcome that suppresses all world-event publication.
     * <p>
     * Used by behaviors whose non-deed completion is not a publicly observable fact and is either
     * recorded privately or not at all (e.g. an abandoned courtship). Distinct from {@link #blank()},
     * which still falls back to the generic completed event.
     */
    public static BehaviorOutcome silent() {
        BehaviorOutcome outcome = new BehaviorOutcome();
        outcome.silent = true;
        return outcome;
    }

    public static BehaviorOutcome forDeed(@Nonnull WorldEventType deedType, @Nullable String unitNoun) {
        return BehaviorOutcome.builder()
                .deedType(deedType)
                .unitNoun(unitNoun)
                .build();
    }

    public void recordYield(int units) {
        if (units <= 0) {
            return;
        }
        this.magnitude += units;
        this.markSucceeded();
    }

    public void recordSocialOutcome(@Nullable UUID partnerId,
                                    @Nullable UUID registryId,
                                    @Nonnull EventOutcome outcome,
                                    @Nullable String detail,
                                    @Nullable String reason) {
        this.partnerId = partnerId;
        this.registryId = registryId;
        this.eventOutcome = outcome;
        this.detail = detail;
        this.failureReason = reason;
        this.success = outcome == EventOutcome.SUCCESS;
    }

    public void markSucceeded() {
        this.success = true;
        this.eventOutcome = EventOutcome.SUCCESS;
        this.failureReason = null;
    }

    public void markFailed(@Nullable String reason) {
        this.success = false;
        this.eventOutcome = EventOutcome.FAILURE;
        this.failureReason = reason;
    }

    public boolean hasDeclaredDeed() {
        return this.deedType != null;
    }

    public boolean hasExplicitEventOutcome() {
        return this.eventOutcome != null;
    }

    @Nullable
    public String resolveDetail() {
        if (this.detail != null) {
            return this.detail;
        }
        if (this.unitNoun != null) {
            return "%d %s".formatted(this.magnitude, this.unitNoun);
        }
        return null;
    }

    @Override
    public void reset() {
        this.success = false;
        this.magnitude = 0;
        this.partnerId = null;
        this.registryId = null;
        this.eventOutcome = null;
        this.detail = null;
        this.failureReason = null;
    }

}
