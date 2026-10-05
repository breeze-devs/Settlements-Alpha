package dev.breezes.settlements.infrastructure.minecraft.behavior.threat;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "threat_golem_summon", type = ConfigurationType.FEATURE)
public record ThreatGolemSummonConfig(

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "guardian_cap",
                description = "Villagers stop summoning iron golems in response to threats when this many guardians are nearby. " +
                        "Guardians are mobs in the settlements:village_guardians entity type tag. " +
                        "Set to 0 to turn off threat summoning",
                defaultValue = 3,
                min = 0,
                max = 64)
        int guardianCap,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "agreement_quorum",
                description = "How many nearby Settlements villagers must agree they need an iron golem before one can " +
                        "be summoned. Includes the summoning villager. Set to 1 to let villagers summon on their own",
                defaultValue = 2,
                min = 1,
                max = 64)
        int agreementQuorum,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "quorum_cooldown_min",
                description = "Minimum wait in seconds before a villager can help summon another iron golem " +
                        "in response to a threat",
                defaultValue = 300,
                min = 0,
                max = 86400)
        int quorumCooldownMinSeconds,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "quorum_cooldown_max",
                description = "Maximum wait in seconds before a villager can help summon another iron golem " +
                        "in response to a threat",
                defaultValue = 600,
                min = 0,
                max = 86400)
        int quorumCooldownMaxSeconds,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "witness_cooldown_min",
                description = "Minimum cooldown in seconds for nearby Settlements villagers who witnessed another " +
                        "villager summon an iron golem",
                defaultValue = 10,
                min = 0,
                max = 86400)
        int witnessCooldownMinSeconds,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "witness_cooldown_max",
                description = "Maximum cooldown in seconds for nearby Settlements villagers who witnessed another " +
                        "villager summon an iron golem",
                defaultValue = 20,
                min = 0,
                max = 86400)
        int witnessCooldownMaxSeconds

) {

    public ThreatGolemSummonConfig {
        if (quorumCooldownMinSeconds > quorumCooldownMaxSeconds) {
            throw new IllegalArgumentException("quorum_cooldown_min cannot be greater than quorum_cooldown_max");
        }
        if (witnessCooldownMinSeconds > witnessCooldownMaxSeconds) {
            throw new IllegalArgumentException("witness_cooldown_min cannot be greater than witness_cooldown_max");
        }
    }

    public ClockTicks quorumCooldownMin() {
        return ClockTicks.seconds(this.quorumCooldownMinSeconds);
    }

    public ClockTicks quorumCooldownMax() {
        return ClockTicks.seconds(this.quorumCooldownMaxSeconds);
    }

    public ClockTicks witnessCooldownMin() {
        return ClockTicks.seconds(this.witnessCooldownMinSeconds);
    }

    public ClockTicks witnessCooldownMax() {
        return ClockTicks.seconds(this.witnessCooldownMaxSeconds);
    }

}
