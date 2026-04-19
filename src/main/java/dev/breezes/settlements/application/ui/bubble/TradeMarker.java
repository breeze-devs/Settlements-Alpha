package dev.breezes.settlements.application.ui.bubble;

import lombok.Getter;
import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;

@Getter
public enum TradeMarker {

    UP("up", "▲", ChatFormatting.GOLD),
    DOWN("down", "▼", ChatFormatting.DARK_RED),
    CHECK("check", "✔", ChatFormatting.DARK_GREEN),
    CROSS("cross", "✖", ChatFormatting.RED);

    private final String serializedName;
    private final String glyph;
    private final ChatFormatting color;

    TradeMarker(String serializedName, String glyph, ChatFormatting color) {
        this.serializedName = serializedName;
        this.glyph = glyph;
        this.color = color;
    }

    public BubbleSegment.Text asSegment() {
        return BubbleSegment.Text.builder()
                .literal(this.glyph)
                .color(this.color)
                .bold(true)
                .scale(0.9F)
                .build();
    }

    public static TradeMarker fromSerializedName(@Nonnull String serializedName) {
        for (TradeMarker marker : values()) {
            if (marker.serializedName.equals(serializedName)) {
                return marker;
            }
        }

        throw new IllegalArgumentException("Unknown trade marker: " + serializedName);
    }

}
