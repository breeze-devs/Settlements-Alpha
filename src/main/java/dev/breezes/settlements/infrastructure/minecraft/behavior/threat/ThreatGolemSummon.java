package dev.breezes.settlements.infrastructure.minecraft.behavior.threat;

import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Attempts to summon an iron golem for a villager who is threatened if fewer than a cap of village guardians are near.
 * <p>
 * An instance holds one villager's summon cadence, so each brain needs its own.
 */
@CustomLog
public final class ThreatGolemSummon extends OneShot<Villager> {

    private static final ClockTicks SUMMON_INTERVAL = ClockTicks.seconds(5);

    /**
     * How recently a villager must have slept to want a golem.
     */
    private static final ClockTicks SLEPT_WITHIN = ClockTicks.minutes(20);

    /**
     * How far, in blocks, a neighbor whose agreement counts toward the quorum may stand from the summoner.
     */
    private static final double AGREEMENT_REACH = 10.0;
    private static final double AGREEMENT_REACH_SQUARED = AGREEMENT_REACH * AGREEMENT_REACH;

    /**
     * How far, in blocks, a target may stand from the summoner for the golem to be called beside it, and a hostile in
     * sight for it to be chosen as the target at all.
     */
    private static final double TARGET_REACH = 8.0;
    private static final double TARGET_REACH_SQUARED = TARGET_REACH * TARGET_REACH;

    /**
     * Placement attempts per summon, shared between the box beside the target and the box around the summoner.
     */
    private static final int SPAWN_ATTEMPTS = 10;
    private static final int SPAWN_HORIZONTAL_SPREAD = 8;
    private static final int SPAWN_VERTICAL_SPREAD = 6;

    private static final int BESIDE_TARGET_SPAWN_ATTEMPTS = 5;
    private static final int BESIDE_TARGET_HORIZONTAL_SPREAD = 2;
    private static final int BESIDE_TARGET_VERTICAL_SPREAD = 2;

    private final ThreatGolemSummonConfig config;
    private final Tickable cadence = Tickable.staggered(SUMMON_INTERVAL);

    public ThreatGolemSummon(ThreatGolemSummonConfig config) {
        this.config = config;
    }

    @Override
    public boolean trigger(ServerLevel level, Villager villager, long gameTime) {
        // The cadence runs through peacetime too, so a new threat meets its first attempt somewhere within the interval
        // rather than a full interval after it starts
        if (!cadence.tickCheckAndReset(1) || !(villager instanceof BaseVillager summoner) || !isThreatened(summoner)) {
            return false;
        }

        Brain<Villager> brain = summoner.getBrain();
        List<LivingEntity> nearby = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
        LivingEntity attacker = brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null);
        trySummon(level, summoner, nearby, attacker, gameTime);
        return true;
    }

    private static boolean isThreatened(BaseVillager villager) {
        ThreatVerdict verdict = villager.getSettlementsBrain().threats().verdict();
        return verdict == ThreatVerdict.PANIC || verdict == ThreatVerdict.COMBAT;
    }

    private void trySummon(ServerLevel level,
                           BaseVillager summoner,
                           List<LivingEntity> nearby,
                           @Nullable LivingEntity attacker,
                           long gameTime) {
        if (!wantsGolem(summoner, gameTime)) {
            return;
        }

        long guardians = nearby.stream()
                .filter(entity -> entity.isAlive() && entity.getType().is(SettlementsEntityTypeTags.VILLAGE_GUARDIANS))
                .count();
        if (guardians >= config.guardianCap()) {
            return;
        }

        // Only a Settlements villager can carry the cooldown, so a vanilla villager never joins the quorum
        List<BaseVillager> partners = nearby.stream()
                .filter(BaseVillager.class::isInstance)
                .filter(entity -> entity.isAlive() && summoner.distanceToSqr(entity) <= AGREEMENT_REACH_SQUARED)
                .map(BaseVillager.class::cast)
                .filter(neighbor -> wantsGolem(neighbor, gameTime))
                .limit(config.agreementQuorum() - 1)
                .toList();
        if (partners.size() < config.agreementQuorum() - 1) {
            return;
        }

        Optional<LivingEntity> target = chooseTargetHostile(summoner, attacker);
        Optional<LivingEntity> besideTarget = target.filter(chosen -> summoner.distanceToSqr(chosen) <= TARGET_REACH_SQUARED);

        // Total summoning attempts are shared between the near-target summon and the random-in-range summon
        int remainingAttempts = besideTarget.isPresent() ? SPAWN_ATTEMPTS - BESIDE_TARGET_SPAWN_ATTEMPTS : SPAWN_ATTEMPTS;
        Optional<IronGolem> golem = besideTarget
                .flatMap(chosen -> spawnGolem(level, chosen.blockPosition(), BESIDE_TARGET_SPAWN_ATTEMPTS,
                        BESIDE_TARGET_HORIZONTAL_SPREAD, BESIDE_TARGET_VERTICAL_SPREAD))
                .or(() -> spawnGolem(level, summoner.blockPosition(), remainingAttempts, SPAWN_HORIZONTAL_SPREAD,
                        SPAWN_VERTICAL_SPREAD));

        golem.ifPresent(summoned -> {
            // canAttackType keeps the golem's own refusals
            target.filter(chosen -> summoned.canAttack(chosen) && summoned.canAttackType(chosen.getType()))
                    .ifPresent(summoned::setTarget);

            setSummoningCooldown(summoner, config.quorumCooldownMin(), config.quorumCooldownMax());
            partners.forEach(partner -> setSummoningCooldown(partner, config.quorumCooldownMin(), config.quorumCooldownMax()));

            // Update memories eagerly instead of waiting for the sensor scan
            GolemSensor.golemDetected(summoner);
            for (LivingEntity entity : nearby) {
                if (entity instanceof Villager villager) {
                    GolemSensor.golemDetected(villager);
                }

                // Apply witness summoning cooldowns to prevent mass summoning
                if (entity instanceof BaseVillager witness) {
                    setSummoningCooldown(witness, config.witnessCooldownMin(), config.witnessCooldownMax());
                }
            }

            Location arrival = Location.of(summoned.getX(), summoned.getY(0.5), summoned.getZ(), level);
            ParticleRegistry.summonIronGolem(arrival);
            SoundRegistry.SUMMON_IRON_GOLEM.playGlobally(arrival, SoundSource.NEUTRAL);
            log.debug("Villager {} summoned iron golem {} at {}", summoner.getUUID(), summoned.getUUID(),
                    summoned.blockPosition().toShortString());
        });
    }

    private static Optional<LivingEntity> chooseTargetHostile(BaseVillager summoner, @Nullable LivingEntity attacker) {
        if (attacker != null && isEligibleTarget(summoner, attacker)) {
            return Optional.of(attacker);
        }

        // Only a hostile in sight, so a golem is never called beside one in a cave below the village or behind a wall
        return summoner.getSettlementsBrain().getMemory(MemoryTypeRegistry.SIGHTED_HOSTILES)
                .flatMap(sighted -> sighted.closest(LivingEntity.class,
                        hostile -> summoner.distanceToSqr(hostile) <= TARGET_REACH_SQUARED
                                && isEligibleTarget(summoner, hostile),
                        summoner));
    }

    private static boolean isEligibleTarget(BaseVillager summoner, LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.level() != summoner.level()) {
            return false;
        }

        if (candidate instanceof Player || candidate.getType().is(SettlementsEntityTypeTags.VILLAGER_ALLIES)) {
            return false;
        }

        // A golem refuses creepers, which only a golem instance reports, so the refusal is mirrored here to keep one
        // from being called beside a creeper it will not fight
        return candidate.getType() != EntityType.CREEPER;
    }

    private static Optional<IronGolem> spawnGolem(ServerLevel level, BlockPos center, int attempts, int horizontalSpread,
                                                  int verticalSpread) {
        // The deprecated strategy is the one vanilla's own villager summon places with; the other would change which
        // ground a golem can appear on
        return SpawnUtil.trySpawnMob(EntityType.IRON_GOLEM, MobSpawnType.MOB_SUMMONED, level, center, attempts,
                horizontalSpread, verticalSpread, SpawnUtil.Strategy.LEGACY_IRON_GOLEM);
    }

    private static boolean wantsGolem(BaseVillager villager, long gameTime) {
        // The cooldown stands in for vanilla's no-golem-seen check, which would stop the summon at a single golem
        if (villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.GOLEM_SUMMON_COOLDOWN).isPresent()) {
            return false;
        }
        return villager.getBrain().getMemory(MemoryModuleType.LAST_SLEPT)
                .filter(lastSlept -> gameTime - lastSlept < SLEPT_WITHIN.getTicks())
                .isPresent();
    }

    private static void setSummoningCooldown(BaseVillager villager, ClockTicks min, ClockTicks max) {
        // Each villager draws its own length, so villagers that cooled down together do not come back together
        long drawn = RandomUtil.randomInt(min.getTicksAsInt(), max.getTicksAsInt(), true);

        // A witness's short cooldown must never cut a quorum member's long one short, so a write only extends
        MemoryType.VanillaMemoryType<Boolean> cooldown = MemoryTypeRegistry.GOLEM_SUMMON_COOLDOWN;
        if (drawn > villager.getBrain().getTimeUntilExpiry(cooldown.getModuleType())) {
            villager.getSettlementsBrain().setMemory(cooldown, true, ClockTicks.of(drawn));
        }
    }

}
