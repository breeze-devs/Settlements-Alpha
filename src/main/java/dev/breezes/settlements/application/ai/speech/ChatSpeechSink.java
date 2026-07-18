package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.application.ai.dialogue.DialogueLine;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mirrors villager speech into the real chat log for nearby players.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ChatSpeechSink implements VillagerSpeechSink {

    private static final EnumMap<SpeechRegister, ChatFormatting> REGISTER_COLORS = new EnumMap<>(Map.of(
            SpeechRegister.MONOLOGUE, ChatFormatting.GRAY,
            SpeechRegister.GOSSIP, ChatFormatting.GRAY,
            SpeechRegister.DIALOGUE, ChatFormatting.WHITE
    ));

    private final SpeechMirrorConfig config;

    @Override
    public void accept(VillagerUtterance utterance) {
        if (!this.config.mirrorToChat()) {
            return;
        }

        ChatFormatting color = REGISTER_COLORS.get(utterance.getRegister());
        if (color == null) {
            // No color mapping means a non-verbal caption, do not display in chat
            return;
        }

        BaseVillager speaker = utterance.getSpeaker();
        if (!(speaker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Component chatLine = Component.empty()
                .append(speaker.getDisplayName())
                .append(Component.literal(" ("))
                .append(speaker.getProfessionDisplayName())
                .append(Component.literal("): "))
                .append(renderLine(utterance.getLine()))
                .withStyle(color);

        Set<ServerPlayer> recipients = new LinkedHashSet<>(serverLevel.getEntitiesOfClass(
                ServerPlayer.class, speaker.getBoundingBox().inflate(this.config.chatRadius())));

        // DIALOGUE force-includes its addressee player even if just outside the proximity
        // radius, so the player being spoken to never misses the line.
        LivingEntity addressee = utterance.getAddressee();
        if (utterance.getRegister() == SpeechRegister.DIALOGUE && addressee instanceof ServerPlayer addresseePlayer) {
            recipients.add(addresseePlayer);
        }

        for (ServerPlayer recipient : recipients) {
            recipient.sendSystemMessage(chatLine);
        }
    }

    private static Component renderLine(DialogueLine line) {
        return switch (line) {
            case DialogueLine.Literal literal -> Component.literal(literal.text());
            case DialogueLine.Translatable translatable ->
                    Component.translatable(translatable.key(), translatable.args().toArray());
        };
    }

}
