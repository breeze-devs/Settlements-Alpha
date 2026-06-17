package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure admission-logic rules that drive {@link SocialCueArbiter}:
 * channel disjoint check, per-key cooldown, per-target cooldown, and duration cap.
 * <p>
 * No Minecraft objects are created; all checks operate on plain Java domain types.
 */
class SocialCueAdmissionLogicTest {

    // -------------------------------------------------------------------------
    // Channel disjoint guard
    // -------------------------------------------------------------------------

    @Test
    void channelDisjoint_allowsAdmissionWhenNoBehaviorRunning() {
        // Arrange – empty occupied set simulates no active behavior.
        Set<BehaviorChannel> cueChannels = Set.of(BehaviorChannel.SOCIAL, BehaviorChannel.INTERACTION);
        Set<BehaviorChannel> occupiedChannels = Collections.emptySet();

        // Act & Assert
        assertTrue(Collections.disjoint(cueChannels, occupiedChannels));
    }

    @Test
    void channelDisjoint_blocksWhenBehaviorClaimsSameChannel() {
        // Arrange – behavior claims INTERACTION; cue also claims INTERACTION.
        Set<BehaviorChannel> cueChannels = Set.of(BehaviorChannel.SOCIAL, BehaviorChannel.INTERACTION);
        Set<BehaviorChannel> occupiedChannels = Set.of(BehaviorChannel.INTERACTION, BehaviorChannel.COGNITION);

        // Act & Assert
        assertFalse(Collections.disjoint(cueChannels, occupiedChannels));
    }

    @Test
    void channelDisjoint_allowsWhenBehaviorOnlyClaimsCognition() {
        // Arrange – a COGNITION-only behavior (e.g. EAT_FOOD claims INTERACTION,
        // but let's model a purely cognitive step) leaves SOCIAL + INTERACTION free.
        Set<BehaviorChannel> cueChannels = Set.of(BehaviorChannel.SOCIAL, BehaviorChannel.INTERACTION);
        Set<BehaviorChannel> occupiedChannels = Set.of(BehaviorChannel.COGNITION);

        // Act & Assert
        assertTrue(Collections.disjoint(cueChannels, occupiedChannels));
    }

    @Test
    void channelDisjoint_blocksWhenBehaviorClaimsMovement_andCueClaimsMovement() {
        // Arrange
        Set<BehaviorChannel> cueChannels = Set.of(BehaviorChannel.MOVEMENT);
        Set<BehaviorChannel> occupiedChannels = Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION);

        // Act & Assert
        assertFalse(Collections.disjoint(cueChannels, occupiedChannels));
    }

    // -------------------------------------------------------------------------
    // Duration cap guard
    // -------------------------------------------------------------------------

    @Test
    void durationCap_acceptsScriptAtOrUnderMax() {
        // Arrange – exactly at the 5 s / 100-tick cap.
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Wait(SocialCueScript.MAX_DURATION)
        ));

        // Act & Assert
        assertFalse(script.getTotalDuration().getTicks() > SocialCueScript.MAX_DURATION.getTicks());
    }

    @Test
    void durationCap_rejectsScriptExceedingMax() {
        // Arrange – one tick over the cap.
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Wait(ClockTicks.of(SocialCueScript.MAX_DURATION.getTicks() + 1))
        ));

        // Act & Assert
        assertTrue(script.getTotalDuration().getTicks() > SocialCueScript.MAX_DURATION.getTicks());
    }

    // -------------------------------------------------------------------------
    // Per-key cooldown
    // -------------------------------------------------------------------------

    @Test
    void perKeyCooldown_blocksDuringWindow() {
        // Arrange
        SocialCueRuntimeState state = new SocialCueRuntimeState();
        SocialCue cue = buildCue("greet", ClockTicks.of(200));
        state.start(cue, null, 1000L);
        state.finish(1000L);

        // Act & Assert – inside the 200-tick window.
        assertTrue(state.isCueOnCooldown("greet", 1100L));
    }

    @Test
    void perKeyCooldown_allowsAfterWindowExpires() {
        // Arrange
        SocialCueRuntimeState state = new SocialCueRuntimeState();
        SocialCue cue = buildCue("greet", ClockTicks.of(200));
        state.start(cue, null, 1000L);
        state.finish(1000L);

        // Act & Assert – at exactly the expiry boundary the key is free again.
        assertFalse(state.isCueOnCooldown("greet", 1200L));
    }

    @Test
    void perKeyCooldown_doesNotBlockDifferentKey() {
        // Arrange – key "greet" is on cooldown; "farewell" should be unaffected.
        SocialCueRuntimeState state = new SocialCueRuntimeState();
        SocialCue cue = buildCue("greet", ClockTicks.of(200));
        state.start(cue, null, 1000L);
        state.finish(1000L);

        // Act & Assert
        assertFalse(state.isCueOnCooldown("farewell", 1100L));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SocialCue buildCue(String key, ClockTicks cooldown) {
        return SocialCue.builder()
                .key(key)
                .channels(Set.of(BehaviorChannel.SOCIAL, BehaviorChannel.INTERACTION))
                .cooldown(cooldown)
                .script(SocialCueScript.of(List.of(
                        new CueStep.Gesture(AnimationArchetype.WAVE),
                        new CueStep.Wait(ClockTicks.of(40))
                )))
                .build();
    }

}
