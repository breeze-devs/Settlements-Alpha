package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Interpolators {

    public static final Interpolator<Float> FLOAT = (from, to, t) -> from + ((to - from) * Math.clamp(t, 0.0F, 1.0F));

    public static final Interpolator<Vec3> VEC3 = (from, to, t) -> {
        float clamped = Math.clamp(t, 0.0F, 1.0F);
        return new Vec3(
                from.x + ((to.x - from.x) * clamped),
                from.y + ((to.y - from.y) * clamped),
                from.z + ((to.z - from.z) * clamped));
    };

    public static final Interpolator<Vector3f> VECTOR3F = (from, to, t) -> {
        float clamped = Math.clamp(t, 0.0F, 1.0F);
        return new Vector3f(
                from.x() + ((to.x() - from.x()) * clamped),
                from.y() + ((to.y() - from.y()) * clamped),
                from.z() + ((to.z() - from.z()) * clamped));
    };

    public static final Interpolator<Boolean> BOOLEAN_STEP = (from, to, t) -> Math.clamp(t, 0.0F, 1.0F) >= 1.0F ? to : from;

    public static final Interpolator<ItemDisplayContext> ITEM_DISPLAY_CONTEXT_STEP = (from, to, t) -> Math.clamp(t, 0.0F, 1.0F) >= 1.0F ? to : from;

}
