package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Always-on scripted-cue cadence knobs, split out of
 * {@link dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig}.
 * <p>
 * Unlike gossip cadence (which only matters with the SIS kill-switch on), ambient chatter and the
 * charisma-scaled cooldown curve {@link SocialCueArbiter} applies to every cue keep firing in
 * dumb mode, so they stay in {@code general.toml}.
 */
@BehaviorConfig(name = "social_cue", type = ConfigurationType.GENERAL)
public record SocialCueConfig(

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "villager_chatter_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between ambient villager chatter bubbles",
                defaultValue = 120,
                min = 5,
                max = 86_400)
        int villagerChatterCooldownSeconds,

        @DoubleConfig(
                type = ConfigurationType.GENERAL,
                identifier = "social_cue_low_charisma_cooldown_multiplier",
                description = "Cooldown multiplier applied at CHARISMA=0.0 before jitter; larger values make low-CHA villagers quieter",
                defaultValue = 4.0,
                min = 0.01,
                max = 100.0)
        double socialCueLowCharismaCooldownMultiplier,

        @DoubleConfig(
                type = ConfigurationType.GENERAL,
                identifier = "social_cue_high_charisma_cooldown_multiplier",
                description = "Cooldown multiplier applied at CHARISMA=1.0 before jitter; smaller values make high-CHA villagers more talkative",
                defaultValue = 0.5,
                min = 0.01,
                max = 100.0)
        double socialCueHighCharismaCooldownMultiplier,

        @DoubleConfig(
                type = ConfigurationType.GENERAL,
                identifier = "social_cue_cooldown_jitter_fraction",
                description = "Random per-cue cooldown jitter half-width; 0.25 means each completed cue varies by +/-25% after charisma scaling",
                defaultValue = 0.25,
                min = 0.0,
                max = 1.0)
        double socialCueCooldownJitterFraction,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "social_cue_charisma_cooldown_scaling",
                description = "How CHARISMA maps between low/high cooldown multipliers. Supported values: linear, exponential",
                defaultValue = "exponential")
        String socialCueCharismaCooldownScaling

) {

}
