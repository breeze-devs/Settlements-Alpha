package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pure fan-out test. {@code speaker}/{@code line}/{@code addressee} are left null since
 * {@link VillagerSpeechService#utter} only forwards the reference to each sink and never
 * dereferences it - no Minecraft object is touched here.
 */
class VillagerSpeechServiceTest {

    @Test
    void utter_dispatchesToEverySinkExactlyOnce() {
        // Arrange
        List<VillagerUtterance> firstSinkCalls = new ArrayList<>();
        List<VillagerUtterance> secondSinkCalls = new ArrayList<>();
        VillagerSpeechSink firstSink = firstSinkCalls::add;
        VillagerSpeechSink secondSink = secondSinkCalls::add;
        VillagerSpeechService service = new VillagerSpeechService(Set.of(firstSink, secondSink));

        VillagerUtterance utterance = VillagerUtterance.builder()
                .speaker(null)
                .line(null)
                .register(SpeechRegister.MONOLOGUE)
                .addressee(null)
                .bubbleTtl(ClockTicks.seconds(5))
                .build();

        // Act
        service.utter(utterance);

        // Assert
        assertEquals(1, firstSinkCalls.size());
        assertEquals(1, secondSinkCalls.size());
        assertSame(utterance, firstSinkCalls.get(0));
        assertSame(utterance, secondSinkCalls.get(0));
    }

}
