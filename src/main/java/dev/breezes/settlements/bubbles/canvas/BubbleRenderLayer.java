package dev.breezes.settlements.bubbles.canvas;

import lombok.Getter;

@Getter
public enum BubbleRenderLayer {

    LOWEST(-100),

    BACKGROUND_LOW(-15),
    BACKGROUND_NORMAL(-10),
    BACKGROUND_HIGH(-5),

    NORMAL(0),

    FOREGROUND_LOW(5),
    FOREGROUND_NORMAL(10),
    FOREGROUND_HIGH(15),

    HIGHEST(100);

    BubbleRenderLayer(int zIndex) {
        this.zIndex = (float) zIndex / 100;
    }

    /**
     * The Z-index of the layer, where higher z-index layers are rendered on top of lower z-index layers
     */
    private final float zIndex;


}
