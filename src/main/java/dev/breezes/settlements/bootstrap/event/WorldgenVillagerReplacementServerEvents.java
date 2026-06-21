package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.attachments.PendingSettlementsReplacementAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion.VillagerConversionUtil;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces a configurable share of vanilla villagers created by village world generation
 * <p>
 * The replacement decision is made during structure finalization, where the spawn source is known.
 * The actual conversion is deferred until the villager joins a live ServerLevel because village
 * generation can run inside a WorldGenRegion and the structure placement path still adds the
 * original entity after finalization.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class WorldgenVillagerReplacementServerEvents {

    /**
     * Upper bound on conversions performed per level tick. A bulk chunk load can join many marked
     * villagers on the same tick; capping the work spreads it over several ticks so no single tick spikes.
     * The persistent marker re-queues the remainder on a later tick or load, so a backlog is never lost.
     */
    private static final int MAX_CONVERSIONS_PER_TICK = 5;

    private final WorldgenVillagerReplacementConfig config;

    /**
     * Transient work queue of villagers awaiting conversion, keyed by dimension so each level drains
     * only its own backlog. This is scheduling state only — the authoritative "convert me" signal is
     * the persistent {@link PendingSettlementsReplacementAttachment} marker, so losing this map
     * merely defers conversion to the next load; it never drops a decision.
     * <p>
     * The map is confined to the server thread: it is touched only from {@link #onEntityJoinLevel}
     * (after the {@code ServerLevel} guard, which rejects off-thread worldgen-region joins) and
     * {@link #onLevelTickPost}. The worldgen-thread {@link #onFinalizeSpawn} path never touches it —
     * it mutates only the entity attachment — so a non-concurrent {@link HashMap} is safe here.
     */
    private final Map<ResourceKey<Level>, Set<UUID>> pendingReplacementUuidsByDimension = new HashMap<>();

    /**
     * Decision hook. Fires during structure finalization — the one spawn path where the source
     * ({@link MobSpawnType#STRUCTURE}) cleanly separates worldgen villagers from breeding, curing,
     * spawn eggs, and {@code /summon}. On a successful roll we only stamp the persistent marker; no
     * world mutation happens here because finalization can run inside a {@code WorldGenRegion}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalizeSpawn(@Nonnull FinalizeSpawnEvent event) {
        if (event.isCanceled() || !isEligibleWorldgenVillager(event)) {
            return;
        }

        if (!RandomUtil.chance(this.config.replacementChance())) {
            return;
        }

        PendingSettlementsReplacementAttachment.mark(event.getEntity());
        log.debug("Marked villager {} for Settlements replacement at {}", event.getEntity().getUUID(), event.getEntity().blockPosition());
    }

    /**
     * Handoff hook. A marked villager can appear long after the decision — the same tick, a later
     * chunk load, or a future server session. Once it joins a real {@link ServerLevel} we queue it
     * for conversion rather than converting inline, deferring past the join so the entity is fully
     * registered before it is discarded.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityJoinLevel(@Nonnull EntityJoinLevelEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !isVanillaVillager(event.getEntity())) {
            return;
        }

        Villager villager = (Villager) event.getEntity();
        if (!PendingSettlementsReplacementAttachment.isPending(villager)) {
            return;
        }

        enqueuePendingReplacement(serverLevel, villager);
    }

    /**
     * Execution hook. Drains queued conversions for this level one tick after they were enqueued,
     * guaranteeing each villager is fully joined before {@link VillagerConversionUtil} discards it.
     */
    @SubscribeEvent
    public void onLevelTickPost(@Nonnull LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        drainPendingReplacements(serverLevel);
    }

    private boolean isEligibleWorldgenVillager(@Nonnull FinalizeSpawnEvent event) {
        return event.getSpawnType() == MobSpawnType.STRUCTURE
                && isVanillaVillager(event.getEntity());
    }

    private static boolean isVanillaVillager(@Nonnull Entity entity) {
        return entity.getClass() == Villager.class;
    }

    private void enqueuePendingReplacement(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        this.pendingReplacementUuidsByDimension
                .computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
                .add(villager.getUUID());
    }

    /**
     * Converts up to {@link #MAX_CONVERSIONS_PER_TICK} queued villagers for the given level.
     * <p>
     * The batch is pulled out of the backing set <em>before</em> any conversion runs. Conversion adds
     * a fresh {@link BaseVillager}, which re-enters {@link #onEntityJoinLevel}; draining into a local
     * batch first means we never mutate the set while reading it, so reentrancy can't corrupt the
     * iterator. UUIDs that no longer resolve are dropped — their persistent marker re-queues them on
     * the next load.
     */
    private void drainPendingReplacements(@Nonnull ServerLevel level) {
        Set<UUID> pendingReplacementUuids = this.pendingReplacementUuidsByDimension.get(level.dimension());
        if (pendingReplacementUuids == null || pendingReplacementUuids.isEmpty()) {
            return;
        }

        List<UUID> batch = new ArrayList<>(Math.min(MAX_CONVERSIONS_PER_TICK, pendingReplacementUuids.size()));
        Iterator<UUID> iterator = pendingReplacementUuids.iterator();
        while (iterator.hasNext() && batch.size() < MAX_CONVERSIONS_PER_TICK) {
            batch.add(iterator.next());
            iterator.remove();
        }

        if (pendingReplacementUuids.isEmpty()) {
            this.pendingReplacementUuidsByDimension.remove(level.dimension());
        }

        for (UUID villagerUuid : batch) {
            convertIfEligible(level, villagerUuid);
        }
    }

    /**
     * Resolves and converts a single queued villager as a best-effort attempt
     * <p>
     * The marker is cleared up front, so a failed attempt — a null result or an exception from
     * NBT another mod left in an unexpected shape — leaves the villager vanilla.
     */
    private void convertIfEligible(@Nonnull ServerLevel level, @Nonnull UUID villagerUuid) {
        Entity entity = level.getEntity(villagerUuid);
        if (entity == null || !isVanillaVillager(entity)) {
            return;
        }

        Villager villager = (Villager) entity;
        if (villager.isRemoved() || !PendingSettlementsReplacementAttachment.isPending(villager)) {
            return;
        }

        // Whether conversion succeeds or fails, this villager is done with the replacement pipeline
        PendingSettlementsReplacementAttachment.clear(villager);

        try {
            BaseVillager replacement = VillagerConversionUtil.convertToSettlements(level, villager);
            if (replacement == null) {
                log.warn("Skipped Settlements replacement for villager {}: conversion produced no entity; left vanilla", villagerUuid);
                return;
            }

            log.debug("Converted villager {} to Settlements villager {}", villagerUuid, replacement.getUUID());
        } catch (Exception e) {
            log.error("Conversion to Settlements villager failed for {}: keeping it vanilla", villagerUuid, e);
        }
    }

}
