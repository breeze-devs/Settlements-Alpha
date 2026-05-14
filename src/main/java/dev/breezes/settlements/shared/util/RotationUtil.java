package dev.breezes.settlements.shared.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.joml.Vector3f;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RotationUtil {

    public static Vector3f degrees(float pitch, float yaw, float roll) {
        return new Vector3f(
                (float) Math.toRadians(pitch),
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(roll));
    }

}
