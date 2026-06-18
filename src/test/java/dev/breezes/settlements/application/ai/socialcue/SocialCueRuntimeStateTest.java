package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialCueRuntimeStateTest {

    private SocialCueRuntimeState state;

    @BeforeEach
    void setUp() {
        this.state = new SocialCueRuntimeState();
    }

    // -------------------------------------------------------------------------
    // Active cue lifecycle
    // -------------------------------------------------------------------------

    @Test
    void isCueActive_falseOnInit() {
        assertFalse(this.state.isCueActive());
    }

    @Test
    void constructor_usesConfiguredObservationBufferCapacity() {
        // Arrange & Act
        SocialCueRuntimeState configuredState = new SocialCueRuntimeState(7);

        // Assert
        Assertions.assertEquals(7, configuredState.getObservationBuffer().capacity());
    }

    @Test
    void isCueActive_trueAfterStart() {
        // Arrange
        SocialCue cue = buildCue("greet");

        // Act
        this.state.start(cue, null, 100L);

        // Assert
        assertTrue(this.state.isCueActive());
    }

    @Test
    void finish_clearsActiveCueAndRecordsCooldown() {
        // Arrange
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 100L);

        // Act
        this.state.finish(100L);

        // Assert – cue no longer active.
        assertFalse(this.state.isCueActive());
        // Cooldown was recorded: at tick 100 + cooldown, key is still on cooldown.
        assertTrue(this.state.isCueOnCooldown("greet", 100L + cue.getCooldown().getTicks() - 1));
    }

    @Test
    void finish_cooldownExpires() {
        // Arrange
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);
        this.state.finish(0L);

        // Act & Assert – at exactly the expiry tick the cooldown is gone.
        assertFalse(this.state.isCueOnCooldown("greet", cue.getCooldown().getTicks()));
    }

    // -------------------------------------------------------------------------
    // Per-target cooldown
    // -------------------------------------------------------------------------

    @Test
    void targetCooldown_respectsRecordedExpiry() {
        // Arrange
        UUID playerId = UUID.randomUUID();

        // Act
        this.state.recordTargetGreeted(playerId, 0L, 200L);

        // Assert
        assertTrue(this.state.isTargetOnCooldown(playerId, 0L));
        assertTrue(this.state.isTargetOnCooldown(playerId, 199L));
        assertFalse(this.state.isTargetOnCooldown(playerId, 200L));
    }

    @Test
    void targetCooldown_unknownTargetIsNotOnCooldown() {
        // Arrange & Act
        UUID unknown = UUID.randomUUID();

        // Assert
        assertFalse(this.state.isTargetOnCooldown(unknown, 100L));
    }

    // -------------------------------------------------------------------------
    // Gaze slot
    // -------------------------------------------------------------------------

    @Test
    void gazeTarget_nullWhenNoActiveCue() {
        assertNull(this.state.getGazeLookTarget());
    }

    @Test
    void gazeTarget_setAndClearedOnFinish() {
        // Arrange
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);
        this.state.setGazeLookTarget(
                Location.of(10, 64, 10, null));

        // Assert – set while active.
        assertNotNull(this.state.getGazeLookTarget());

        // Act
        this.state.finish(0L);

        // Assert – cleared on finish.
        assertNull(this.state.getGazeLookTarget());
    }

    // -------------------------------------------------------------------------
    // Step advancement
    // -------------------------------------------------------------------------

    @Test
    void advance_incrementsNextStepIndex() {
        // Arrange
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);

        // Act
        this.state.advance();
        this.state.advance();

        // Assert
        Assertions.assertEquals(2, this.state.getNextStepIndex());
    }

    @Test
    void reset_clearsAllState() {
        // Arrange
        UUID playerId = UUID.randomUUID();
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 100L);
        this.state.recordTargetGreeted(playerId, 0L, 9999L);

        // Act
        this.state.reset();

        // Assert
        assertFalse(this.state.isCueActive());
        assertFalse(this.state.isCueOnCooldown("greet", 100L));
        assertFalse(this.state.isTargetOnCooldown(playerId, 0L));
    }

    // -------------------------------------------------------------------------
    // Admission scan cadence
    // -------------------------------------------------------------------------

    @Test
    void admissionScan_dueOnFirstTickAfterInit() {
        // A freshly constructed state schedules no scan, so any non-negative tick is already due.
        Assertions.assertEquals(0L, this.state.getNextAdmissionScanTick());
    }

    @Test
    void scheduleNextAdmissionScan_recordsNextDueTick() {
        // Act
        this.state.scheduleNextAdmissionScan(1020L);

        // Assert
        Assertions.assertEquals(1020L, this.state.getNextAdmissionScanTick());
    }

    @Test
    void reset_clearsScheduledAdmissionScan() {
        // Arrange
        this.state.scheduleNextAdmissionScan(5000L);

        // Act
        this.state.reset();

        // Assert – next scan falls back to "due immediately".
        Assertions.assertEquals(0L, this.state.getNextAdmissionScanTick());
    }

    // -------------------------------------------------------------------------
    // targetCooldowns sweep on finish
    // -------------------------------------------------------------------------

    @Test
    void finish_sweepsExpiredTargetCooldowns() {
        // Arrange – record two targets, one whose cooldown expires at tick 100 and one that
        // does not expire until tick 500. Start and then finish a cue at tick 100 so the sweep
        // runs with gameTime = 100.
        UUID expiredTarget = UUID.randomUUID();
        UUID activeTarget = UUID.randomUUID();
        this.state.recordTargetGreeted(expiredTarget, 0L, 100L);  // expiry = 100 (expired at tick 100)
        this.state.recordTargetGreeted(activeTarget, 0L, 500L);   // expiry = 500 (still active)

        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);

        // Act – finish at exactly tick 100; expiredTarget's entry has expiry == 100 so it is swept.
        this.state.finish(100L);

        // Assert – expired entry is gone, active entry survives.
        assertFalse(this.state.isTargetOnCooldown(expiredTarget, 50L),
                "expired entry should have been swept");
        assertTrue(this.state.isTargetOnCooldown(activeTarget, 100L),
                "still-active entry must not be swept");
    }

    @Test
    void finish_retainsAllEntriesWhenNoneExpired() {
        // Arrange – two targets both with cooldowns that haven't expired by finish time.
        UUID targetA = UUID.randomUUID();
        UUID targetB = UUID.randomUUID();
        this.state.recordTargetGreeted(targetA, 0L, 999L);
        this.state.recordTargetGreeted(targetB, 0L, 999L);

        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);

        // Act – finish early; neither entry has expired.
        this.state.finish(50L);

        // Assert – both entries are retained.
        assertTrue(this.state.isTargetOnCooldown(targetA, 50L));
        assertTrue(this.state.isTargetOnCooldown(targetB, 50L));
    }

    @Test
    void finish_removesAllEntriesWhenAllExpired() {
        // Arrange – three targets all expired by finish time.
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        UUID t3 = UUID.randomUUID();
        this.state.recordTargetGreeted(t1, 0L, 10L);
        this.state.recordTargetGreeted(t2, 0L, 20L);
        this.state.recordTargetGreeted(t3, 0L, 5L);

        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);

        // Act – finish well past all expiry times.
        this.state.finish(1000L);

        // Assert – map is empty; no stale entries remain.
        assertFalse(this.state.isTargetOnCooldown(t1, 0L));
        assertFalse(this.state.isTargetOnCooldown(t2, 0L));
        assertFalse(this.state.isTargetOnCooldown(t3, 0L));
    }

    // -------------------------------------------------------------------------
    // Explicit-cooldown overload of finish
    // -------------------------------------------------------------------------

    @Test
    void finish_withExplicitCooldown_recordsThatCooldownNotCueCooldown() {
        // Arrange
        SocialCue cue = buildCue("greet"); // base cooldown = 100 ticks
        this.state.start(cue, null, 0L);
        long explicitCooldown = 777L;

        // Act
        this.state.finish(0L, explicitCooldown);

        // Assert: the cue key is gated by the explicit value, not the cue's own 100-tick cooldown.
        assertTrue(this.state.isCueOnCooldown("greet", 776L),
                "should still be on cooldown one tick before explicit expiry");
        assertFalse(this.state.isCueOnCooldown("greet", 777L),
                "should be off cooldown at the explicit expiry tick");
        // Verify the explicit cooldown (777) is longer than the cue's base (100): still active at 500.
        assertTrue(this.state.isCueOnCooldown("greet", 500L),
                "should still be on cooldown well past the cue's own base (100 ticks) — explicit cooldown governs");
    }

    @Test
    void finish_withExplicitCooldown_clearsCueAndGaze() {
        // Arrange
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);
        this.state.setGazeLookTarget(Location.of(1, 64, 1, null));

        // Act
        this.state.finish(0L, 500L);

        // Assert
        assertFalse(this.state.isCueActive());
        assertNull(this.state.getGazeLookTarget());
    }

    // -------------------------------------------------------------------------
    // admissionScanInitialized flag
    // -------------------------------------------------------------------------

    @Test
    void admissionScanInitialized_falseOnInit() {
        assertFalse(this.state.isAdmissionScanInitialized());
    }

    @Test
    void markAdmissionScanInitialized_flipsFlag() {
        // Act
        this.state.markAdmissionScanInitialized();

        // Assert
        assertTrue(this.state.isAdmissionScanInitialized());
    }

    @Test
    void reset_clearsAdmissionScanInitializedFlag() {
        // Arrange
        this.state.markAdmissionScanInitialized();

        // Act
        this.state.reset();

        // Assert
        assertFalse(this.state.isAdmissionScanInitialized());
    }

    // -------------------------------------------------------------------------
    // cancelActiveCue — sleep/suppression path
    // -------------------------------------------------------------------------

    @Test
    void cancelActiveCue_clearsCueAndGaze_withoutRecordingCooldown() {
        // Arrange – start a cue so the lane is active and a gaze target is set.
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 100L);
        this.state.setGazeLookTarget(Location.of(5, 64, 5, null));

        // Act – cancel mid-flight (e.g. villager falls asleep).
        this.state.cancelActiveCue();

        // Assert – lane is idle and gaze is cleared.
        assertFalse(this.state.isCueActive());
        assertNull(this.state.getGazeLookTarget());
        // No cooldown was recorded: the cue key should be admissible again immediately.
        assertFalse(this.state.isCueOnCooldown("greet", 100L),
                "cancelled cue must not impose a cooldown — the lane should be fully open on wake-up");
    }

    @Test
    void cancelActiveCue_resetsStepIndex() {
        // Arrange – advance a step so nextStepIndex > 0.
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);
        this.state.advance();

        // Act
        this.state.cancelActiveCue();

        // Assert – step index reset so a future start() begins from step 0.
        Assertions.assertEquals(0, this.state.getNextStepIndex());
    }

    @Test
    void cancelActiveCue_noOp_whenNoCueIsActive() {
        // Arrange – no cue running.
        assertFalse(this.state.isCueActive());

        // Act – should not throw and state should remain clean.
        this.state.cancelActiveCue();

        // Assert
        assertFalse(this.state.isCueActive());
        assertNull(this.state.getGazeLookTarget());
    }

    @Test
    void cancelActiveCue_targetCooldownsUnaffected() {
        // Arrange – a per-target cooldown exists before and during a cue.
        UUID playerId = UUID.randomUUID();
        this.state.recordTargetGreeted(playerId, 0L, 500L);
        SocialCue cue = buildCue("greet");
        this.state.start(cue, null, 0L);

        // Act – cancel (e.g. sleep interrupts the wave).
        this.state.cancelActiveCue();

        // Assert – target cooldown must survive; we already greeted them.
        assertTrue(this.state.isTargetOnCooldown(playerId, 0L),
                "per-target cooldown must not be cleared by a cancel — the greeting already happened");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SocialCue buildCue(String key) {
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(40))
        ));

        return SocialCue.builder()
                .key(key)
                .channels(Set.of(BehaviorChannel.SOCIAL, BehaviorChannel.INTERACTION))
                .cooldown(ClockTicks.of(100))
                .script(script)
                .build();
    }

}
