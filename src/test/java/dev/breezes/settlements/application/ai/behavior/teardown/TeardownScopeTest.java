package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeardownScopeTest {

    // -------------------------------------------------------------------------
    // Fake obligation — records discharge calls without touching MC objects.
    // ServerLevel is never used; null is safe to pass in tests.
    // -------------------------------------------------------------------------

    private static class RecordingObligation implements TeardownObligation {

        private final String id;
        private final List<String> dischargeLog;
        private final boolean shouldThrow;

        RecordingObligation(String id, List<String> dischargeLog) {
            this(id, dischargeLog, false);
        }

        RecordingObligation(String id, List<String> dischargeLog, boolean shouldThrow) {
            this.id = id;
            this.dischargeLog = dischargeLog;
            this.shouldThrow = shouldThrow;
        }

        @Override
        public BlockPos targetPos() {
            return BlockPos.ZERO;
        }

        @Override
        public boolean stillValid(ServerLevel level) {
            // Always valid — scope tests don't exercise chunk-loading or entity presence logic
            return true;
        }

        @Override
        public void discharge(ServerLevel level) {
            this.dischargeLog.add(this.id);
            if (this.shouldThrow) {
                throw new RuntimeException("Simulated discharge failure for " + this.id);
            }
        }

        @Override
        public boolean durable() {
            return true;
        }

        @Override
        public String describe() {
            return "recording:" + this.id;
        }

    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void teardownAll_dischargesInLifoOrder() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", log));
        scope.track(new RecordingObligation("B", log));
        scope.track(new RecordingObligation("C", log));

        // Act — null ServerLevel is safe because RecordingObligation ignores it
        scope.teardownAll(null);

        // Assert — C tracked last, discharged first
        assertEquals(List.of("C", "B", "A"), log);
    }

    @Test
    void teardownAll_isolatesExceptions_allObligationsAttempted() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", log));
        scope.track(new RecordingObligation("B", log, true)); // B throws
        scope.track(new RecordingObligation("C", log));

        // Act
        scope.teardownAll(null);

        // Assert — B's exception is caught; A and C still run
        assertEquals(3, log.size());
        assertTrue(log.contains("A"));
        assertTrue(log.contains("B"));
        assertTrue(log.contains("C"));
    }

    @Test
    void teardownAll_isIdempotent() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", log));

        // Act
        scope.teardownAll(null);
        scope.teardownAll(null);

        // Assert — second call is a no-op on an empty scope
        assertEquals(1, log.size());
    }

    @Test
    void artifactHandle_dispose_dischargesAndRemovesFromScope() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        TemporaryArtifactHandle handle = scope.track(new RecordingObligation("A", log));

        // Act
        handle.dispose(null);

        // Assert — obligation discharged
        assertEquals(List.of("A"), log);
        // Assert — no longer open; subsequent teardownAll is a no-op
        scope.teardownAll(null);
        assertEquals(1, log.size());
    }

    @Test
    void artifactHandle_dispose_isIdempotent() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        TemporaryArtifactHandle handle = scope.track(new RecordingObligation("A", log));

        // Act
        handle.dispose(null);
        handle.dispose(null);

        // Assert — discharge called exactly once
        assertEquals(1, log.size());
        assertTrue(handle.isDisposed());
    }

    @Test
    void teardownAll_afterDispose_doesNotDoubleDischarge() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", log));
        TemporaryArtifactHandle handleB = scope.track(new RecordingObligation("B", log));
        scope.track(new RecordingObligation("C", log));

        // Act — mid-run dispose of B, then full teardown
        handleB.dispose(null);
        scope.teardownAll(null);

        // Assert — each obligation discharged exactly once
        assertEquals(3, log.size());
        assertEquals(1, log.stream().filter("B"::equals).count());
    }

    @Test
    void teardownAll_skipsNotValidObligations_withoutError() {
        // Arrange
        List<String> log = new ArrayList<>();
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", log));
        // B reports itself as no longer valid (entity gone / replaced)
        scope.track(new RecordingObligation("B", log) {
            @Override
            public boolean stillValid(ServerLevel level) {
                return false;
            }
        });
        scope.track(new RecordingObligation("C", log));

        // Act
        scope.teardownAll(null);

        // Assert — B is skipped (not an error), A and C discharged
        assertEquals(2, log.size());
        assertTrue(log.contains("A"));
        assertFalse(log.contains("B"));
        assertTrue(log.contains("C"));
    }

    @Test
    void openObligations_reflectsCurrentScope() {
        // Arrange
        TeardownScope scope = new TeardownScope();
        scope.track(new RecordingObligation("A", new ArrayList<>()));
        TemporaryArtifactHandle handleB = scope.track(new RecordingObligation("B", new ArrayList<>()));

        // Assert — two open
        assertEquals(2, scope.openObligations().size());

        // Act
        handleB.dispose(null);

        // Assert — one remaining
        assertEquals(1, scope.openObligations().size());
        assertFalse(scope.openObligations().isEmpty());
    }

}
