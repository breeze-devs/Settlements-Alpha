package dev.breezes.settlements.domain.ai.eventlane;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * Tuning knobs for the event-lane subsystem: WorldEventBus TTL, observation buffer capacity,
 * knowledge store capacity, gossip radius, and credibility decay rate.
 * <p>
 * Defaults mirror the current hard-coded constants so existing deployments are unaffected
 * by first-time config file creation.
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
