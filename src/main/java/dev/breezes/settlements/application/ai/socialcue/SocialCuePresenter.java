package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.speech.VillagerSpeechService;
import dev.breezes.settlements.application.ai.speech.VillagerUtterance;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.sounds.SoundSource;

import javax.inject.Inject;

/**
 * Dispatches a single {@link CueStep} onto the entity: triggers animations,
 * produces utterances via {@link VillagerSpeechService}, plays sounds, and parks gaze
 * targets into {@link SocialCueRuntimeState} for {@code LookQueries} to consume.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SocialCuePresenter {

    private final VillagerSpeechService villagerSpeechService;

    /**
     * Dispatches one step and records any side-effects on the runtime state.
     * Noop for unknown step types so future additions do not throw at runtime.
     */
    public void dispatch(CueStep step, BaseVillager villager, SocialCueRuntimeState runtimeState) {
        switch (step) {
            case CueStep.Gesture gesture -> villager.triggerMotion(gesture.archetype());
            case CueStep.Speak speak -> this.villagerSpeechService.utter(VillagerUtterance.builder()
                    .speaker(villager)
                    .line(speak.line())
                    .register(speak.register())
                    .addressee(null)
                    .bubbleTtl(speak.ttl())
                    .build());
            case CueStep.Sound sound ->
                    Location.fromEntity(villager, true).playSound(sound.soundEvent(), sound.volume(), sound.pitch(), SoundSource.NEUTRAL);
            case CueStep.Gaze gaze -> runtimeState.setGazeLookTarget(gaze.target());
            case CueStep.Wait ignored -> {
                // Wait steps carry no dispatch: the arbiter's tick loop handles the timing.
            }
        }
    }

}
