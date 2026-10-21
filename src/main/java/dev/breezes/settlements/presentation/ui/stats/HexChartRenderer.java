package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.presentation.ui.framework.Bounds;
import dev.breezes.settlements.presentation.ui.framework.GuiLineDrawing;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Renders the hexagonal genetics radar chart for the villager stats screen.
 * Uses Tesselator-based drawing via {@link GuiLineDrawing} for efficient batched rendering.
 * <p>
 * Gene data rarely changes between snapshots — vertex positions are cached and only
 * recalculated when gene values actually change.
 */
@ClientSide
class HexChartRenderer {

    private static final int NUM_AXES = 6;
    private static final int CONCENTRIC_RINGS = 5;
    private static final int LABEL_MARGIN = 14;

    private static final int GRID_LINE_COLOR = 0x30FFFFFF;
    private static final int AXIS_LINE_COLOR = 0x20FFFFFF;
    private static final int DATA_FILL_COLOR = 0xA055FF55;

    // Grade thresholds: A >= 0.80, B >= 0.60, C >= 0.40, D >= 0.20, E < 0.20
    private static final double GRADE_A_THRESHOLD = 0.80;
    private static final double GRADE_B_THRESHOLD = 0.60;
    private static final double GRADE_C_THRESHOLD = 0.40;
    private static final double GRADE_D_THRESHOLD = 0.20;

    /*
     * Cached states to avoid recalculating vertices every frame
     */
    @Nullable
    private double[] cachedGeneValues;
    @Nullable
    private float[][] cachedDataVertices;
    @Nullable
    private float[][] cachedOuterVertices;
    private float cachedCx;
    private float cachedCy;
    private float cachedRadius;

    public void render(@Nonnull GuiGraphics graphics,
                       @Nonnull Bounds bounds,
                       int mouseX,
                       int mouseY,
                       @Nonnull Font font,
                       @Nonnull double[] geneValues,
                       @Nonnull UITheme theme) {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        // Center and radius scale to fit available space with margin for labels
        float cx = bounds.x() + bounds.width() / 2.0F;
        float cy = bounds.y() + bounds.height() / 2.0F;
        float radius = Math.min(bounds.width(), bounds.height()) / 2.0F - LABEL_MARGIN;
        if (radius <= 0) {
            return;
        }

        // Draw concentric hexagon rings
        for (int ring = 1; ring <= CONCENTRIC_RINGS; ring++) {
            float ringRadius = radius * ring / CONCENTRIC_RINGS;
            GuiLineDrawing.drawHexagon(graphics, cx, cy, ringRadius, GRID_LINE_COLOR);
        }

        // Draw axis lines through the center connecting opposite vertices
        float[][] outerVertices = getOrComputeOuterVertices(cx, cy, radius);
        for (int i = 0; i < NUM_AXES / 2; i++) {
            int opposite = i + NUM_AXES / 2;
            GuiLineDrawing.drawLine(graphics,
                    outerVertices[i][0], outerVertices[i][1],
                    outerVertices[opposite][0], outerVertices[opposite][1],
                    AXIS_LINE_COLOR);
        }

        // Compute data polygon vertices
        float[][] dataVertices = getOrComputeDataVertices(cx, cy, radius, geneValues);

        // Fill data polygon
        GuiLineDrawing.fillStarPolygon(graphics, cx, cy, dataVertices, DATA_FILL_COLOR);

        // Draw abbreviation labels at each axis
        GeneType[] genes = GeneType.VALUES;
        for (int i = 0; i < NUM_AXES; i++) {
            float labelRadius = radius + LABEL_MARGIN;
            double angle = Math.toRadians(-90 + i * 60);
            float lx = cx + labelRadius * (float) Math.cos(angle);
            float ly = cy + labelRadius * (float) Math.sin(angle);

            String abbrev = genes[i].getAbbreviation();
            int textWidth = font.width(abbrev);
            int drawX = Math.round(lx) - textWidth / 2;
            int drawY = Math.round(ly) - font.lineHeight / 2;
            graphics.drawString(font, abbrev, drawX, drawY, theme.subtleTextColor(), false);
        }

        // Hover: detect which axis the mouse is closest to and show detail text
        int hoveredAxis = getHoveredAxis(cx, cy, mouseX, mouseY, bounds);
        if (hoveredAxis >= 0 && hoveredAxis < genes.length) {
            double value = hoveredAxis < geneValues.length ? geneValues[hoveredAxis] : 0;
            String grade = getGradeLetter(value);
            String formattedValue = String.valueOf(Math.round(value * 100));
            Component detail = Component.translatable(genes[hoveredAxis].getDescriptionKey(), formattedValue, grade);
            graphics.renderTooltip(font, detail, mouseX, mouseY);
        }
    }

    private float[][] getOrComputeOuterVertices(float cx, float cy, float radius) {
        if (cachedOuterVertices != null
                && Float.compare(cachedCx, cx) == 0
                && Float.compare(cachedCy, cy) == 0
                && Float.compare(cachedRadius, radius) == 0) {
            return cachedOuterVertices;
        }
        cachedOuterVertices = GuiLineDrawing.computeHexagonVertices(cx, cy, radius);
        return cachedOuterVertices;
    }

    private float[][] getOrComputeDataVertices(float cx, float cy, float radius, @Nonnull double[] geneValues) {
        if (cachedDataVertices != null
                && cachedGeneValues != null
                && Float.compare(cachedCx, cx) == 0
                && Float.compare(cachedCy, cy) == 0
                && Float.compare(cachedRadius, radius) == 0
                && Arrays.equals(cachedGeneValues, geneValues)) {
            return cachedDataVertices;
        }

        float[][] vertices = computeDataVertices(cx, cy, radius, geneValues);
        cachedGeneValues = Arrays.copyOf(geneValues, geneValues.length);
        cachedDataVertices = vertices;
        cachedCx = cx;
        cachedCy = cy;
        cachedRadius = radius;
        return vertices;
    }

    /**
     * Computes the 6 data polygon vertices based on gene values (0.0–1.0).
     * Each vertex is positioned along its axis at a distance proportional to the gene value.
     */
    public static float[][] computeDataVertices(float cx, float cy, float radius, @Nonnull double[] geneValues) {
        float[][] vertices = new float[NUM_AXES][2];
        for (int i = 0; i < NUM_AXES; i++) {
            double value = i < geneValues.length ? Math.clamp(geneValues[i], 0.0, 1.0) : 0;
            double angle = Math.toRadians(-90 + i * 60);
            vertices[i][0] = cx + radius * (float) (value * Math.cos(angle));
            vertices[i][1] = cy + radius * (float) (value * Math.sin(angle));
        }
        return vertices;
    }

    /**
     * Determines which axis the mouse is hovering over based on angular sectors.
     * Each axis has a 60-degree sector centered on it. Returns -1 if mouse is outside the bounds.
     */
    public static int getHoveredAxis(float cx, float cy, int mouseX, int mouseY, @Nonnull Bounds bounds) {
        if (!bounds.contains(mouseX, mouseY)) {
            return -1;
        }

        float dx = mouseX - cx;
        float dy = mouseY - cy;
        float distSq = dx * dx + dy * dy;

        // Ignore if the mouse is very close to center (ambiguous sector)
        if (distSq < 16) {
            return -1;
        }

        // Compute the angle from the center, matching our vertex layout (0 = top, clockwise)
        double angle = Math.atan2(dy, dx);
        // Normalize: our axis 0 is at -90 degrees (-PI/2)
        double normalizedAngle = angle + Math.PI / 2;
        if (normalizedAngle < 0) {
            normalizedAngle += 2 * Math.PI;
        }

        // Each sector is 60 degrees (PI/3 radians)
        return (int) Math.round(normalizedAngle / (Math.PI / 3)) % NUM_AXES;
    }

    /**
     * Returns the grade letter for a gene value.
     * A: 0.80–1.0, B: 0.60–0.79, C: 0.40–0.59, D: 0.20–0.39, E: 0.00–0.19
     */
    public static String getGradeLetter(double value) {
        if (value >= GRADE_A_THRESHOLD) {
            return "A";
        } else if (value >= GRADE_B_THRESHOLD) {
            return "B";
        } else if (value >= GRADE_C_THRESHOLD) {
            return "C";
        } else if (value >= GRADE_D_THRESHOLD) {
            return "D";
        } else {
            return "E";
        }
    }

}
