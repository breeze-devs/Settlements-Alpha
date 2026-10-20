package dev.breezes.settlements.infrastructure.rendering.border;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbonProfileTest {

    /**
     * The test's own ribbon: its band height, how far its risers reach out from a face, and the ground it stands on.
     */
    private static final double HEIGHT = 1.5D;
    private static final double REACH = 0.5D;
    private static final double GROUND = 64.0D;
    private static final int COLUMNS = 16;

    private static final RibbonProfile PROFILE = new RibbonProfile(HEIGHT, REACH);

    /**
     * How far a probe stands inside a piece's edge: well clear of floating-point error, close enough to read the edge.
     */
    private static final double NEAR = 0.01D;

    /**
     * The least weight that reads as the bright line along the ground or a face.
     */
    private static final double BRIGHT = 0.9D;

    /**
     * How far outside a triangle a point may sit and still count as on it.
     */
    private static final double ON_EDGE = 1e-9D;

    @Test
    void flatGround_standsOneBandOnEveryColumn_fadingFromGroundToTop() {
        // Arrange
        double[] ground = flat(COLUMNS);

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, 0.0D, COLUMNS, Double.NaN, Double.NaN);

        // Assert
        double top = GROUND + HEIGHT;
        for (double along = 0.05D; along < COLUMNS; along += 0.25D) {
            assertTrue(weightAt(ribbon, along, GROUND + NEAR) >= BRIGHT, "bright on the ground at " + along);
            assertEquals(0.0D, weightAt(ribbon, along, top - NEAR), NEAR, "faded out at the top at " + along);
            assertTrue(Double.isNaN(weightAt(ribbon, along, GROUND - NEAR)), "nothing below the ground at " + along);
            assertTrue(Double.isNaN(weightAt(ribbon, along, top + NEAR)), "nothing above the band at " + along);
        }
    }

    @ParameterizedTest(name = "{0} blocks, up: {1}")
    @MethodSource("steps")
    void step_climbsTheWholeFaceUnbroken(double rise, boolean up) {
        // Arrange
        double[] ground = stepAtMiddle(rise, up);
        // The face stands between columns 7 and 8; the riser stands on the lower column's side of it
        double besideFace = up ? COLUMNS / 2.0D - NEAR : COLUMNS / 2.0D + NEAR;

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, 0.0D, COLUMNS, Double.NaN, Double.NaN);

        // Assert
        for (double y = GROUND + NEAR; y < GROUND + rise; y += 0.05D) {
            assertTrue(weightAt(ribbon, besideFace, y) >= BRIGHT, "bright against the face at height " + y);
        }
    }

    @ParameterizedTest(name = "{0} blocks, up: {1}")
    @MethodSource("steps")
    void step_coversNoPointTwice(double rise, boolean up) {
        // Arrange
        double[] ground = stepAtMiddle(rise, up);

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, 0.0D, COLUMNS, Double.NaN, Double.NaN);

        // Assert
        assertCoversNoPointTwice(ribbon, COLUMNS / 2.0D - 2.0D, COLUMNS / 2.0D + 2.0D, GROUND, GROUND + rise + HEIGHT);
    }

    @ParameterizedTest(name = "{0} blocks, up: {1}")
    @MethodSource("steps")
    void step_fadesWithoutASeam(double rise, boolean up) {
        // Arrange
        double[] ground = stepAtMiddle(rise, up);
        double spacing = 0.02D;
        // No weight changes faster than across a riser's reach or the band's height
        double steepest = spacing * (1.0D / REACH + 1.0D / HEIGHT);

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, 0.0D, COLUMNS, Double.NaN, Double.NaN);

        // Assert
        double[][] weights = sampleWeights(ribbon, COLUMNS / 2.0D - 1.99D, spacing, 200,
                GROUND - 0.21D, spacing, (int) ((rise + HEIGHT + 0.42D) / spacing));
        for (int column = 0; column < weights.length; column++) {
            for (int row = 0; row < weights[column].length; row++) {
                double weight = weights[column][row];
                if (column + 1 < weights.length) {
                    assertNoSeam(weight, weights[column + 1][row], steepest, column, row);
                }
                if (row + 1 < weights[column].length) {
                    assertNoSeam(weight, weights[column][row + 1], steepest, column, row);
                }
            }
        }
    }

    @Test
    void pit_givesItsTwoRisersSeparateHalves() {
        // Arrange: one low column between two higher ones, and risers reaching further than half a column
        RibbonProfile wideRisers = new RibbonProfile(HEIGHT, 0.8D);
        double[] ground = {GROUND + 6.0D, GROUND, GROUND + 6.0D};

        // Act
        List<Triangle> ribbon = emit(wideRisers, ground, 0.0D, ground.length, Double.NaN, Double.NaN);

        // Assert
        assertCoversNoPointTwice(ribbon, 0.0D, ground.length, GROUND, GROUND + 6.0D + HEIGHT);
        for (double y = GROUND + NEAR; y < GROUND + 6.0D; y += 0.05D) {
            assertTrue(weightAt(ribbon, 1.0D + NEAR, y) >= BRIGHT, "bright against the west face at height " + y);
            assertTrue(weightAt(ribbon, 2.0D - NEAR, y) >= BRIGHT, "bright against the east face at height " + y);
        }
    }

    @ParameterizedTest(name = "past the start: {0}, neighbor''s rise: {1}")
    @CsvSource({
            "true, 6.0",
            "true, NaN",
            "false, 6.0",
            "false, NaN",
    })
    void end_climbsToTheGroundPastIt_onlyWhereTheRibbonGoesOn(boolean pastStart, double neighborRise) {
        // Arrange: a NaN rise leaves the neighbor's ground NaN, the ribbon not going on past that end
        double[] ground = flat(COLUMNS);
        double neighborGround = GROUND + neighborRise;
        double before = pastStart ? neighborGround : Double.NaN;
        double after = pastStart ? Double.NaN : neighborGround;
        double atEnd = pastStart ? NEAR : COLUMNS - NEAR;

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, 0.0D, COLUMNS, before, after);

        // Assert: above the band, against the end, only a riser can be drawn
        double aboveBand = GROUND + HEIGHT + 1.0D;
        assertEquals(!Double.isNaN(neighborGround), !Double.isNaN(weightAt(ribbon, atEnd, aboveBand)));
    }

    @ParameterizedTest(name = "from {0} to {1}")
    @CsvSource({
            // Reaching past both chunk corners
            "-0.3, 16.3",
            // Stopping short of both
            "0.3, 15.7",
            // Ending on them
            "0.0, 16.0",
    })
    void ends_spanExactlyFromStartToEnd(double start, double end) {
        // Arrange: level ground runs on past both ends, for the ribbon to stand on wherever it reaches past the row
        double[] ground = flat(COLUMNS);
        double onGround = GROUND + NEAR;

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, start, end, GROUND, GROUND);

        // Assert
        assertFalse(Double.isNaN(weightAt(ribbon, start + NEAR, onGround)), "covered just inside the start");
        assertTrue(Double.isNaN(weightAt(ribbon, start - NEAR, onGround)), "bare just before the start");
        assertFalse(Double.isNaN(weightAt(ribbon, end - NEAR, onGround)), "covered just inside the end");
        assertTrue(Double.isNaN(weightAt(ribbon, end + NEAR, onGround)), "bare just past the end");
    }

    @ParameterizedTest(name = "past the start: {0}, neighbor''s rise: {1}")
    @CsvSource({
            "true, 2.0",
            "true, -2.0",
            "false, 2.0",
            "false, -2.0",
    })
    void reach_intoTheColumnPastAnEnd_standsOnItsGround_andClimbsTheFaceBetween(boolean pastStart,
                                                                                double neighborRise) {
        // Arrange: the ribbon reaches part way into the column past one end, as it does at an inner corner
        double[] ground = flat(COLUMNS);
        double neighborGround = GROUND + neighborRise;
        double reach = 0.3D;
        double start = pastStart ? -reach : 0.0D;
        double end = pastStart ? COLUMNS : COLUMNS + reach;
        double face = pastStart ? 0.0D : COLUMNS;
        double intoNeighbor = pastStart ? -reach / 2.0D : COLUMNS + reach / 2.0D;
        // The riser stands on the lower column's side of the face
        double towardNeighbor = pastStart ? -1.0D : 1.0D;
        double besideFace = face + (neighborRise < 0.0D ? towardNeighbor : -towardNeighbor) * NEAR;
        double low = Math.min(GROUND, neighborGround);
        double high = Math.max(GROUND, neighborGround);

        // Act
        List<Triangle> ribbon = emit(PROFILE, ground, start, end,
                pastStart ? neighborGround : Double.NaN, pastStart ? Double.NaN : neighborGround);

        // Assert
        assertTrue(weightAt(ribbon, intoNeighbor, neighborGround + NEAR) >= BRIGHT, "bright on the neighbor's ground");
        for (double y = low + NEAR; y < high; y += 0.05D) {
            assertTrue(weightAt(ribbon, besideFace, y) >= BRIGHT, "bright against the face at height " + y);
        }
        assertCoversNoPointTwice(ribbon, face - 2.0D, face + 2.0D, low, high + HEIGHT);
    }

    static Stream<Arguments> steps() {
        // A step lower than a block, one block, exactly the band's height, taller than it, and a cliff
        return Stream.of(0.5D, 1.0D, HEIGHT, 3.0D, 18.0D)
                .flatMap(rise -> Stream.of(Arguments.of(rise, true), Arguments.of(rise, false)));
    }

    private static double[] flat(int columns) {
        double[] ground = new double[columns];
        Arrays.fill(ground, GROUND);
        return ground;
    }

    /**
     * Ground that steps up or down by the rise between the two halves of the row.
     */
    private static double[] stepAtMiddle(double rise, boolean up) {
        double[] ground = flat(COLUMNS);
        for (int column = 0; column < COLUMNS; column++) {
            boolean firstHalf = column < COLUMNS / 2;
            if (firstHalf != up) {
                ground[column] += rise;
            }
        }
        return ground;
    }

    private static List<Triangle> emit(RibbonProfile profile, double[] ground, double start, double end,
                                       double groundBefore, double groundAfter) {
        List<Vertex> vertices = new ArrayList<>();
        profile.emit(ground, start, end, groundBefore, groundAfter,
                (along, y, weight) -> vertices.add(new Vertex(along, y, weight)));
        assertEquals(0, vertices.size() % 4, "vertices must arrive as whole quads");

        // The split a quad is drawn with
        List<Triangle> triangles = new ArrayList<>();
        for (int first = 0; first < vertices.size(); first += 4) {
            triangles.add(new Triangle(vertices.get(first), vertices.get(first + 1), vertices.get(first + 2)));
            triangles.add(new Triangle(vertices.get(first + 2), vertices.get(first + 3), vertices.get(first)));
        }
        return triangles;
    }

    /**
     * Returns the weight a triangle containing the point interpolates there, or NaN when no triangle contains it.
     */
    private static double weightAt(List<Triangle> triangles, double along, double y) {
        for (Triangle triangle : triangles) {
            double[] coordinates = triangle.barycentric(along, y);
            if (coordinates != null && Arrays.stream(coordinates).allMatch(value -> value >= -ON_EDGE)) {
                return coordinates[0] * triangle.first().weight()
                        + coordinates[1] * triangle.second().weight()
                        + coordinates[2] * triangle.third().weight();
            }
        }
        return Double.NaN;
    }

    /**
     * Asserts that no point of a grid over the region, from below the given bottom to above the given top, lies
     * strictly inside two triangles. A point on an edge two pieces share lies strictly inside neither.
     */
    private static void assertCoversNoPointTwice(List<Triangle> triangles, double fromAlong, double toAlong,
                                                 double bottom, double top) {
        // An irregular spacing keeps the grid from tracking any piece's edge
        for (double along = fromAlong + 0.0123D; along < toAlong; along += 0.0371D) {
            for (double y = bottom - 0.5D; y < top + 0.5D; y += 0.0371D) {
                int covering = 0;
                for (Triangle triangle : triangles) {
                    double[] coordinates = triangle.barycentric(along, y);
                    if (coordinates != null && Arrays.stream(coordinates).allMatch(value -> value > ON_EDGE)) {
                        covering++;
                    }
                }
                assertTrue(covering <= 1, "covered " + covering + " times at (" + along + ", " + y + ")");
            }
        }
    }

    private static double[][] sampleWeights(List<Triangle> triangles, double fromAlong, double alongSpacing,
                                            int columns, double fromY, double ySpacing, int rows) {
        double[][] weights = new double[columns][rows];
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                weights[column][row] = weightAt(triangles, fromAlong + column * alongSpacing, fromY + row * ySpacing);
            }
        }
        return weights;
    }

    /**
     * Asserts two neighboring samples differ by no more than the steepest fade allows, where both lie on the ribbon.
     */
    private static void assertNoSeam(double weight, double neighborWeight, double steepest, int column, int row) {
        if (Double.isNaN(weight) || Double.isNaN(neighborWeight)) {
            return;
        }
        assertTrue(Math.abs(weight - neighborWeight) <= steepest + 1e-6D,
                "weight jumps from " + weight + " to " + neighborWeight + " at sample (" + column + ", " + row + ")");
    }

    private record Vertex(double along, double y, double weight) {
    }

    private record Triangle(Vertex first, Vertex second, Vertex third) {

        /**
         * Returns the point's barycentric coordinates in this triangle, or null when the triangle has no area.
         */
        @Nullable
        double[] barycentric(double along, double y) {
            double determinant = (this.second.y() - this.third.y()) * (this.first.along() - this.third.along())
                    + (this.third.along() - this.second.along()) * (this.first.y() - this.third.y());
            if (Math.abs(determinant) < 1e-12D) {
                return null;
            }
            double firstWeight = ((this.second.y() - this.third.y()) * (along - this.third.along())
                    + (this.third.along() - this.second.along()) * (y - this.third.y())) / determinant;
            double secondWeight = ((this.third.y() - this.first.y()) * (along - this.third.along())
                    + (this.first.along() - this.third.along()) * (y - this.third.y())) / determinant;
            return new double[]{firstWeight, secondWeight, 1.0D - firstWeight - secondWeight};
        }

    }

}
