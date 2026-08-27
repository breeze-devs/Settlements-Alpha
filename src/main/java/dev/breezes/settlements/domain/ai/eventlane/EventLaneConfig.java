package dev.breezes.settlements.domain.ai.eventlane;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * Tuning knobs for the event-lane subsystem: {@code WorldEventBus} TTL, observation buffer
 * capacity, knowledge store capacity, and gossip cadence and range.
 * <p>
 * The perception knobs take effect only while the SIS kill-switch is on, since the lane they tune is
 * not ticked otherwise. The gossip knobs apply either way — the gossip cues are presentation and run
 * regardless of the switch.
 * <p>
 * TODO: move the gossip knobs to {@code SocialCueConfig} ({@code general.toml}) — they tune an
 *  always-on lane, and an operator with inference off should not have to edit the inference config
 *  to change live behavior.
 * <p>
 * The Minecraft entity constructor path is not Dagger-created, so per-villager stores read
 * these values through the current server component with constant fallbacks during early bootstrap.
 */
@BehaviorConfig(name = "event_lane", type = ConfigurationType.INFERENCE)
public record EventLaneConfig(

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "world_event_ttl_ticks",
                description = "How many ticks a world event remains in the bus before being evicted (~5 s at 20 tps = 100 ticks)",
                defaultValue = 100,
                min = 20,
                max = 12_000)
        int worldEventTtlTicks,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "observation_buffer_capacity",
                description = "Maximum observations buffered per villager per tick before older ones are dropped",
                defaultValue = 50,
                min = 10,
                max = 500)
        int observationBufferCapacity,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "knowledge_store_max_entries",
                description = "Maximum episodic knowledge entries a villager retains (oldest evicted when full)",
                defaultValue = 200,
                min = 10,
                max = 2_000)
        int knowledgeStoreMaxEntries,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "gossip_max_distance_squared",
                description = "Maximum squared block distance between two villagers for gossip to be possible",
                defaultValue = 25,
                min = 4,
                max = 400)
        int gossipMaxDistanceSquared,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "gossip_initiate_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between attempts by one villager to initiate gossip",
                defaultValue = 120,
                min = 5,
                max = 86_400)
        int gossipInitiateCooldownSeconds,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "gossip_accept_cooldown_seconds",
                description = "Base cooldown, before charisma and jitter, between gossip accept cues for one villager",
                defaultValue = 10,
                min = 1,
                max = 86_400)
        int gossipAcceptCooldownSeconds,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "gossip_target_cooldown_seconds",
                description = "Exact per-receiver cooldown after one villager initiates gossip with another villager",
                defaultValue = 300,
                min = 5,
                max = 86_400)
        int gossipTargetCooldownSeconds

) {

}
