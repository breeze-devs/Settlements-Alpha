package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.application.ai.dialogue.DialogueLine;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Renders every {@link VillagerUtterance} onto the villager's FLAVOR bubble channel.
 * <p>
 * This is a mechanical move of the styling that used to live directly in
 * {@code SocialCuePresenter}.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BubbleSpeechSink implements VillagerSpeechSink {

    private static final String FLAVOR_SOURCE = "social-cue";

    private final VillagerBubbleService villagerBubbleService;

    @Override
    public void accept(VillagerUtterance utterance) {
        BaseVillager speaker = utterance.getSpeaker();

        // Every register shares today's monologue styling for now; per-register bubble
        // styling (e.g. distinguishing GOSSIP/DIALOGUE from plain MONOLOGUE) lands with
        // those producers once they exist.
        BubbleSegment segment = switch (utterance.getLine()) {
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
                .ttl(utterance.getBubbleTtl())
                .sourceType(FLAVOR_SOURCE)
                .segments(List.of(segment))
                .build();

        this.villagerBubbleService.applyCommand(speaker, new BubbleCommand.Push(BubbleChannel.FLAVOR, message),
                speaker.level().getGameTime());
    }

}
