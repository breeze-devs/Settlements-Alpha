package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Configuration record for dialogue behavior.
 * <p>
 * {@link DialogueMode#SCRIPTED} is the default — no dialog service is required, and the mod remains
 * fully playable with no inference backend service reachable.
 */
@BehaviorConfig(name = "dialogue", type = ConfigurationType.GENERAL)
public record DialogueConfig(

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "mode",
                description = "Dialog mode: SCRIPTED (default, localized built-in lines) or REHEARSED (dialog service batch with SCRIPTED fallback).",
                defaultValue = "SCRIPTED")
        String mode,

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
        int bubbleCharCap,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "pack_lines_per_villager",
                description = "How many candidate lines to generate per villager in the evening REHEARSED sweep.",
                defaultValue = 12,
                min = 1,
                max = 50)
        int packLinesPerVillager,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "pack_sweep_deadline_seconds",
                description = "Total time budget in seconds for the evening REHEARSED sweep across all villagers.",
                defaultValue = 30,
                min = 5,
                max = 120)
        int packSweepDeadlineSeconds

) {

    /**
     * Resolves the {@code mode} string to a {@link DialogueMode} enum value
     * Unrecognized strings default to {@link DialogueMode#SCRIPTED}
     */
    public DialogueMode resolvedMode() {
        try {
            return DialogueMode.valueOf(this.mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DialogueMode.SCRIPTED;
        }
    }

}
