package dev.breezes.settlements.util;

import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.*;

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

    public static <T> T choice(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static <T> T choice(T[] list) {
        return list[RANDOM.nextInt(list.length)];
    }

    public static <T> T weightedChoice(@Nonnull Map<T, Double> weightMap) {
        double totalWeight = weightMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        double targetWeight = randomDouble(0, totalWeight);
        double currentWeight = 0;
        for (Map.Entry<T, Double> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (currentWeight > targetWeight) {
                return entry.getKey();
            }
        }

        throw new ArithmeticException("Invalid weights! Check if any weight(s) are zero or negative");
    }

    /**
     * Shuffles the given list in-place and returns the list itself
     */
    public static <T> ArrayList<T> shuffle(ArrayList<T> list) {
        Collections.shuffle(list);
        return list;
    }

}
