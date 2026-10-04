package dev.breezes.settlements.infrastructure.minecraft.behavior.home;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

/**
 * Forgets a home whose bed is gone, checked while the villager is near it.
 * It leaves an occupied bed alone, whoever occupies it, a player included, and never releases a ticket.
 * <p>
 * This is vanilla's ValidateNearbyPoi for HOME without its occupied-bed branch. That branch releases the bed's only
 * ticket while the sleeper still claims the bed, so the bed reads vacant to the next homeless villager; and it runs on
 * every tick near the bed, so it would act on a contested bed before {@link BedCompetitorScan} does. Bed contests
 * belong to that scan.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidateNearbyHome {

    // Mirrors vanilla ValidateNearbyPoi's MAX_DISTANCE.
    private static final double MAX_DISTANCE = 16.0D;

    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(instance -> instance.group(instance.present(MemoryModuleType.HOME))
                .apply(instance, homeMemory -> (level, entity, gameTime) -> {
                    GlobalPos home = instance.get(homeMemory);
                    if (level.dimension() != home.dimension() || !home.pos().closerToCenterThan(entity.position(), MAX_DISTANCE)) {
                        return false;
                    }

                    // The dimension check makes this level the Home's own, so vanilla's lookup of the Home's level, and
                    // its branch for a missing one, reduce to this level's POI manager
                    if (!level.getPoiManager().exists(home.pos(), poiType -> poiType.is(PoiTypes.HOME))) {
                        homeMemory.erase();
                    }

                    return true;
                }));
    }

}
