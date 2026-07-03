package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.application.ai.dialogue.DialogueLine;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * A single villager utterance produced once by a producer (e.g. {@code SocialCuePresenter})
 * and fanned out to every {@link VillagerSpeechSink} by {@link VillagerSpeechService}.
 */
@Builder
@Getter
public final class VillagerUtterance {

    private final BaseVillager speaker;

    private final DialogueLine line;

    private final SpeechRegister register;

    /**
     * The other villager for {@link SpeechRegister#GOSSIP} or the player for
     * {@link SpeechRegister#DIALOGUE}; null for {@link SpeechRegister#MONOLOGUE} and
     * {@link SpeechRegister#AMBIENT}.
     */
    @Nullable
    private final LivingEntity addressee;

    private final ClockTicks bubbleTtl;

}
