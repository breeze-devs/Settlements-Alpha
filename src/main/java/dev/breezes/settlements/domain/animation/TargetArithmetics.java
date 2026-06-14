package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TargetArithmetics {

    public static final TargetArithmetic<Float> FLOAT = new TargetArithmetic<>() {
        @Override
        public Float add(@Nonnull Float left, @Nonnull Float right) {
            return left + right;
        }

        @Override
        public Float subtract(@Nonnull Float left, @Nonnull Float right) {
            return left - right;
        }

        @Override
        public Float scale(@Nonnull Float value, float scalar) {
            return value * scalar;
        }

        @Override
        public Float multiply(@Nonnull Float left, @Nonnull Float right) {
            return left * right;
        }
    };

    public static final TargetArithmetic<Vec3> VEC3 = new TargetArithmetic<>() {
        @Override
        public Vec3 add(@Nonnull Vec3 left, @Nonnull Vec3 right) {
            return left.add(right);
        }

        @Override
        public Vec3 subtract(@Nonnull Vec3 left, @Nonnull Vec3 right) {
            return left.subtract(right);
        }

        @Override
        public Vec3 scale(@Nonnull Vec3 value, float scalar) {
            return value.scale(scalar);
        }

        @Override
        public Vec3 multiply(@Nonnull Vec3 left, @Nonnull Vec3 right) {
            return new Vec3(left.x * right.x, left.y * right.y, left.z * right.z);
        }
    };

    public static final TargetArithmetic<Vector3f> VECTOR3F = new TargetArithmetic<>() {
        @Override
        public Vector3f add(@Nonnull Vector3f left, @Nonnull Vector3f right) {
            return new Vector3f(left).add(right);
        }

        @Override
        public Vector3f subtract(@Nonnull Vector3f left, @Nonnull Vector3f right) {
            return new Vector3f(left).sub(right);
        }

        @Override
        public Vector3f scale(@Nonnull Vector3f value, float scalar) {
            return new Vector3f(value).mul(scalar);
        }

        @Override
        public Vector3f multiply(@Nonnull Vector3f left, @Nonnull Vector3f right) {
            return new Vector3f(left).mul(right);
        }
    };

    @SuppressWarnings("unchecked")
    public static <V> TargetArithmetic<V> forValueType(@Nonnull Class<V> valueType) {
        if (valueType == Float.class) {
            return (TargetArithmetic<V>) FLOAT;
        }
        if (valueType == Vec3.class) {
            return (TargetArithmetic<V>) VEC3;
        }
        if (valueType == Vector3f.class) {
            return (TargetArithmetic<V>) VECTOR3F;
        }
        return unsupported(valueType);
    }

    private static <V> TargetArithmetic<V> unsupported(@Nonnull Class<V> valueType) {
        return new TargetArithmetic<>() {
            @Override
            public V add(@Nonnull V left, @Nonnull V right) {
                throw unsupportedOperation(valueType);
            }

            @Override
            public V subtract(@Nonnull V left, @Nonnull V right) {
                throw unsupportedOperation(valueType);
            }

            @Override
            public V scale(@Nonnull V value, float scalar) {
                throw unsupportedOperation(valueType);
            }

            @Override
            public V multiply(@Nonnull V left, @Nonnull V right) {
                throw unsupportedOperation(valueType);
            }
        };
    }

    private static UnsupportedOperationException unsupportedOperation(@Nonnull Class<?> valueType) {
        return new UnsupportedOperationException("No target arithmetic registered for " + valueType.getName());
    }

}
