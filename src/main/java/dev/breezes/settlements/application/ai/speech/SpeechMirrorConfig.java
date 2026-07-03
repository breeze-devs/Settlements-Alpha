package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * Runtime toggle for mirroring villager speech (monologue/gossip/dialogue) into the chat log.
 */
@BehaviorConfig(name = "speech_mirror", type = ConfigurationType.FEATURE)
public record SpeechMirrorConfig(
        @BooleanConfig(
                type = ConfigurationType.FEATURE,
                identifier = "mirror_to_chat",
                description = "When true, villager speech (monologue/gossip/dialogue) is echoed into the chat log",
                defaultValue = false)
        boolean mirrorToChat,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "chat_radius",
                description = "Radius in blocks around a speaking villager within which players receive the chat mirror",
                defaultValue = 16,
                min = 1,
                max = 128)
        int chatRadius
) {

}
