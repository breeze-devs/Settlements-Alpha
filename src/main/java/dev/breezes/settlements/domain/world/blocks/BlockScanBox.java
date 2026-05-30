package dev.breezes.settlements.domain.world.blocks;

public record BlockScanBox(int horizontalRadius, int verticalRadius) {

    public BlockScanBox {
        if (horizontalRadius < 0) {
            throw new IllegalArgumentException("Horizontal radius cannot be negative");
        }
        if (verticalRadius < 0) {
            throw new IllegalArgumentException("Vertical radius cannot be negative");
        }
    }

    public static BlockScanBox self() {
        return new BlockScanBox(0, 0);
    }

    public static BlockScanBox confirm() {
        return new BlockScanBox(2, 1);
    }

}
