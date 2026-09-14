package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BallistaAimHudProviderTest {

    private static final float[] YAWS = {-180.0F, -135.0F, -90.0F, -37.0F, 0.0F, 12.0F, 90.0F, 135.0F, 179.0F};

    @Test
    void compassBearing_readsTheMuzzleAsACompassWould() {
        for (float yaw : YAWS) {
            // Arrange: a compass measures clockwise from north (-z), toward east (+x)
            Vec3 muzzle = BallistaLaunchPoint.direction(BallistaAim.facing(yaw, 0.0F));
            long expected = Math.floorMod(Math.round(Math.toDegrees(Math.atan2(muzzle.x, -muzzle.z))), 360L);

            // Act
            int bearing = BallistaAimHudProvider.compassBearing(yaw);

            // Assert: Minecraft's own yaw starts from south, so shown raw it would read north as 180
            assertEquals(expected, bearing, "yaw " + yaw);
        }
    }

    @Test
    void compassBearing_readsJustShortOfNorthAsNorth_notAsAFullTurn() {
        // Act
        int bearing = BallistaAimHudProvider.compassBearing(179.8F);

        // Assert
        assertEquals(0, bearing);
    }

}
