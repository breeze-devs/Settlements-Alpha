package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.perception.SightingDedupeKeyFactory;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Per-villager sensor that bridges entity sightings from {@link PerceivedEntities} into
 * the world-event bus, where they flow through the perception → knowledge → gossip pipeline.
 * <p>
 * This sensor does NOT write any brain memory; the bus emit is the sole side effect. This
 * is intentional: a sighting is an episodic event anchored in time and space, not a spatial
 * memory slot that needs to be overwritten on every scan. Memory promotion happens downstream
 * in {@link dev.breezes.settlements.application.ai.perception.PerceptionPipeline} once the
 * event is observed by the villager through the normal importance-gate path.
 * <p>
 * Coarse-cell deduplication via {@link SightingDedupeKeyFactory} allows independent witnesses
 * of the same entity to converge on one episodic fact, enabling gossip-based corroboration
 * without requiring exact positional agreement.
 */
public final class EntitySightingEmitterSensor extends AbstractSensor<BaseVillager> {

    /**
     * Sightings of player, wandering trader, golem, etc
     */
    private static final long SOCIAL_RESIGHT_COOLDOWN_TICKS = ClockTicks.minutes(3).getTicks();
    private static final long ZOMBIE_RESIGHT_COOLDOWN_TICKS = ClockTicks.minutes(2).getTicks();

    private static final int SCAN_INTERVAL_TICKS = ClockTicks.seconds(1).getTicksAsInt();

    private final WorldEventEmitter emitter;

    /**
     * Per-entity-UUID expiry tick to suppress re-emitting for the SAME entity
     */
    private final Map<UUID, Long> subjectCooldowns = new HashMap<>();

    public EntitySightingEmitterSensor(@Nonnull WorldEventEmitter emitter, @Nonnull BaseVillager villager) {
        super(List.of(), createStaggeredCooldown(villager));
        this.emitter = emitter;
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager villager) {
        long currentTick = world.getGameTime();
        purgeExpiredCooldowns(currentTick);

        PerceivedEntities perceived = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());

        emitForType(perceived, Zombie.class, Zombie::isAlive,
                WorldEventType.ZOMBIE_SIGHTED, villager, currentTick, ZOMBIE_RESIGHT_COOLDOWN_TICKS);
        emitForType(perceived, Player.class, Player::isAlive,
                WorldEventType.PLAYER_SIGHTED, villager, currentTick, SOCIAL_RESIGHT_COOLDOWN_TICKS);
        emitForType(perceived, WanderingTrader.class, WanderingTrader::isAlive,
                WorldEventType.WANDERING_TRADER_SIGHTED, villager, currentTick, SOCIAL_RESIGHT_COOLDOWN_TICKS);
        emitForType(perceived, IronGolem.class, IronGolem::isAlive,
                WorldEventType.GOLEM_SIGHTED, villager, currentTick, SOCIAL_RESIGHT_COOLDOWN_TICKS);

        // A sighting is an episodic bus event, not a memory slot — no brain writes needed here.
        return List.of();
    }

    private <E extends Entity> void emitForType(@Nonnull PerceivedEntities perceived,
                                                @Nonnull Class<E> type,
                                                @Nonnull Predicate<E> filter,
                                                @Nonnull WorldEventType eventType,
                                                @Nonnull BaseVillager witness,
                                                long currentTick,
                                                long cooldownTicks) {
        perceived.ofType(type, filter)
                .filter(entity -> !isCoolingDown(entity.getUUID(), currentTick))
                .forEach(entity -> {
                    int blockX = (int) Math.floor(entity.getX());
                    int blockZ = (int) Math.floor(entity.getZ());
                    String entityTypeId = entityTypeString(entity);
                    UUID dedupeKey = SightingDedupeKeyFactory.computeDedupeKey(entityTypeId, blockX, blockZ, currentTick);
                    this.emitter.emitSighting(witness, entity, entityTypeId, eventType, dedupeKey);
                    this.subjectCooldowns.put(entity.getUUID(), currentTick + cooldownTicks);
                });
    }

    private boolean isCoolingDown(UUID subjectId, long currentTick) {
        Long expiry = this.subjectCooldowns.get(subjectId);
        return expiry != null && currentTick < expiry;
    }

    private void purgeExpiredCooldowns(long currentTick) {
        this.subjectCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
    }

    /**
     * Returns the namespaced entity type string (e.g. "minecraft:zombie") that SIS uses for
     * species-aware monologue rendering.
     */
    private static String entityTypeString(@Nonnull Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType())
                .toString();
    }

    private static Tickable createStaggeredCooldown(@Nonnull BaseVillager villager) {
        int initialDelay = Math.floorMod(villager.getUUID().hashCode(), SCAN_INTERVAL_TICKS);
        return new Tickable(SCAN_INTERVAL_TICKS, initialDelay);
    }

}
