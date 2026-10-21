package dev.breezes.settlements.domain.generation.model.survey;

import java.util.Arrays;

public final class TerrainGrid {

    private final int originX;
    private final int originZ;
    private final int sampleInterval;
    private final TerrainSample[][] samples;
    private final int gridWidth;
    private final int gridDepth;

    public TerrainGrid(int originX, int originZ, int sampleInterval, TerrainSample[][] samples) {
        if (sampleInterval <= 0) {
            throw new IllegalArgumentException("sampleInterval must be > 0");
        }
        if (samples == null || samples.length == 0 || samples[0].length == 0) {
            throw new IllegalArgumentException("samples must be a non-empty rectangular grid");
        }

        int detectedWidth = samples[0].length;
        TerrainSample[][] copied = new TerrainSample[samples.length][];
        for (int z = 0; z < samples.length; z++) {
            if (samples[z] == null || samples[z].length != detectedWidth) {
                throw new IllegalArgumentException("samples must be a non-empty rectangular grid");
            }
            copied[z] = Arrays.copyOf(samples[z], samples[z].length);
        }

        this.originX = originX;
        this.originZ = originZ;
        this.sampleInterval = sampleInterval;
        this.samples = copied;
        this.gridWidth = detectedWidth;
        this.gridDepth = copied.length;
    }

    public static TerrainGrid of(int originX, int originZ, int sampleInterval, TerrainSample[][] samples) {
        return new TerrainGrid(originX, originZ, sampleInterval, samples);
    }

    public int worldToGridX(int worldX) {
        return clampGridX(Math.round((worldX - this.originX) / (float) this.sampleInterval));
    }

    public int worldToGridZ(int worldZ) {
        return clampGridZ(Math.round((worldZ - this.originZ) / (float) this.sampleInterval));
    }

    public TerrainSample getAtGrid(int gridX, int gridZ) {
        requireGridCoordinates(gridX, gridZ);
        return this.samples[gridZ][gridX];
    }

    public TerrainSample getAtWorld(int worldX, int worldZ) {
        return this.getAtGrid(this.worldToGridX(worldX), this.worldToGridZ(worldZ));
    }

    public int getHeightAtGrid(int gridX, int gridZ) {
        return this.getAtGrid(gridX, gridZ).height();
    }

    public int getHeightAtWorld(int worldX, int worldZ) {
        return this.getAtWorld(worldX, worldZ).height();
    }

    public int originX() {
        return this.originX;
    }

    public int originZ() {
        return this.originZ;
    }

    public int sampleInterval() {
        return this.sampleInterval;
    }

    public TerrainSample[][] samples() {
        TerrainSample[][] copy = new TerrainSample[this.samples.length][];
        for (int i = 0; i < this.samples.length; i++) {
            copy[i] = Arrays.copyOf(this.samples[i], this.samples[i].length);
        }
        return copy;
    }

    public int gridWidth() {
        return this.gridWidth;
    }

    public int gridDepth() {
        return this.gridDepth;
    }

    private void requireGridCoordinates(int gridX, int gridZ) {
        if (gridX < 0 || gridX >= this.gridWidth || gridZ < 0 || gridZ >= this.gridDepth) {
            throw new IndexOutOfBoundsException("Grid coordinates out of bounds: (" + gridX + ", " + gridZ + ")");
        }
    }

    private int clampGridX(int gridX) {
        return Math.clamp(gridX, 0, this.gridWidth - 1);
    }

    private int clampGridZ(int gridZ) {
        return Math.clamp(gridZ, 0, this.gridDepth - 1);
    }

    @Override
    public String toString() {
        return "TerrainGrid{" +
                "originX=" + this.originX +
                ", originZ=" + this.originZ +
                ", sampleInterval=" + this.sampleInterval +
                ", gridWidth=" + this.gridWidth +
                ", gridDepth=" + this.gridDepth +
                '}';
    }
}
