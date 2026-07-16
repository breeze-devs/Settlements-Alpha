package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * SIS-backed dialogue knobs, split out of {@link DialogueConfig}. Only matters when
 * the Settlements Inference Service is reachable.
 */
@BehaviorConfig(name = "rehearsed_dialogue", type = ConfigurationType.INFERENCE)
public record RehearsedDialogueConfig(

        @StringConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "mode",
                description = "Dialog mode: SCRIPTED (default, localized built-in lines) or REHEARSED (dialog service batch with SCRIPTED fallback).",
                defaultValue = "SCRIPTED")
        String mode,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "pack_lines_per_villager",
                description = "How many candidate lines to generate per villager in the evening REHEARSED sweep.",
                defaultValue = 12,
                min = 1,
                max = 50)
        int packLinesPerVillager,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "pack_sweep_deadline_seconds",
                description = "Total time budget in seconds for the evening REHEARSED sweep across all villagers.",
                defaultValue = 60,
                min = 5,
                max = 300)
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
