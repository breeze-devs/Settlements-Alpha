package dev.breezes.settlements.util;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class DistanceUtils {

    public static boolean isWithinDistance(@Nonnull Vec3 position1, @Nonnull Vec3 position2, double distance) {
        return position1.distanceToSqr(position2) <= distance * distance;
    }

}
