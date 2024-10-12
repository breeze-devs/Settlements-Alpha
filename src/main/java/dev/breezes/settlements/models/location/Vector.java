package dev.breezes.settlements.models.location;

import dev.breezes.settlements.util.MathUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class Vector {

    private double x;
    private double y;
    private double z;

    public Vector normalize(boolean clone) {
        double length = this.magnitude();
        double newX = this.x / length;
        double newY = this.y / length;
        double newZ = this.z / length;

        if (clone) {
            return new Vector(newX, newY, newZ);
        }

        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public double magnitude() {
        return Math.sqrt(MathUtil.square(this.x) + MathUtil.square(this.y) + MathUtil.square(this.z));
    }

}
