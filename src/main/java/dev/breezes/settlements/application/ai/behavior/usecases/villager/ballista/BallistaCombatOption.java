package dev.breezes.settlements.application.ai.behavior.usecases.villager.ballista;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.threat.CombatOption;
import dev.breezes.settlements.application.ai.threat.CombatReadiness;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaBlockEntity;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Combat option for villagers operating a ballista.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BallistaCombatOption implements CombatOption {

    private static final double NERVE_CONTRIBUTION = 8.0;

    // Maximum distance from the villager to navigate to a ballista
    private static final double APPROACH_RANGE_BLOCKS = 24.0;

    // Maximum target distance from the ballista when starting an engagement
    private static final double ENGAGEMENT_RANGE_BLOCKS = 48.0;

    private static final double RAID_ENGAGEMENT_WIDENING_BLOCKS = 8.0;
    private static final double RAID_INTELLIGENCE_RELIEF = 0.2;

    /**
     * Maximum target distance from the ballista for an existing engagement.
     * Extends beyond villager awareness to support long-range fire.
     * Must cover the engagement range, including its raid bonus, so newly acquired targets remain valid.
     */
    static final double LOCK_RANGE_BLOCKS = 128.0;

    // Reach is measured from the villager, but firing range is measured from the ballista
    private static final double REACH_BLOCKS = APPROACH_RANGE_BLOCKS + LOCK_RANGE_BLOCKS;

    // Delay retries long enough to try another ballista or combat option, but allow recovery from temporary obstructions
    private static final ClockTicks REFUSAL_MEMORY = ClockTicks.seconds(20);

    private static final BehaviorDisplayMetadata DISPLAY_INFO = BehaviorDisplayMetadata.builder()
            .displayNameKey("ui.settlements.behavior.combat.man_ballista")
            .iconItemId(ItemRegistry.BALLISTA.getId())
            .build();

    private final BehaviorSupport support;
    private final BallistaOperationConfig config;

    /**
     * Computed per ballista x hostiles, shared across nearby villagers.
     */
    private final BallistaShotChecks shotChecks = new BallistaShotChecks();

    @Override
    public double nerveContribution() {
        return NERVE_CONTRIBUTION;
    }

    @Override
    public double reachBlocks() {
        return REACH_BLOCKS;
    }

    /**
     * Null when the villager is not an eligible operator or knows no ballista it can use.
     */
    @Override
    @Nullable
    public Readiness prepare(@Nonnull BaseVillager villager) {
        if (!(villager.level() instanceof ServerLevel level) || !this.isEligible(level, villager)) {
            return null;
        }

        boolean holdsBolt = hasBoltInInventory(villager);
        Set<GlobalPos> refused = refusedMachines(villager);
        List<BlockPos> usableMachines = new ArrayList<>();
        for (GlobalPos site : knownMachines(villager)) {
            if (isAvailable(level, villager, site, refused, holdsBolt)) {
                usableMachines.add(site.pos());
            }
        }
        if (usableMachines.isEmpty()) {
            return null;
        }

        // Prefer nearby ballistas regardless of the order in site memory
        usableMachines.sort(Comparator.comparingDouble(pos -> villager.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
        return new Readiness(level, this.shotChecks, usableMachines, engagementRangeBlocks(isRaidActive(level, villager)));
    }

    @Override
    public IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target) {
        return new ManBallistaBehavior(this.support, this, this.config, this.shotChecks, target);
    }

    @Override
    public BehaviorDisplayMetadata displayInfo() {
        return DISPLAY_INFO;
    }

    /**
     * Records this ballista as temporarily unavailable to the villager.
     */
    void rememberRefusal(@Nonnull BaseVillager villager, @Nonnull BlockPos machinePos) {
        Set<GlobalPos> refused = new HashSet<>(refusedMachines(villager));
        refused.add(GlobalPos.of(villager.level().dimension(), machinePos));
        // All remembered ballistas share this expiry; earlier refusals stay active when a new one is added
        villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.REFUSED_BALLISTAS, Set.copyOf(refused), REFUSAL_MEMORY);
    }

    /**
     * Returns the center of the target's hitbox.
     */
    static Vec3 aimPointOf(@Nonnull LivingEntity target) {
        // Aim at the hitbox center to maximize the margin for scatter
        return target.getBoundingBox().getCenter();
    }

    /**
     * True if the villager carries a bolt or inventory requirements are bypassed.
     */
    static boolean hasBoltInInventory(@Nonnull BaseVillager villager) {
        return villager.getSettlementsInventory().containsOrBypassed(ItemRegistry.BALLISTA_BOLT.get());
    }

    /**
     * Reference point for ballista-relative offsets: the bottom center of its block.
     */
    static Vec3 floorCenterOf(@Nonnull BlockPos machinePos) {
        return new Vec3(machinePos.getX() + 0.5, machinePos.getY(), machinePos.getZ() + 0.5);
    }

    private boolean isEligible(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (VillagerProfessionKey.FLETCHER.equals(villager.getProfession())) {
            return true;
        }

        double intelligence = villager.getGenetics().getGeneValue(GeneType.INTELLIGENCE);
        double threshold = this.config.operatorIntelligenceThreshold();
        if (intelligence >= threshold) {
            return true;
        }
        // Values above the gene maximum disable intelligence-based eligibility, even during raids.
        // The raid lookup runs last, so only villagers within the relief margin pay for it.
        return threshold <= Gene.MAXIMUM_VALUE
                && intelligence >= threshold - RAID_INTELLIGENCE_RELIEF
                && isRaidActive(level, villager);
    }

    private static double engagementRangeBlocks(boolean duringRaid) {
        return duringRaid ? ENGAGEMENT_RANGE_BLOCKS + RAID_ENGAGEMENT_WIDENING_BLOCKS : ENGAGEMENT_RANGE_BLOCKS;
    }

    private static boolean isRaidActive(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        Raid raid = level.getRaidAt(villager.blockPosition());
        return raid != null && raid.isActive() && !raid.isOver();
    }

    private static List<GlobalPos> knownMachines(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.BALLISTA_SITES).orElse(List.of());
    }

    private static Set<GlobalPos> refusedMachines(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.REFUSED_BALLISTAS).orElse(Set.of());
    }

    /**
     * True if the villager can use this ballista. Does not assess a target.
     */
    private static boolean isAvailable(@Nonnull ServerLevel level,
                                       @Nonnull BaseVillager villager,
                                       @Nonnull GlobalPos site,
                                       @Nonnull Set<GlobalPos> refused,
                                       boolean holdsBolt) {
        if (!site.dimension().equals(level.dimension())
                || villager.position().distanceToSqr(floorCenterOf(site.pos())) > APPROACH_RANGE_BLOCKS * APPROACH_RANGE_BLOCKS) {
            return false;
        }

        BallistaBlockEntity machine = BallistaBlockEntity.inLoadedChunk(level, site.pos());
        if (machine == null) {
            return false;
        }
        // Accept the villager's own lease so an active operator does not disqualify itself
        if (!machine.isLeaseFreeFor(villager.getUUID())) {
            return false;
        }
        if (refused.contains(site)) {
            return false;
        }
        // A loaded bolt is usable even if the ballista still needs winding
        return holdsBolt || machine.hasSeatedAmmunition();
    }

    /**
     * The ballistas one villager can use, nearest to it first, judging each hostile against them.
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Readiness implements CombatReadiness {

        private final ServerLevel level;
        private final BallistaShotChecks shotChecks;
        private final List<BlockPos> usableMachines;
        private final double engagementRangeBlocks;

        @Override
        public boolean canEngage(@Nonnull LivingEntity hostile) {
            return this.machineFor(hostile, false) != null;
        }

        @Override
        public boolean canStayLockedOn(@Nonnull LivingEntity target) {
            return this.machineFor(target, true) != null;
        }

        /**
         * The nearest usable ballista with a shot at the hostile, or null if none has one.
         *
         * @param held true for an existing engagement, allowing the longer lock range instead of the engagement range
         */
        @Nullable
        BallistaBlockEntity machineFor(@Nonnull LivingEntity hostile, boolean held) {
            double range = held ? LOCK_RANGE_BLOCKS : this.engagementRangeBlocks;
            for (BlockPos machinePos : this.usableMachines) {
                // Reject impossible shots to avoid aiming indefinitely; reject blocked sight to avoid firing into cover
                if (hostile.position().distanceToSqr(floorCenterOf(machinePos)) <= range * range
                        && this.shotChecks.canHit(this.level, machinePos, hostile)) {
                    return BallistaBlockEntity.inLoadedChunk(this.level, machinePos);
                }
            }
            return null;
        }

    }

}
