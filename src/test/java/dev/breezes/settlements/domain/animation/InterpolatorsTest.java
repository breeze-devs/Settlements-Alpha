package dev.breezes.settlements.domain.animation;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpolatorsTest {

    @Test
    void floatInterpolator_lerpsAndClampsProgress() {
        // Arrange, Act, Assert
        assertEquals(5.0F, Interpolators.FLOAT.interpolate(0.0F, 10.0F, 0.5F), 0.0001F);
        assertEquals(0.0F, Interpolators.FLOAT.interpolate(0.0F, 10.0F, -1.0F), 0.0001F);
        assertEquals(10.0F, Interpolators.FLOAT.interpolate(0.0F, 10.0F, 2.0F), 0.0001F);
    }

    @Test
    void vec3Interpolator_lerpsEachAxis() {
        // Arrange
        Vec3 from = new Vec3(0.0D, 10.0D, -10.0D);
        Vec3 to = new Vec3(10.0D, 20.0D, 10.0D);

        // Act
        Vec3 result = Interpolators.VEC3.interpolate(from, to, 0.25F);

        // Assert
        assertEquals(2.5D, result.x, 0.0001D);
        assertEquals(12.5D, result.y, 0.0001D);
        assertEquals(-5.0D, result.z, 0.0001D);
    }

    @Test
    void vector3fInterpolator_lerpsEachAxis() {
        // Arrange
        Vector3f from = new Vector3f(0.0F, 10.0F, -10.0F);
        Vector3f to = new Vector3f(10.0F, 20.0F, 10.0F);

        // Act
        Vector3f result = Interpolators.VECTOR3F.interpolate(from, to, 0.5F);

        // Assert
        assertEquals(5.0F, result.x(), 0.0001F);
        assertEquals(15.0F, result.y(), 0.0001F);
        assertEquals(0.0F, result.z(), 0.0001F);
    }

    @Test
    void booleanStepInterpolator_holdsUntilEnd() {
        // Arrange, Act, Assert
        assertFalse(Interpolators.BOOLEAN_STEP.interpolate(false, true, 0.5F));
        assertTrue(Interpolators.BOOLEAN_STEP.interpolate(false, true, 1.0F));
    }
}
