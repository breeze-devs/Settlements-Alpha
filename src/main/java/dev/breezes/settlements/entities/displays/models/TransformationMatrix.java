package dev.breezes.settlements.entities.displays.models;

import com.mojang.math.Transformation;
import lombok.Getter;
import org.joml.Matrix4f;

@Getter
public class TransformationMatrix {

    private final Matrix4f matrix;

    public TransformationMatrix(float f01, float f02, float f03, float f04,
                                float f11, float f12, float f13, float f14,
                                float f21, float f22, float f23, float f24,
                                float f31, float f32, float f33, float f34,
                                boolean transpose) {
        this.matrix = new Matrix4f(
                f01, f02, f03, f04,
                f11, f12, f13, f14,
                f21, f22, f23, f24,
                f31, f32, f33, f34
        );

        if (transpose) {
            this.matrix.transpose();
        }
    }

    public TransformationMatrix(float f01, float f02, float f03, float f04,
                                float f11, float f12, float f13, float f14,
                                float f21, float f22, float f23, float f24,
                                float f31, float f32, float f33, float f34) {
        this(f01, f02, f03, f04,
                f11, f12, f13, f14,
                f21, f22, f23, f24,
                f31, f32, f33, f34,
                true);
    }

    public Transformation getMinecraftTransformation() {
        return new Transformation(this.matrix);
    }

}
