package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spawns Settlements wolves (and very rarely farm animals) near active villages, mirroring the
 * approach vanilla CatSpawner uses — a tick countdown that fires periodically and looks for a
 * suitable position near a random player. Cadence, caps, and the farm-animal roster are all driven
 * by {@link VillageAnimalSpawnerConfig}.
 * <p>
 * Running on level-tick rather than as a registered CustomSpawner avoids coupling to vanilla's
 * per-chunk spawning budget and lets us apply Settlements-specific caps independently.
 */
@ServerScope
@CustomLog
public final class VillageAnimalSpawnerServerEvents {

    private static final double VILLAGE_SCAN_RADIUS = 48.0;
    private static final double VILLAGE_SCAN_HEIGHT = 8.0;

    private final VillageAnimalSpawnerConfig config;

    private final ITickable spawnTickable;
    private final List<Map.Entry<EntityType<?>, Integer>> farmAnimalSpawnEntries;

    @Inject
    VillageAnimalSpawnerServerEvents(VillageAnimalSpawnerConfig config) {
        this.config = config;

        this.spawnTickable = ClockTicks.seconds(config.spawnIntervalSeconds()).asTickable();
        this.farmAnimalSpawnEntries = this.resolveFarmAnimalSpawnEntries(config.farmAnimalCaps());
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !serverLevel.dimension().equals(ServerLevel.OVERWORLD)
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }

        if (!this.spawnTickable.tickCheckAndReset(1)) {
            return;
        }

        // Pick a random player, if that player is close to the village, then try to attempt spawn
        // Mirrors vanilla, but this decreases chances of supplementary spawns when there are many players
        Player player = serverLevel.getRandomPlayer();
        if (player == null) {
            return;
        }

        int targetX = player.blockPosition().getX() + RandomUtil.randomInt(-31, 31, true);
        int targetZ = player.blockPosition().getZ() + RandomUtil.randomInt(-31, 31, true);
        int surfaceY = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);

        BlockPos candidate = new BlockPos(targetX, surfaceY, targetZ);

        // Check if chunk is loaded
        if (!serverLevel.hasChunksAt(candidate.getX() - 10, candidate.getZ() - 10, candidate.getX() + 10, candidate.getZ() + 10)) {
            return;
        }

        if (!serverLevel.isCloseToVillage(candidate, 2)) {
            return;
        }

        // Only spawn if the area looks like a real village (enough occupied homes)
        long homeCount = serverLevel.getPoiManager()
                .getCountInRange(p -> p.is(PoiTypes.HOME), candidate, (int) VILLAGE_SCAN_RADIUS, PoiManager.Occupancy.IS_OCCUPIED);
        if (homeCount < this.config.minimumOccupiedHomes()) {
            return;
        }

        this.trySpawnWolf(serverLevel, candidate);
        this.trySpawnFarmAnimal(serverLevel, candidate);
    }

    private void trySpawnWolf(ServerLevel level, BlockPos pos) {
        // Respect the per-village wolf cap
        int nearbyWolfCount = level.getEntitiesOfClass(SettlementsWolf.class,
                new AABB(pos).inflate(VILLAGE_SCAN_RADIUS, VILLAGE_SCAN_HEIGHT, VILLAGE_SCAN_RADIUS)).size();
        if (nearbyWolfCount >= this.config.wolfVillageCap()) {
            return;
        }

        // Check spawn condition
        if (!SpawnPlacements.isSpawnPositionOk(EntityRegistry.SETTLEMENTS_WOLF.get(), level, pos)) {
            return;
        }

        Location spawnLocation = Location.of(pos, level);
        SettlementsWolf wolf = SettlementsWolf.spawn(spawnLocation);
        log.debug("Spawned SettlementsWolf {} near village at {}", wolf.getUUID(), pos);
    }

    private void trySpawnFarmAnimal(ServerLevel level, BlockPos pos) {
        // Rare supplemental animal spawn
        if (!RandomUtil.chance(this.config.farmAnimalSpawnChance())) {
            return;
        }

        // Pick a random configured species; an empty or all-invalid roster means no farm spawns.
        Optional<Map.Entry<EntityType<?>, Integer>> chosen = RandomUtil.choice(this.farmAnimalSpawnEntries);
        if (chosen.isEmpty()) {
            return;
        }

        EntityType<?> animalType = chosen.get().getKey();
        int speciesCap = chosen.get().getValue();

        // Respect the configured per-species cap
        long nearbyCount = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(VILLAGE_SCAN_RADIUS, VILLAGE_SCAN_HEIGHT, VILLAGE_SCAN_RADIUS),
                animal -> animal.getType() == animalType).size();
        if (nearbyCount >= speciesCap) {
            return;
        }

        // Check spawn condition
        if (!SpawnPlacements.isSpawnPositionOk(animalType, level, pos)) {
            return;
        }

        // Check animal
        if (!(animalType.create(level) instanceof Animal animal)) {
            return;
        }

        animal.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        animal.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntityWithPassengers(animal);
        log.debug("Spawned farm animal {} near village at {}", animalType.toShortString(), pos);
    }

    private List<Map.Entry<EntityType<?>, Integer>> resolveFarmAnimalSpawnEntries(Map<String, Integer> farmAnimalCaps) {
        List<Map.Entry<EntityType<?>, Integer>> eligible = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : farmAnimalCaps.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                log.error("Ignoring unknown farm animal entity id in village spawner config: {}", entry.getKey());
                continue;
            }
            eligible.add(Map.entry(BuiltInRegistries.ENTITY_TYPE.get(id), entry.getValue()));
        }

        return eligible;
    }

}
