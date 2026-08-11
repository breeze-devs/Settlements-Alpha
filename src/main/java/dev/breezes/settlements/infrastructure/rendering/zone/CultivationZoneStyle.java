package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ArgbColorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector3f;

/**
 * The color vocabulary every Cultivation Lily surface reads.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationZoneStyle {

    private static final Vector3f RGB_VALID = new Vector3f(0.15F, 0.95F, 0.25F);
    private static final Vector3f RGB_INVALID = new Vector3f(0.95F, 0.15F, 0.15F);
    private static final Vector3f RGB_PROSPECTIVE = new Vector3f(1.0F, 1.0F, 1.0F);

    private static final int ACCENT_VALID = pack(RGB_VALID);
    private static final int ACCENT_INVALID = pack(RGB_INVALID);

    /**
     * The color a placed lily's zone outline is drawn in, as shader-ready [0,1] channels.
     */
    public static Vector3f outlineColor(boolean valid) {
        return new Vector3f(valid ? RGB_VALID : RGB_INVALID);
    }

    /**
     * The color a not-yet-placed zone's outline is drawn in — neutral.
     */
    public static Vector3f prospectiveOutlineColor() {
        return new Vector3f(RGB_PROSPECTIVE);
    }

    /**
     * The same color as {@link #outlineColor}, packed for text drawing.
     */
    public static int hudAccentColor(boolean valid) {
        return valid ? ACCENT_VALID : ACCENT_INVALID;
    }

    private static int pack(Vector3f rgb) {
        return ArgbColorUtil.pack(rgb.x(), rgb.y(), rgb.z(), 1.0F);
    }

}
