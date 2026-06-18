package dev.breezes.settlements.domain.ai.eventlane;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * TODO: maybe we can refactor the gossip or social ones into another config?
 * Tuning knobs for the event-lane subsystem: WorldEventBus TTL, observation buffer capacity,
 * knowledge store capacity, gossip/social-cue cadence, and credibility decay rate.
 * <p>
 * Defaults intentionally favor quieter gossip while leaving ambient chatter close to its
 * current cadence; the CHA multiplier range gives unsociable villagers much longer waits.
 * <p>
 * The Minecraft entity constructor path is not Dagger-created, so per-villager stores read
 * these values through the current server component with constant fallbacks during early bootstrap.
 */
@BehaviorConfig(name = "event_lane", type = ConfigurationType.GENERAL)
public record EventLaneConfig(

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "world_event_ttl_ticks",
                description = "How many ticks a world event remains in the bus before being evicted (~5 s at 20 tps = 100 ticks)",
                defaultValue = 100,
                min = 20,
                max = 12_000)
        int worldEventTtlTicks,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "observation_buffer_capacity",
                description = "Maximum observations buffered per villager per tick before older ones are dropped",
                defaultValue = 50,
                min = 10,
                max = 500)
        int observationBufferCapacity,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "knowledge_store_max_entries",
                description = "Maximum episodic knowledge entries a villager retains (oldest evicted when full)",
                defaultValue = 200,
                min = 10,
                max = 2_000)
        int knowledgeStoreMaxEntries,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "gossip_max_distance_squared",
                description = "Maximum squared block distance between two villagers for gossip to be possible",
                defaultValue = 25,
                min = 4,
                max = 400)
        int gossipMaxDistanceSquared,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "villager_chatter_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between ambient villager chatter bubbles",
                defaultValue = 120,
                min = 5,
                max = 86_400)
        int villagerChatterCooldownSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "gossip_initiate_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between attempts by one villager to initiate gossip",
                defaultValue = 120,
                min = 5,
                max = 86_400)
        int gossipInitiateCooldownSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "gossip_accept_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between gossip accept cues for one villager",
                defaultValue = 10,
                min = 1,
                max = 86_400)
        int gossipAcceptCooldownSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "gossip_target_cooldown_seconds",
                description = "Exact per-receiver cooldown after one villager initiates gossip with another villager",
                defaultValue = 300,
                min = 5,
                max = 86_400)
        int gossipTargetCooldownSeconds,

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
        String socialCueCharismaCooldownScaling,

        @FloatConfig(
                type = ConfigurationType.GENERAL,
                identifier = "credibility_decay_per_tick",
                description = "Per-tick fraction of the (score - neutral) deviation that decays toward neutral, " +
                        "applied in 20-tick batches at 1 Hz. Default ~9.627e-6 gives a 3-day (72000-tick) half-life.",
                defaultValue = 9.627044E-6f,
                min = 0.0f,
                max = 0.5f)
        float credibilityDecayPerTick

) {

}
