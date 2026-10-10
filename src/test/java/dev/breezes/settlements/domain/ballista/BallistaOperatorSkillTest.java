package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaOperatorSkillTest {

    // The widest scatter a server could configure, owned here rather than borrowed from the config's default
    private static final double MAX_SCATTER_DEGREES = 8.0;

    // From the lowest gene value to the highest, rising
    private static final double[] RISING_GENE_VALUES = {Gene.MINIMUM_VALUE, 0.25, 0.5, 0.75, Gene.MAXIMUM_VALUE};

    private static final double MIDDLING_GENE_VALUE = 0.5;

    // Each end of a draw's range and points between
    private static final double[] DRAWS = {0.0, 0.125, 0.25, 0.4, 0.5, 0.75, 0.9, 1.0};

    // A level target at a middling range, which a still bolt reaches near the middle of the pitch range
    private static final Vec3 LEVEL_TARGET = new Vec3(0.0, 0.0, 30.0);
    private static final double LAUNCH_SPEED = 3.9;

    private static final double TOLERANCE = 1.0E-4;

    private static final Expertise[] RISING_TIERS = Arrays.stream(Expertise.values())
            .sorted(Comparator.comparingInt(Expertise::getLevel))
            .toArray(Expertise[]::new);

    @Test
    void aHigherTier_neverScattersWiderLeadsWorseOrLoadsSlower_atAnyGenes() {
        for (double intelligence : RISING_GENE_VALUES) {
            for (double agility : RISING_GENE_VALUES) {
                for (int i = 1; i < RISING_TIERS.length; i++) {
                    // Act
                    BallistaOperatorSkill lower = skill(intelligence, agility, RISING_TIERS[i - 1]);
                    BallistaOperatorSkill higher = skill(intelligence, agility, RISING_TIERS[i]);

                    // Assert: a tier step that undoes the one before it makes promotion a penalty
                    String described = RISING_TIERS[i] + " over " + RISING_TIERS[i - 1] + " at INT " + intelligence
                            + " AGI " + agility;
                    assertTrue(higher.getScatterDegrees() <= lower.getScatterDegrees(), described + " scatters wider");
                    assertTrue(higher.getLeadQuality() >= lower.getLeadQuality(), described + " leads worse");
                    assertTrue(higher.getLoadDuration().getTicks() <= lower.getLoadDuration().getTicks(),
                            described + " loads slower");
                }
            }
        }
    }

    @Test
    void higherIntelligence_neverScattersWiderOrLeadsWorse_atAnyTier() {
        for (Expertise tier : RISING_TIERS) {
            for (int i = 1; i < RISING_GENE_VALUES.length; i++) {
                // Act
                BallistaOperatorSkill duller = skill(RISING_GENE_VALUES[i - 1], MIDDLING_GENE_VALUE, tier);
                BallistaOperatorSkill brighter = skill(RISING_GENE_VALUES[i], MIDDLING_GENE_VALUE, tier);

                // Assert: INT working backwards makes the cleverest villagers the worst shots
                String described = tier + " at INT " + RISING_GENE_VALUES[i];
                assertTrue(brighter.getScatterDegrees() <= duller.getScatterDegrees(), described + " scatters wider");
                assertTrue(brighter.getLeadQuality() >= duller.getLeadQuality(), described + " leads worse");
            }
        }
    }

    @Test
    void higherAgility_neverLoadsSlower_atAnyTier() {
        for (Expertise tier : RISING_TIERS) {
            for (int i = 1; i < RISING_GENE_VALUES.length; i++) {
                // Act
                BallistaOperatorSkill clumsier = skill(MIDDLING_GENE_VALUE, RISING_GENE_VALUES[i - 1], tier);
                BallistaOperatorSkill nimbler = skill(MIDDLING_GENE_VALUE, RISING_GENE_VALUES[i], tier);

                // Assert: AGI working backwards makes the nimblest villagers the slowest loaders
                assertTrue(nimbler.getLoadDuration().getTicks() <= clumsier.getLoadDuration().getTicks(),
                        tier + " at AGI " + RISING_GENE_VALUES[i] + " loads slower");
            }
        }
    }

    @Test
    void theTierIntelligenceAndAgility_eachImproveTheMagnitudesTheyScale() {
        // Act
        BallistaOperatorSkill novice = skill(MIDDLING_GENE_VALUE, MIDDLING_GENE_VALUE, Expertise.NOVICE);
        BallistaOperatorSkill master = skill(MIDDLING_GENE_VALUE, MIDDLING_GENE_VALUE, Expertise.MASTER);
        BallistaOperatorSkill dullest = skill(Gene.MINIMUM_VALUE, MIDDLING_GENE_VALUE, Expertise.JOURNEYMAN);
        BallistaOperatorSkill brightest = skill(Gene.MAXIMUM_VALUE, MIDDLING_GENE_VALUE, Expertise.JOURNEYMAN);
        BallistaOperatorSkill clumsiest = skill(MIDDLING_GENE_VALUE, Gene.MINIMUM_VALUE, Expertise.JOURNEYMAN);
        BallistaOperatorSkill nimblest = skill(MIDDLING_GENE_VALUE, Gene.MAXIMUM_VALUE, Expertise.JOURNEYMAN);

        // Assert: an input left unwired passes every "never worse" check while changing nothing a player can see
        assertTrue(master.getScatterDegrees() < novice.getScatterDegrees(), "the tier does not narrow the scatter");
        assertTrue(master.getLeadQuality() > novice.getLeadQuality(), "the tier does not improve the lead");
        assertTrue(master.getLoadDuration().getTicks() < novice.getLoadDuration().getTicks(),
                "the tier does not shorten the load");
        assertTrue(brightest.getScatterDegrees() < dullest.getScatterDegrees(), "INT does not narrow the scatter");
        assertTrue(brightest.getLeadQuality() > dullest.getLeadQuality(), "INT does not improve the lead");
        assertTrue(nimblest.getLoadDuration().getTicks() < clumsiest.getLoadDuration().getTicks(),
                "AGI does not shorten the load");
    }

    @Test
    void eachGene_movesOnlyTheMagnitudesItScales() {
        for (Expertise tier : RISING_TIERS) {
            for (double fixed : RISING_GENE_VALUES) {
                // Act
                BallistaOperatorSkill clumsiest = skill(fixed, Gene.MINIMUM_VALUE, tier);
                BallistaOperatorSkill nimblest = skill(fixed, Gene.MAXIMUM_VALUE, tier);
                BallistaOperatorSkill dullest = skill(Gene.MINIMUM_VALUE, fixed, tier);
                BallistaOperatorSkill brightest = skill(Gene.MAXIMUM_VALUE, fixed, tier);

                // Assert: AGI steadying the aim, or INT speeding the load, crosses the genes' visible axes
                String described = tier + " with the other gene at " + fixed;
                assertEquals(clumsiest.getScatterDegrees(), nimblest.getScatterDegrees(), described + ": AGI scatters");
                assertEquals(clumsiest.getLeadQuality(), nimblest.getLeadQuality(), described + ": AGI leads");
                assertEquals(dullest.getLoadDuration(), brightest.getLoadDuration(), described + ": INT loads");
            }
        }
    }

    @Test
    void theLeastSkilledOperator_scattersTheConfiguredWidest_andNobodyScattersWider() {
        // Act
        BallistaOperatorSkill leastSkilled = skill(Gene.MINIMUM_VALUE, MIDDLING_GENE_VALUE, Expertise.NOVICE);

        // Assert: the configured value is the scatter a server owner sees on their worst gunner, never exceeded
        assertEquals(MAX_SCATTER_DEGREES, leastSkilled.getScatterDegrees(), TOLERANCE);
        for (Expertise tier : RISING_TIERS) {
            for (double intelligence : RISING_GENE_VALUES) {
                double scatter = skill(intelligence, MIDDLING_GENE_VALUE, tier).getScatterDegrees();
                assertTrue(scatter <= MAX_SCATTER_DEGREES, tier + " at INT " + intelligence + " scatters wider");
            }
        }
    }

    @Test
    void everyMagnitude_staysInItsBand_atEveryTierAndGeneExtreme() {
        double[] extremes = {Gene.MINIMUM_VALUE, Gene.MAXIMUM_VALUE};
        double[] widestScatters = {0.0, MAX_SCATTER_DEGREES};
        for (Expertise tier : RISING_TIERS) {
            for (double intelligence : extremes) {
                for (double agility : extremes) {
                    for (double widestScatter : widestScatters) {
                        // Act
                        BallistaOperatorSkill skill = BallistaOperatorSkill.of(genetics(intelligence, agility), tier,
                                widestScatter);

                        // Assert: a negative scatter, a lead past either end of the full one, or a load that takes no
                        // time is not a magnitude a player can see
                        String described = tier + " at INT " + intelligence + " AGI " + agility
                                + " widest scatter " + widestScatter;
                        assertTrue(skill.getScatterDegrees() >= 0.0, described + " scatters negatively");
                        assertTrue(skill.getLeadQuality() >= 0.0 && skill.getLeadQuality() <= 1.0,
                                described + " leads by " + skill.getLeadQuality());
                        assertTrue(skill.getLoadDuration().getTicks() > 0, described + " loads instantly");
                    }
                }
            }
        }
    }

    @Test
    void scatter_neverStraysBeyondTheScatter_andReachesItAtTheFullestDraw() {
        BallistaFiringSolution solution = levelSolution();
        for (Expertise tier : RISING_TIERS) {
            for (double intelligence : RISING_GENE_VALUES) {
                BallistaOperatorSkill skill = skill(intelligence, MIDDLING_GENE_VALUE, tier);
                assertSitsInsideThePitchRange(solution, skill);
                for (double directionDraw : DRAWS) {
                    for (double distanceDraw : DRAWS) {
                        // Act
                        BallistaFiringSolution scattered = skill.scatter(solution, directionDraw, distanceDraw);

                        // Assert: a shot past the scatter is a hidden roll; a fullest draw short of it shrinks the
                        // scatter the player is shown
                        double stray = stray(solution, scattered);
                        String described = tier + " at INT " + intelligence + " drawing " + directionDraw + ", "
                                + distanceDraw;
                        assertTrue(stray <= skill.getScatterDegrees() + TOLERANCE, described + " strays " + stray);
                        if (distanceDraw == 1.0) {
                            assertEquals(skill.getScatterDegrees(), stray, TOLERANCE, described);
                        }
                    }
                }
            }
        }
    }

    @Test
    void scatter_straysOppositeWays_forOppositeDirectionDraws() {
        // Arrange
        BallistaFiringSolution solution = levelSolution();
        BallistaOperatorSkill skill = skill(Gene.MINIMUM_VALUE, MIDDLING_GENE_VALUE, Expertise.NOVICE);
        assertSitsInsideThePitchRange(solution, skill);

        for (double directionDraw : DRAWS) {
            if (directionDraw > 0.5) {
                continue;
            }

            // Act
            BallistaFiringSolution one = skill.scatter(solution, directionDraw, 1.0);
            BallistaFiringSolution opposite = skill.scatter(solution, directionDraw + 0.5, 1.0);

            // Assert: strays that do not cancel pull every shot the same way, a bias rather than a spread around the aim
            double yawSum = wrapped(one.getYaw() - solution.getYaw()) + wrapped(opposite.getYaw() - solution.getYaw());
            double pitchSum = (one.getPitch() - solution.getPitch()) + (opposite.getPitch() - solution.getPitch());
            assertEquals(0.0, yawSum, TOLERANCE, "yaw strays at direction " + directionDraw);
            assertEquals(0.0, pitchSum, TOLERANCE, "pitch strays at direction " + directionDraw);
        }
    }

    private static BallistaOperatorSkill skill(double intelligence, double agility, Expertise tier) {
        return BallistaOperatorSkill.of(genetics(intelligence, agility), tier, MAX_SCATTER_DEGREES);
    }

    private static GeneticsProfile genetics(double intelligence, double agility) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        genes.put(GeneType.INTELLIGENCE, new Gene(intelligence));
        genes.put(GeneType.AGILITY, new Gene(agility));
        return new GeneticsProfile(genes);
    }

    private static BallistaFiringSolution levelSolution() {
        return BallistaFiringSolution.solve(LEVEL_TARGET, Vec3.ZERO, LAUNCH_SPEED).orElseThrow();
    }

    /**
     * Fails unless the solution is at least the skill's scatter inside both ends of the pitch range, where clamping
     * cannot shorten a stray.
     */
    private static void assertSitsInsideThePitchRange(BallistaFiringSolution solution, BallistaOperatorSkill skill) {
        assertTrue(solution.getPitch() - skill.getScatterDegrees() >= BallistaAim.MIN_PITCH_DEGREES
                        && solution.getPitch() + skill.getScatterDegrees() <= BallistaAim.MAX_PITCH_DEGREES,
                "the level target's pitch " + solution.getPitch() + " leaves no room for the scatter");
    }

    private static double stray(BallistaFiringSolution from, BallistaFiringSolution to) {
        return Math.hypot(wrapped(to.getYaw() - from.getYaw()), to.getPitch() - from.getPitch());
    }

    /**
     * The given turn in degrees, taken the short way around.
     */
    private static double wrapped(double degrees) {
        double turn = degrees % 360.0;
        if (turn >= 180.0) {
            return turn - 360.0;
        }
        if (turn < -180.0) {
            return turn + 360.0;
        }
        return turn;
    }

}
