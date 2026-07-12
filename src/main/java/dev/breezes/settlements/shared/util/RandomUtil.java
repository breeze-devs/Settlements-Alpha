package dev.breezes.settlements.shared.util;

import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.ToDoubleFunction;

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

    public static <T> Optional<T> choice(List<T> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(RANDOM.nextInt(list.size())));
    }

    public static <T> T choice(T[] list) {
        return list[RANDOM.nextInt(list.length)];
    }

    public static <T> T weightedChoice(@Nonnull Map<T, Double> weightMap) {
        return weightedChoice(weightMap, RANDOM);
    }

    /**
     * Same cumulative-weight walk as {@link #weightedChoice(Map)}, but drawing from a
     * caller-supplied {@link Random} instead of the shared static one — the entry point callers
     * on a seeded plan-generation path (e.g. {@code WeightedRandomSelectionStrategy}) use so their
     * draw participates in that caller's own reproducible sequence rather than the live shared RNG.
     */
    public static <T> T weightedChoice(@Nonnull Map<T, Double> weightMap, @Nonnull Random random) {
        if (weightMap.isEmpty()) {
            throw new IllegalArgumentException("weightMap must not be empty");
        }

        double totalWeight = weightMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        double targetWeight = random.nextDouble(0, totalWeight);
        double currentWeight = 0;
        T last = null;
        for (Map.Entry<T, Double> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            last = entry.getKey();
            if (currentWeight > targetWeight) {
                return last;
            }
        }

        // Floating-point accumulation can fall just short of totalWeight; the last entry is the correct pick
        return last;
    }

    /**
     * Weighted pick that reads weights straight off the source list via {@code weigher}, so hot callers
     * don't allocate an intermediate weight map on every call.
     * <p>
     * Non-positive weights are ignored; returns empty when nothing is selectable.
     */
    public static <T> Optional<T> weightedChoice(@Nonnull List<T> items, @Nonnull ToDoubleFunction<T> weigher) {
        double totalWeight = 0;
        for (T item : items) {
            double weight = weigher.applyAsDouble(item);
            if (weight > 0) {
                totalWeight += weight;
            }
        }
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        double targetWeight = randomDouble(0, totalWeight);
        double currentWeight = 0;
        T last = null;
        for (T item : items) {
            double weight = weigher.applyAsDouble(item);
            if (weight <= 0) {
                continue;
            }
            currentWeight += weight;
            last = item;
            if (currentWeight > targetWeight) {
                return Optional.of(last);
            }
        }

        // Floating-point accumulation can fall just short of totalWeight; the last positive entry is the correct pick
        return Optional.ofNullable(last);
    }

    /**
     * Shuffles the given list in-place and returns the list itself
     */
    public static <T> ArrayList<T> shuffle(ArrayList<T> list) {
        Collections.shuffle(list);
        return list;
    }

    public static String randomString(int length) {
        return RANDOM.ints(48, 123)
                .filter(i -> (i < 58 || i > 64) && (i < 91 || i > 96))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Flips a coin with the given chance of heads
     *
     * @param chance the chance of heads, between 0 and 1
     */
    public static boolean chance(double chance) {
        return RANDOM.nextDouble() < chance;
    }

    public static int stochasticRound(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }

        int floor = (int) Math.floor(value);
        return floor + (chance(value - floor) ? 1 : 0);
    }

    public static void chance(double chance, @Nonnull Runnable runnable) {
        if (chance(chance)) {
            runnable.run();
        }
    }

    /**
     * Generates a random value using a normal (Gaussian) distribution.
     * Useful for stats where most values should group around a center (mean),
     * with rare outliers on the high and low ends.
     *
     * @param mean              The center of the bell curve.
     * @param standardDeviation How wide the curve is (smaller = tighter grouping).
     */
    public static double randomGaussian(double mean, double standardDeviation) {
        double gaussian = RANDOM.nextGaussian();
        return mean + (gaussian * standardDeviation);
    }

}
