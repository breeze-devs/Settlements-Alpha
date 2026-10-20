package dev.breezes.settlements.domain.settlement.naming;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementNameGeneratorTest {

    // Each seed starts the walk at its own pairing and stride, so many seeds cover the small pools here many ways
    private static final int SEEDS = 64;

    private static final SettlementNamePools TWO_BY_TWO = new SettlementNamePools(List.of("Ash", "Oak"), List.of("ford", "wick"));
    private static final List<String> TWO_BY_TWO_NAMES = List.of("Ashford", "Ashwick", "Oakford", "Oakwick");

    @Test
    void generate_returnsAPrefixFollowedByItsSuffixInLowercase() {
        // Arrange — a suffix written capitalized must still read lowercase inside the name
        SettlementNamePools pools = new SettlementNamePools(List.of("Ash", "Oak"), List.of("Ford", "wick"));
        Set<String> pairings = Set.of("Ashford", "Ashwick", "Oakford", "Oakwick");

        for (long seed = 0; seed < SEEDS; seed++) {
            // Act
            Optional<String> name = SettlementNameGenerator.generate(pools, seed, List.of());

            // Assert
            assertTrue(name.isPresent() && pairings.contains(name.get()), "seed " + seed + " gave " + name);
        }
    }

    @Test
    void generate_skipsATakenName_ignoringCaseAndSurroundingSpace() {
        SettlementNamePools pools = new SettlementNamePools(List.of("Ash"), List.of("ford", "wick"));
        for (long seed = 0; seed < SEEDS; seed++) {
            // Arrange — take the name this seed picks when nothing is taken, spelled differently
            String unconstrained = SettlementNameGenerator.generate(pools, seed, List.of()).orElseThrow();
            String otherPairing = unconstrained.equals("Ashford") ? "Ashwick" : "Ashford";
            List<String> taken = List.of("  " + unconstrained.toUpperCase(Locale.ROOT) + " ");

            // Act
            Optional<String> name = SettlementNameGenerator.generate(pools, seed, taken);

            // Assert
            assertEquals(Optional.of(otherPairing), name, "seed " + seed);
        }
    }

    @Test
    void generate_reachesEveryPairing_beforeFallingBackToANumeral() {
        // Arrange — six pairings, a count that shares factors with most strides, so a walk that skips any is caught
        SettlementNamePools pools = new SettlementNamePools(List.of("Ash", "Oak"), List.of("ford", "wick", "ton"));
        List<String> pairings = List.of("Ashford", "Ashwick", "Ashton", "Oakford", "Oakwick", "Oakton");

        for (String free : pairings) {
            List<String> taken = pairings.stream().filter(pairing -> !pairing.equals(free)).toList();
            for (long seed = 0; seed < SEEDS; seed++) {
                // Act
                Optional<String> name = SettlementNameGenerator.generate(pools, seed, taken);

                // Assert
                assertEquals(Optional.of(free), name, "seed " + seed);
            }
        }
    }

    @Test
    void generate_numbersTheSeedsFirstPairing_whenEveryPairingIsTaken() {
        for (long seed = 0; seed < SEEDS; seed++) {
            // Arrange
            String first = SettlementNameGenerator.generate(TWO_BY_TWO, seed, List.of()).orElseThrow();

            // Act
            Optional<String> name = SettlementNameGenerator.generate(TWO_BY_TWO, seed, TWO_BY_TWO_NAMES);

            // Assert
            assertEquals(Optional.of(first + " II"), name, "seed " + seed);
        }
    }

    @Test
    void generate_takesTheNextNumeral_whenTheFallbacksFirstNumeralIsTaken() {
        for (long seed = 0; seed < SEEDS; seed++) {
            // Arrange
            String first = SettlementNameGenerator.generate(TWO_BY_TWO, seed, List.of()).orElseThrow();
            List<String> taken = new ArrayList<>(TWO_BY_TWO_NAMES);
            taken.add(first + " II");

            // Act
            Optional<String> name = SettlementNameGenerator.generate(TWO_BY_TWO, seed, taken);

            // Assert
            assertEquals(Optional.of(first + " III"), name, "seed " + seed);
        }
    }

    @Test
    void generate_yieldsNothing_whenThePrefixPoolIsEmpty() {
        // Arrange
        SettlementNamePools pools = new SettlementNamePools(List.of(), List.of("ford"));

        // Act
        Optional<String> name = SettlementNameGenerator.generate(pools, 0, List.of());

        // Assert
        assertEquals(Optional.empty(), name);
    }

    @Test
    void generate_yieldsNothing_whenTheSuffixPoolIsEmpty() {
        // Arrange
        SettlementNamePools pools = new SettlementNamePools(List.of("Ash"), List.of());

        // Act
        Optional<String> name = SettlementNameGenerator.generate(pools, 0, List.of());

        // Assert
        assertEquals(Optional.empty(), name);
    }

    @Test
    void generate_variesTheName_acrossSeeds() {
        // Arrange — a seed that never moved the starting pairing would name every settlement alike until it is taken
        SettlementNamePools pools = new SettlementNamePools(
                List.of("Ash", "Oak", "Elm", "Yew", "Fir"), List.of("ford", "wick", "ton", "by", "dale"));

        // Act
        long distinct = LongStream.range(0, SEEDS)
                .mapToObj(seed -> SettlementNameGenerator.generate(pools, seed, List.of()).orElseThrow())
                .distinct()
                .count();

        // Assert
        assertTrue(distinct > 1, "every seed gave the same name");
    }

    @Test
    void canGenerate_isFalse_whenThePrefixPoolIsEmpty() {
        // Arrange
        SettlementNamePools pools = new SettlementNamePools(List.of(), List.of("ford"));

        // Act & Assert
        assertFalse(SettlementNameGenerator.canGenerate(pools));
    }

    @Test
    void canGenerate_isFalse_whenTheSuffixPoolIsEmpty() {
        // Arrange
        SettlementNamePools pools = new SettlementNamePools(List.of("Ash"), List.of());

        // Act & Assert
        assertFalse(SettlementNameGenerator.canGenerate(pools));
    }

    @Test
    void canGenerate_isTrue_whenBothPoolsHoldWords_andGenerateThenYieldsEvenWithEveryPairingTaken() {
        // Act
        boolean canGenerate = SettlementNameGenerator.canGenerate(TWO_BY_TWO);
        Optional<String> name = SettlementNameGenerator.generate(TWO_BY_TWO, 0, TWO_BY_TWO_NAMES);

        // Assert
        assertTrue(canGenerate);
        assertTrue(name.isPresent());
    }

    @Test
    void numbered_keepsThePreferredNameTrimmed_whenItIsFree() {
        // Act
        String name = SettlementNameGenerator.numbered("  Ashford ", List.of("Oakwick"));

        // Assert
        assertEquals("Ashford", name);
    }

    @Test
    void numbered_addsII_whenThePreferredNameIsTakenInAnotherCase() {
        // Act
        String name = SettlementNameGenerator.numbered("Ashford", List.of(" ASHFORD"));

        // Assert
        assertEquals("Ashford II", name);
    }

    @Test
    void numbered_takesTheLowestFreeRomanNumeral() {
        // Arrange — the taken numerals include the subtractive IV, so a numeral written IIII would read as free
        List<String> taken = List.of("Ashford", "Ashford II", "Ashford III", "Ashford IV", "Ashford V", "Ashford VI",
                "Ashford VII", "Ashford VIII");

        // Act
        String name = SettlementNameGenerator.numbered("Ashford", taken);

        // Assert
        assertEquals("Ashford IX", name);
    }

}
