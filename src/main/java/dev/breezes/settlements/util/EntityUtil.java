package dev.breezes.settlements.util;

import dev.breezes.settlements.entities.ISettlementsVillager;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import java.util.Optional;

public class EntityUtil {

    public static Optional<ISettlementsVillager> convertToVillager(@Nonnull Entity entity) {
        return entity instanceof ISettlementsVillager
                ? Optional.of((ISettlementsVillager) entity)
                : Optional.empty();
    }

}
