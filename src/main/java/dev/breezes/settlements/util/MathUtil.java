package dev.breezes.settlements.util;

public class MathUtil {

    public static final float PI_DIV_180 = (float) (Math.PI / 180);


    public static float toRadians(float degrees) {
        return degrees * PI_DIV_180;
    }

    public static float toDegrees(float radians) {
        return radians / PI_DIV_180;
    }

    public static int square(int value) {
        return value * value;
    }

    public static float square(float value) {
        return value * value;
    }

    public static double square(double value) {
        return value * value;
    }

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("Decimal places must be non-negative");
        }
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

}
