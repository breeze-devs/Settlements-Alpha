package dev.breezes.settlements.infrastructure.minecraft.items;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ArgbColorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Villager outline colors for the villager totem, derived from {@link TotemMode#hue()}.
 * <p>
 * Every color here is resolved at class-init, into a packed 0xAARRGGBB int: {@link TotemMode#hue()}
 * allocates a fresh {@link Vector3f} on every call, so resolving it once here means no caller ever pays that
 * cost more than once per game session, let alone per frame.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TotemModeStyle {

    /**
     * The aimed-at villager's outline, owned by no mode: mode identity is already spoken by every other lit
     * villager on screen, so answering "which one will this click hit" with a mode hue would ask one channel
     * two questions at once.
     */
    private static final int HOVER_OUTLINE_COLOR = ArgbColorUtil.pack(0.95F, 0.95F, 0.95F, 1.0F);

    private static final Map<TotemMode, Integer> MODE_COLORS = Arrays.stream(TotemMode.values())
            .collect(Collectors.toUnmodifiableMap(mode -> mode, TotemModeStyle::packHue));

    public static int restingOutlineColor(@Nonnull TotemMode mode) {
        return MODE_COLORS.get(mode);
    }

    public static int hoverOutlineColor() {
        return HOVER_OUTLINE_COLOR;
    }

    private static int packHue(@Nonnull TotemMode mode) {
        Vector3f hue = mode.hue();
        return ArgbColorUtil.pack(hue.x(), hue.y(), hue.z(), 1.0F);
    }

}
