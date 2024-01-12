package dev.breezes.settlements.util;

import net.minecraft.util.Mth;

import java.util.Random;

public class RandomUtil {

    public static final Random RANDOM = new Random();

    /**
     * Returns a random int within the range [min, max]
     * - if inclusive is true, max is inclusive, otherwise max is exclusive
     */
    public static int randomInt(int min, int max, boolean inclusive) {
        return RANDOM.nextInt(min, max + (inclusive ? 1 : 0));
    }

    /**
     * Returns a random double between min (inclusive) and max (exclusive)
     * - although max is exclusive, it is possible to get values very close to max
     */
    public static double randomDouble(double min, double max) {
        return RANDOM.nextDouble(min, max);
    }

    public static double clamp(double value, double min, double max) {
        return Mth.clamp(value, min, max);
    }


}
