package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.dialogue.DialogueLine;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundSource;

import javax.inject.Inject;
import java.util.List;

/**
 * Dispatches a single {@link CueStep} onto the entity: triggers animations,
 * pushes FLAVOR bubbles, plays sounds, and parks gaze targets into
 * {@link SocialCueRuntimeState} for {@code LookQueries} to consume.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SocialCuePresenter {

    private static final String FLAVOR_SOURCE = "social-cue";

    private final VillagerBubbleService villagerBubbleService;

    /**
     * Dispatches one step and records any side-effects on the runtime state.
     * Noop for unknown step types so future additions do not throw at runtime.
     */
    public void dispatch(CueStep step, BaseVillager villager, SocialCueRuntimeState runtimeState) {
        switch (step) {
            case CueStep.Gesture gesture -> villager.triggerMotion(gesture.archetype());
            case CueStep.Bubble bubble -> pushFlavorBubble(villager, bubble.line(), bubble.ttl());
            case CueStep.Sound sound ->
                    Location.fromEntity(villager, true).playSound(sound.soundEvent(), sound.volume(), sound.pitch(), SoundSource.NEUTRAL);
            case CueStep.Gaze gaze -> runtimeState.setGazeLookTarget(gaze.target());
            case CueStep.Wait ignored -> {
                // Wait steps carry no dispatch: the arbiter's tick loop handles the timing.
            }
        }
    }

    private void pushFlavorBubble(BaseVillager villager, DialogueLine line, ClockTicks ttl) {
        BubbleSegment segment = switch (line) {
            case DialogueLine.Literal literal -> BubbleSegment.Text.builder()
                    .literal(literal.text())
                    .color(ChatFormatting.BLACK)
                    .bold(false)
                    .scale(0.85F)
                    .build();
            case DialogueLine.Translatable translatable -> BubbleSegment.Translatable.builder()
                    .key(translatable.key())
                    .args(translatable.args())
                    .color(ChatFormatting.BLACK)
                    .bold(false)
                    .scale(0.85F)
                    .build();
        };

        BubbleMessage message = BubbleMessage.builder()
                .priority(5)
                .ttl(ttl)
                .sourceType(FLAVOR_SOURCE)
                .segments(List.of(segment))
                .build();

        this.villagerBubbleService.applyCommand(villager, new BubbleCommand.Push(BubbleChannel.FLAVOR, message),
                villager.level().getGameTime());
    }

}
