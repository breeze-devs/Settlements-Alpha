package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.bootstrap.registry.datamaps.SettlementsDataMaps;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.perception.SensedEntity;
import dev.breezes.settlements.domain.ai.threat.QualifyingHit;
import dev.breezes.settlements.domain.ai.threat.ThreatDecision;
import dev.breezes.settlements.domain.ai.threat.ThreatResponseRules;
import dev.breezes.settlements.domain.ai.threat.ThreatSituation;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Runs one villager's threat assessment against the live world and records the outcome.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ThreatAssessor {

    private static final float DEFAULT_THREAT_WEIGHT = 1.0f;

    /**
     * The share of its threat weight a hostile out of sight carries, in both danger and nerve. A villager cannot fear
     * what it cannot see; the remainder stands for what it hears.
     */
    private static final double UNSEEN_THREAT_FRACTION = 0.2;

    private static final ThreatAssessmentResult PANIC_RESULT = new ThreatAssessmentResult(ThreatVerdict.PANIC, null);
    private static final ThreatAssessmentResult HOLD_RESULT = new ThreatAssessmentResult(ThreatVerdict.HOLD, null);

    private final CombatOptionCatalog catalog;

    /**
     * Runs the villager's threat assessment when due, and records its outcome in the given state.
     */
    public void tick(@Nonnull BaseVillager villager, @Nonnull ThreatAssessmentState state) {
        PerceivedEntities nearbyHostiles = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_HOSTILES)
                .orElse(PerceivedEntities.empty());
        if (!state.isDue(nearbyHostiles)) {
            return;
        }

        long gameTime = villager.level().getGameTime();
        QualifyingHit latestHit = state.getLatestHit();
        boolean wasHitRecently = latestHit != null && latestHit.isLive(gameTime);
        ThreatAssessmentResult previousAssessment = state.getLatestResult();
        ThreatVerdict previousVerdict = previousAssessment == null ? null : previousAssessment.verdict();

        PerceivedEntities unseenHostiles = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.UNSEEN_HOSTILES)
                .orElse(PerceivedEntities.empty());
        double danger = 0.0;
        double totalHostileThreat = 0.0;
        boolean awareHostilePresent = false;
        for (SensedEntity sensed : nearbyHostiles.entities()) {
            LivingEntity hostile = sensed.entity();
            // A hostile can die or be removed between the sensor's scan and this assessment, so liveness is
            // re-checked rather than trusted from the memory
            if (!hostile.isAlive()) {
                continue;
            }

            double threatWeight = Optional.ofNullable(hostile.getType().builtInRegistryHolder().getData(SettlementsDataMaps.THREAT_WEIGHTS))
                    .orElse(DEFAULT_THREAT_WEIGHT);
            // Only a hostile known to be out of sight is discounted; one whose sight went unchecked counts in full,
            // so a large visible group is never mistaken for a hidden one
            if (contains(unseenHostiles, hostile)) {
                threatWeight *= UNSEEN_THREAT_FRACTION;
            }
            danger += ThreatResponseRules.dangerContribution(sensed.distance(), threatWeight);
            totalHostileThreat += threatWeight;
            awareHostilePresent = true;
        }

        if (!awareHostilePresent && !wasHitRecently && (previousVerdict == null || previousVerdict == ThreatVerdict.HOLD)) {
            // Peaceful path: with nothing aware, no live hit and no response under way, the rules can only hold, so
            // the scoring is skipped
            state.recordAssessment(ThreatDecision.HOLD, HOLD_RESULT, nearbyHostiles, gameTime);
            return;
        }

        PerceivedEntities sightedHostiles = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.SIGHTED_HOSTILES)
                .orElse(PerceivedEntities.empty());
        CombatSelection previousSelection = previousAssessment == null ? null : previousAssessment.selection();
        CombatOption continuingOption = previousSelection != null && previousSelection.option().canContinue(villager)
                ? previousSelection.option()
                : null;
        // Searched before deciding, because how long the fight has gone without a target decides whether it continues
        LivingEntity continuationTarget = continuingOption == null ? null
                : findNearestEngageableHostile(villager, continuingOption, sightedHostiles);
        ClockTicks sinceEngageableTarget = continuationTarget != null ? ClockTicks.ZERO
                : ClockTicks.of(gameTime - state.getLastEngagedGameTime());

        // A brain with no combat runner has nothing to launch a selected option with
        CombatSelection entryCandidate = villager.isBaby() ? null : findFirstCombatCandidate(villager, sightedHostiles);

        double willGeneValue = villager.getGenetics().getGeneValue(GeneType.WILL);
        double healthFraction = villager.getHealth() / villager.getMaxHealth();
        Double continuingNerve = continuingOption == null ? null
                : ThreatResponseRules.nerve(willGeneValue, healthFraction, continuingOption.nerveContribution(), totalHostileThreat);
        Double enteringNerve = entryCandidate == null ? null
                : ThreatResponseRules.nerve(willGeneValue, healthFraction, entryCandidate.option().nerveContribution(), totalHostileThreat);
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(previousVerdict)
                .danger(danger)
                .awareHostilePresent(awareHostilePresent)
                .liveHitPresent(wasHitRecently)
                .continuingNerve(continuingNerve)
                .sinceEngageableTarget(sinceEngageableTarget)
                .enteringNerve(enteringNerve)
                .sinceAlarmed(ClockTicks.of(gameTime - state.getLastAlarmedGameTime()))
                .build();

        ThreatDecision decision = ThreatResponseRules.decide(situation);
        ThreatAssessmentResult result = switch (decision) {
            // The target is this assessment's own engagement scan, never carried over from the previous
            // selection: a fighter that kills its target while a second hostile waits behind a wall fights on
            // with no target, until the targetless limit, rather than chasing a dead entity's id
            case CONTINUE_COMBAT -> new ThreatAssessmentResult(ThreatVerdict.COMBAT,
                    new CombatSelection(continuingOption, continuationTarget == null ? null : continuationTarget.getUUID()));
            case ENTER_COMBAT -> new ThreatAssessmentResult(ThreatVerdict.COMBAT, entryCandidate);
            case PANIC, LINGER_IN_PANIC -> PANIC_RESULT;
            case HOLD -> HOLD_RESULT;
        };

        state.recordAssessment(decision, result, nearbyHostiles, gameTime);
    }

    /**
     * The first option in precedence order that can engage a sighted hostile; or null when none can.
     */
    @Nullable
    private CombatSelection findFirstCombatCandidate(@Nonnull BaseVillager villager, @Nonnull PerceivedEntities sightedHostiles) {
        for (CombatOption option : this.catalog.orderedOptions()) {
            LivingEntity target = findNearestEngageableHostile(villager, option, sightedHostiles);
            if (target != null) {
                return new CombatSelection(option, target.getUUID());
            }
        }
        return null;
    }

    /**
     * The nearest sighted, live hostile this option could engage within its reach, or null.
     * A zombie villager being cured is never a candidate.
     * <p>
     * Sighted hostiles are nearest-first, so once one falls outside reach every later one does too.
     */
    @Nullable
    private static LivingEntity findNearestEngageableHostile(@Nonnull BaseVillager villager,
                                                             @Nonnull CombatOption option,
                                                             @Nonnull PerceivedEntities sightedHostiles) {
        for (SensedEntity sensed : sightedHostiles.entities()) {
            LivingEntity candidate = sensed.entity();
            if (!candidate.isAlive()) {
                continue;
            }
            if (sensed.distance() > option.reachBlocks()) {
                break;
            }
            // The patient is a villager in the making, so no option may fight it, whatever its weapon
            if (candidate instanceof ZombieVillager patient && patient.isConverting()) {
                continue;
            }
            if (option.canEngage(villager, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean contains(@Nonnull PerceivedEntities perceived, @Nonnull LivingEntity entity) {
        for (SensedEntity sensed : perceived.entities()) {
            if (sensed.entity() == entity) {
                return true;
            }
        }
        return false;
    }

}
