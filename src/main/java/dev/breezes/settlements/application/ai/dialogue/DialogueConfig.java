package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * Player-facing dialogue configuration that stays meaningful with the SIS kill-switch off.
 */
@BehaviorConfig(name = "dialogue", type = ConfigurationType.GENERAL)
public record DialogueConfig(

        @BooleanConfig(
                type = ConfigurationType.GENERAL,
                identifier = "scripted_chatter",
                description = "Whether the backend-free SCRIPTED dialogue floor may emit ambient chatter.",
                defaultValue = true)
        boolean scriptedChatter,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "bubble_char_cap",
                description = "Maximum characters of generated literal text shown in a bubble after defensive truncation.",
                defaultValue = 120,
                min = 20,
                max = 300)
        int bubbleCharCap

) {

}
