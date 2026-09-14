package dev.breezes.settlements.domain.ballista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BallistaIntentPredictionTest {

    // Far longer than any echo takes, so each test reads a hold as long over
    private static final long LONG_AFTER = 10_000L;

    private static final float EARLIER_YAW = 10.0F;
    private static final float PREDICTED_YAW = 40.0F;
    private static final float OTHER_PLAYERS_YAW = -120.0F;
    private static final float PITCH = 5.0F;

    @Test
    void receive_keepsThePrediction_whenAnEchoOfAnEarlierIntentArrivesJustAfterIt() {
        // Arrange
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);

        // Act
        BallistaAim received = prediction.receive(predicted, EARLIER_YAW, PITCH, 1L);

        // Assert: applied, the stale echo would swing the barrel back to where the player just turned it from
        assertEquals(PREDICTED_YAW, received.getIntentYaw());
    }

    @Test
    void settle_appliesTheLatestHeldIntent_onceTheHoldEnds() {
        // Arrange: another player's adjustment reaches this client while its own player's prediction holds
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);
        BallistaAim held = prediction.receive(predicted, PREDICTED_YAW, PITCH, 1L);
        held = prediction.receive(held, OTHER_PLAYERS_YAW, PITCH, 2L);

        // Act
        BallistaAim settled = prediction.settle(held, LONG_AFTER);

        // Assert: the server's last word wins, or two clients would disagree on the heading for good
        assertEquals(OTHER_PLAYERS_YAW, settled.getIntentYaw());
    }

    @Test
    void settle_leavesThePrediction_whileTheHoldLasts() {
        // Arrange
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);
        BallistaAim held = prediction.receive(predicted, EARLIER_YAW, PITCH, 1L);

        // Act
        BallistaAim settled = prediction.settle(held, 2L);

        // Assert
        assertEquals(PREDICTED_YAW, settled.getIntentYaw());
    }

    @Test
    void receive_appliesAtOnce_withNoLocalAdjustment() {
        // Arrange
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim aim = BallistaAim.facing(EARLIER_YAW, PITCH);

        // Act
        BallistaAim received = prediction.receive(aim, OTHER_PLAYERS_YAW, PITCH, 0L);

        // Assert: a client that is not adjusting has nothing to protect, and follows another player's adjustment
        assertEquals(OTHER_PLAYERS_YAW, received.getIntentYaw());
    }

    @Test
    void receive_appliesAtOnce_afterTheHoldHasEnded() {
        // Arrange
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);

        // Act
        BallistaAim received = prediction.receive(predicted, OTHER_PLAYERS_YAW, PITCH, LONG_AFTER);

        // Assert
        assertEquals(OTHER_PLAYERS_YAW, received.getIntentYaw());
    }

    @Test
    void settle_doesNotReapplyAnIntentAlreadyOverriddenByALaterOne() {
        // Arrange: an intent held during one hold, then a later one applied at once after it ended
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);
        BallistaAim aim = prediction.receive(predicted, EARLIER_YAW, PITCH, 1L);
        aim = prediction.receive(aim, OTHER_PLAYERS_YAW, PITCH, LONG_AFTER);

        // Act
        BallistaAim settled = prediction.settle(aim, LONG_AFTER + 1L);

        // Assert: the kept intent is older than the one the server sent last
        assertEquals(OTHER_PLAYERS_YAW, settled.getIntentYaw());
    }

    @Test
    void predictedAt_extendsTheHold_fromTheLatestAdjustment() {
        // Arrange: a second adjustment made late in the first one's hold
        BallistaIntentPrediction prediction = new BallistaIntentPrediction();
        BallistaAim predicted = BallistaAim.facing(EARLIER_YAW, PITCH).withIntent(PREDICTED_YAW, PITCH);
        prediction.predictedAt(0L);
        long secondAdjustment = LONG_AFTER;
        prediction.predictedAt(secondAdjustment);

        // Act
        BallistaAim received = prediction.receive(predicted, EARLIER_YAW, PITCH, secondAdjustment + 1L);

        // Assert: a hold timed from the first adjustment would let the second one's stale echoes through
        assertEquals(PREDICTED_YAW, received.getIntentYaw());
    }

}
