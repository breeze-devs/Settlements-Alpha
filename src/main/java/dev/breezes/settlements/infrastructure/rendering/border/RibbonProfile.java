package dev.breezes.settlements.infrastructure.rendering.border;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;

/**
 * The side view of a border's ribbon along one straight edge: a band standing on the ground of each block column the edge
 * runs past, with a riser up the face of every step in the ground, so the band stays unbroken however tall the step.
 * <p>
 * The pieces tile the ribbon without overlapping, so a translucent ribbon drawn from them never doubles up where two
 * pieces meet. Each vertex carries a ground weight: 1 on the ground, falling linearly to 0 at the band's outer edge,
 * and equal on both sides of every edge two pieces share.
 */
@ClientSide
final class RibbonProfile {

    private final double height;
    private final double riserReach;

    /**
     * @param height     how far the band reaches above flat ground, in blocks
     * @param riserReach how far a riser reaches out from the face of a step, in blocks; a column with a riser at each
     *                   side gives each at most half its width
     */
    RibbonProfile(double height, double riserReach) {
        Validate.isTrue(height > 0.0D, "height must be positive: %s", height);
        Validate.isTrue(riserReach > 0.0D, "riserReach must be positive: %s", riserReach);
        this.height = height;
        this.riserReach = riserReach;
    }

    /**
     * Emits the ribbon along a row of block columns. Column i spans the positions from i to i + 1 along the edge. The
     * row's own columns are those ground names, and the columns just before and after the row are -1 and ground.length.
     * <p>
     * The ribbon spans the positions from start to end and stands on the ground of every column it spans, including
     * part of a column beside the row where it reaches into one. A riser climbs from a column to a higher neighbor on
     * the lower column's side of the face. A column beside the row that the ribbon does not span gets one only when its
     * ground is given.
     *
     * @param ground       the height of the ground surface in each of the row's columns
     * @param start        where the ribbon starts along the edge; more than -1 and less than 1
     * @param end          where the ribbon ends along the edge; more than the last column's index and less than
     *                     ground.length + 1
     * @param groundBefore the ground of the column just before the row, or NaN when it is not given; given whenever
     *                     start is negative
     * @param groundAfter  the ground of the column just after the row, or NaN when it is not given; given whenever end
     *                     is more than ground.length
     */
    void emit(@Nonnull double[] ground, double start, double end, double groundBefore, double groundAfter,
              @Nonnull Sink sink) {
        int last = ground.length - 1;
        Validate.isTrue(last >= 0, "ground must name at least one column");
        Validate.isTrue(start > -1.0D && start < 1.0D, "start must lie in the first column or the one before it: %s",
                start);
        Validate.isTrue(end > last && end < ground.length + 1.0D,
                "end must lie in the last column or the one after it: %s", end);
        Validate.isTrue(start >= 0.0D || !Double.isNaN(groundBefore),
                "a ribbon starting before the row needs the ground there");
        Validate.isTrue(end <= ground.length || !Double.isNaN(groundAfter),
                "a ribbon ending after the row needs the ground there");

        int firstSpanned = start < 0.0D ? -1 : 0;
        int lastSpanned = end > ground.length ? ground.length : last;
        for (int column = firstSpanned; column <= lastSpanned; column++) {
            double floor = groundOf(ground, groundBefore, groundAfter, column);
            double left = column == firstSpanned ? start : column;
            double right = column == lastSpanned ? end : column + 1;
            // A NaN neighbor compares false here, so a column whose ground is not given gets no riser
            double leftStep = groundOf(ground, groundBefore, groundAfter, column - 1) - floor;
            double rightStep = groundOf(ground, groundBefore, groundAfter, column + 1) - floor;
            boolean leftRiser = leftStep > 0.0D;
            boolean rightRiser = rightStep > 0.0D;

            double reachLimit = (leftRiser && rightRiser ? 0.5D : 1.0D) * (right - left);
            double leftReach = leftRiser ? Math.min(this.riserReach, reachLimit) : 0.0D;
            double rightReach = rightRiser ? Math.min(this.riserReach, reachLimit) : 0.0D;
            double floorLeft = left + leftReach * this.riserShare(leftStep);
            double floorRight = right - rightReach * this.riserShare(rightStep);

            if (floorRight > floorLeft) {
                quad(sink,
                        floorLeft, floor, 1.0F,
                        floorRight, floor, 1.0F,
                        floorRight, floor + this.height, 0.0F,
                        floorLeft, floor + this.height, 0.0F);
            }
            if (leftRiser) {
                this.emitRiser(sink, left, 1.0D, floor, leftStep, leftReach);
            }
            if (rightRiser) {
                this.emitRiser(sink, right, -1.0D, floor, rightStep, rightReach);
            }
        }
    }

    /**
     * Returns the ground of a column of the row or beside it, or NaN for a column further out.
     */
    private static double groundOf(@Nonnull double[] ground, double groundBefore, double groundAfter, int column) {
        if (column == -1) {
            return groundBefore;
        }
        if (column == ground.length) {
            return groundAfter;
        }
        return column >= 0 && column < ground.length ? ground[column] : Double.NaN;
    }

    /**
     * The riser against the face of one step up from a column's floor, standing on the column's side of the face.
     *
     * @param face    the position of the face along the edge
     * @param outward the direction along the edge from the face into the column, 1 or -1
     */
    private void emitRiser(@Nonnull Sink sink, double face, double outward, double floor, double step, double reach) {
        // The pieces meet where the floor's fade with height and the face's fade with distance from it are equal, so
        // the weight is continuous across them without either piece overlapping the other. A step lower than the band
        // ends that miter early, at the top of the step.
        double share = this.riserShare(step);
        double miterAlong = face + outward * reach * share;
        double miterTop = floor + this.height * share;
        double top = floor + step;
        float miterWeight = (float) (1.0D - share);

        // The floor beside the face, below the miter
        quad(sink,
                face, floor, 1.0F,
                miterAlong, floor, 1.0F,
                miterAlong, miterTop, miterWeight,
                miterAlong, miterTop, miterWeight);
        // The face, above the miter and up to the higher ground
        quad(sink,
                face, floor, 1.0F,
                face, top, 1.0F,
                miterAlong, top, miterWeight,
                miterAlong, miterTop, miterWeight);
        // The corner over the edge of the higher ground, fading with both height and distance from the face, where it
        // meets the higher column's band
        quad(sink,
                face, top, 1.0F,
                face, top + this.height, 0.0F,
                miterAlong, top + this.height * (1.0D - share), 0.0F,
                miterAlong, top, miterWeight);
    }

    /**
     * Returns how much of a riser's reach its pieces span for a step of the given height: all of it for a step at least
     * as tall as the band, and the step's share of the band's height for a lower one.
     */
    private double riserShare(double step) {
        return step > 0.0D ? Math.min(step / this.height, 1.0D) : 0.0D;
    }

    private static void quad(@Nonnull Sink sink,
                             double along0, double y0, float weight0,
                             double along1, double y1, float weight1,
                             double along2, double y2, float weight2,
                             double along3, double y3, float weight3) {
        sink.vertex(along0, y0, weight0);
        sink.vertex(along1, y1, weight1);
        sink.vertex(along2, y2, weight2);
        sink.vertex(along3, y3, weight3);
    }

    /**
     * Receives a ribbon's pieces as quads, four vertices at a time. Each quad is convex and splits into the triangles
     * of its vertices 0, 1, 2 and 2, 3, 0; a triangle repeats its last vertex.
     */
    @FunctionalInterface
    interface Sink {

        /**
         * Receives one vertex at a position along the edge and a height, with its ground weight.
         */
        void vertex(double along, double y, float groundWeight);

    }

}
