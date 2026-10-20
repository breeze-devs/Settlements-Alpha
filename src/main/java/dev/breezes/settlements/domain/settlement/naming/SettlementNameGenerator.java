package dev.breezes.settlements.domain.settlement.naming;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Picks settlement names that differ from every name already taken. Names compare trimmed and ignoring case.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementNameGenerator {

    private static final int[] NUMERAL_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] NUMERAL_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * Returns whether generate yields a name from the pools, whatever names are taken: it does unless either pool is
     * empty.
     */
    public static boolean canGenerate(@Nonnull SettlementNamePools pools) {
        return !pools.prefixes().isEmpty() && !pools.suffixes().isEmpty();
    }

    /**
     * Returns a name made from the pools that is not taken or empty when either pool is empty.
     */
    public static Optional<String> generate(@Nonnull SettlementNamePools pools,
                                            long seed,
                                            @Nonnull Collection<String> takenNames) {
        if (!canGenerate(pools)) {
            return Optional.empty();
        }

        List<String> prefixes = pools.prefixes();
        List<String> suffixes = pools.suffixes();
        Set<String> taken = comparisonKeys(takenNames);
        long pairings = (long) prefixes.size() * suffixes.size();
        Random random = new Random(seed);
        long first = random.nextLong(pairings);
        // Stepping through the pairing indexes by a stride coprime to their count visits each index exactly once
        // before returning to the first, so every pairing is tried without building the cross product, which grows
        // with the product of two pack-extensible pools
        long stride = coprimeStride(pairings, random);

        long index = first;
        for (long tried = 0; tried < pairings; tried++) {
            String name = pairing(prefixes, suffixes, index);
            if (!taken.contains(comparisonKey(name))) {
                return Optional.of(name);
            }
            index += stride;
            if (index >= pairings) {
                index -= pairings;
            }
        }

        return Optional.of(numbered(pairing(prefixes, suffixes, first), taken));
    }

    /**
     * Returns the preferred name trimmed, or when that is taken, followed by the lowest Roman numeral from II up that
     * makes it free: "Ashford" becomes "Ashford II", then "Ashford III".
     *
     * @throws IllegalArgumentException if preferred is blank
     */
    public static String numbered(@Nonnull String preferred, @Nonnull Collection<String> takenNames) {
        Validate.notBlank(preferred, "Preferred settlement name must not be blank");
        return numbered(preferred.strip(), comparisonKeys(takenNames));
    }

    private static String numbered(String name, Set<String> taken) {
        if (!taken.contains(comparisonKey(name))) {
            return name;
        }

        // Unbounded on purpose: only finitely many names are taken, so the loop always reaches a free numeral
        for (int numeral = 2; ; numeral++) {
            String candidate = name + " " + romanNumeral(numeral);
            if (!taken.contains(comparisonKey(candidate))) {
                return candidate;
            }
        }
    }

    private static long coprimeStride(long pairings, Random random) {
        if (pairings == 1) {
            return 1;
        }
        long stride = 1 + random.nextLong(pairings - 1);
        // Wraps to 1 past the last stride, and 1 is coprime to every count, so the search always ends
        while (greatestCommonDivisor(stride, pairings) != 1) {
            stride = stride == pairings - 1 ? 1 : stride + 1;
        }
        return stride;
    }

    private static long greatestCommonDivisor(long a, long b) {
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    private static String pairing(List<String> prefixes, List<String> suffixes, long index) {
        int suffixCount = suffixes.size();
        String prefix = prefixes.get((int) (index / suffixCount));
        String suffix = suffixes.get((int) (index % suffixCount));
        return prefix + suffix.toLowerCase(Locale.ROOT);
    }

    private static String romanNumeral(int value) {
        StringBuilder numeral = new StringBuilder();
        int remaining = value;
        for (int i = 0; i < NUMERAL_VALUES.length; i++) {
            while (remaining >= NUMERAL_VALUES[i]) {
                numeral.append(NUMERAL_SYMBOLS[i]);
                remaining -= NUMERAL_VALUES[i];
            }
        }
        return numeral.toString();
    }

    private static Set<String> comparisonKeys(Collection<String> names) {
        Set<String> keys = new HashSet<>();
        for (String name : names) {
            keys.add(comparisonKey(name));
        }
        return keys;
    }

    private static String comparisonKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

}
