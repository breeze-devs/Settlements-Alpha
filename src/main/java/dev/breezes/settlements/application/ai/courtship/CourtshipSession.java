package dev.breezes.settlements.application.ai.courtship;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.UUID;

@Getter
@Builder
public final class CourtshipSession {

    private final UUID sessionId;
    /**
     * The presenter is always the villager with the lower UUID, derived by CourtshipRole.of().
     * Both sides compute their role independently from these two IDs.
     */
    private final UUID presenterId;
    private final UUID receiverId;

    private int courtshipDurationTicks;

    private CourtshipPhase phase;
    private long phaseEnteredGameTime;

    @Builder.Default
    private long courtshipStartGameTime = -1L;
    @Builder.Default
    private int currentBeatIndex = 0;

    /**
     * Which choreography routine the presenter selected for this session.
     * Stored so the receiver can look up the same timeline independently.
     */
    @Setter
    @Builder.Default
    private int choreographyId = 0;

    private boolean birthCompleted;

    public void transitionTo(@Nonnull CourtshipPhase next, long now) {
        this.phase = next;
        this.phaseEnteredGameTime = now;
    }

    /**
     * Records the start of the active courtship dance, overwriting the initial duration estimate
     * with the true timeline duration so registry timeouts stay accurate.
     */
    public void beginCourtship(long now, int actualDurationTicks) {
        this.courtshipStartGameTime = now;
        this.courtshipDurationTicks = actualDurationTicks;
        this.phase = CourtshipPhase.COURTSHIP;
        this.phaseEnteredGameTime = now;
    }

    public void advanceBeat(int beatIndex) {
        this.currentBeatIndex = beatIndex;
    }

    public void markBirthCompleted() {
        this.birthCompleted = true;
    }

    public UUID partnerOf(@Nonnull UUID villagerId) {
        return villagerId.equals(this.presenterId) ? this.receiverId : this.presenterId;
    }

}
