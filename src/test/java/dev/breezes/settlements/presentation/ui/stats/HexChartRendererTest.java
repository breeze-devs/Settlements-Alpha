package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.presentation.ui.framework.Bounds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HexChartRendererTest {

    @ParameterizedTest
    @CsvSource({
            "1.0, A",
            "0.80, A",
            "0.99, A",
            "0.79, B",
            "0.60, B",
            "0.70, B",
            "0.59, C",
            "0.40, C",
            "0.50, C",
            "0.39, D",
            "0.20, D",
            "0.30, D",
            "0.19, E",
            "0.0, E",
            "0.01, E"
    })
    void getGradeLetter_returnsCorrectGrade(double value, String expectedGrade) {
        Assertions.assertEquals(expectedGrade, HexChartRenderer.getGradeLetter(value));
    }

    @Test
    void computeDataVertices_allOnes_verticesAtFullRadius() {
        float cx = 100, cy = 100, radius = 50;
        double[] genes = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        float[][] vertices = HexChartRenderer.computeDataVertices(cx, cy, radius, genes);

        Assertions.assertEquals(6, vertices.length);
        // Vertex 0 (top, angle -90°) should be at (cx, cy - radius)
        Assertions.assertEquals(cx, vertices[0][0], 0.5F);
        Assertions.assertEquals(cy - radius, vertices[0][1], 0.5F);
        // Vertex 3 (bottom, angle 90°) should be at (cx, cy + radius)
        Assertions.assertEquals(cx, vertices[3][0], 0.5F);
        Assertions.assertEquals(cy + radius, vertices[3][1], 0.5F);
    }

    @Test
    void computeDataVertices_allZeros_verticesAtCenter() {
        float cx = 100, cy = 100, radius = 50;
        double[] genes = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        float[][] vertices = HexChartRenderer.computeDataVertices(cx, cy, radius, genes);

        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(cx, vertices[i][0], 0.01F, "Vertex " + i + " x should be at center");
            Assertions.assertEquals(cy, vertices[i][1], 0.01F, "Vertex " + i + " y should be at center");
        }
    }

    @Test
    void computeDataVertices_halfValues_verticesAtHalfRadius() {
        float cx = 200, cy = 200, radius = 80;
        double[] genes = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        float[][] vertices = HexChartRenderer.computeDataVertices(cx, cy, radius, genes);

        // Vertex 0 (top) at half radius
        Assertions.assertEquals(cx, vertices[0][0], 0.5F);
        Assertions.assertEquals(cy - radius * 0.5F, vertices[0][1], 0.5F);
    }

    @Test
    void computeDataVertices_clampsAboveOne() {
        float cx = 100, cy = 100, radius = 50;
        double[] genes = {1.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        float[][] vertices = HexChartRenderer.computeDataVertices(cx, cy, radius, genes);

        // Should clamp to 1.0, so vertex 0 at full radius
        Assertions.assertEquals(cx, vertices[0][0], 0.5F);
        Assertions.assertEquals(cy - radius, vertices[0][1], 0.5F);
    }

    @Test
    void computeDataVertices_clampsBelowZero() {
        float cx = 100, cy = 100, radius = 50;
        double[] genes = {-0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        float[][] vertices = HexChartRenderer.computeDataVertices(cx, cy, radius, genes);

        // Should clamp to 0.0, so vertex 0 at center
        Assertions.assertEquals(cx, vertices[0][0], 0.5F);
        Assertions.assertEquals(cy, vertices[0][1], 0.5F);
    }

    @Test
    void getHoveredAxis_outsideBounds_returnsNegativeOne() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        int result = HexChartRenderer.getHoveredAxis(100, 100, 200, 200, bounds);
        Assertions.assertEquals(-1, result);
    }

    @Test
    void getHoveredAxis_veryCloseToCenter_returnsNegativeOne() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        // Mouse at center (100, 100), distance < 4
        int result = HexChartRenderer.getHoveredAxis(100, 100, 101, 101, bounds);
        Assertions.assertEquals(-1, result);
    }

    @Test
    void getHoveredAxis_directlyAboveCenter_returnsAxis0() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        // Mouse directly above center
        int result = HexChartRenderer.getHoveredAxis(100, 100, 100, 70, bounds);
        Assertions.assertEquals(0, result);
    }

    @Test
    void getHoveredAxis_directlyBelowCenter_returnsAxis3() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        // Mouse directly below center
        int result = HexChartRenderer.getHoveredAxis(100, 100, 100, 130, bounds);
        Assertions.assertEquals(3, result);
    }

    @Test
    void getHoveredAxis_topRight_returnsAxis1() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        // Mouse to top-right (approximately 60° from top = axis 1)
        int result = HexChartRenderer.getHoveredAxis(100, 100, 130, 80, bounds);
        Assertions.assertEquals(1, result);
    }

    @Test
    void getHoveredAxis_bottomLeft_returnsAxis4() {
        Bounds bounds = new Bounds(50, 50, 100, 100);
        // Mouse to bottom-left (approximately 240° from top = axis 4)
        int result = HexChartRenderer.getHoveredAxis(100, 100, 70, 120, bounds);
        Assertions.assertEquals(4, result);
    }

}
