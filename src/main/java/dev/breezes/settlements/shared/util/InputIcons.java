package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * The gesture glyphs player-facing hints are written in. Each is one character in a private font, so a hint
 * stays a single {@link Component} and inherits centering, wrapping, fade alpha, and tooltip layout from the
 * normal text path rather than needing any of them rebuilt.
 * <p>
 * Methods are named for the <em>action</em> while the glyph depicts a <em>key</em>, and that gap is deliberate:
 * Use and Sneak are rebindable, so an icon can name a button the player does not have. Depicting the default
 * binding regardless is an accepted trade — near-universal bindings against a hint that reads like a game. This
 * class is the seam that keeps it cheap to revisit: swapping in {@code Component.keybind} for a non-default
 * binding changes these method bodies and nothing else.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputIcons {

    private static final ResourceLocation FONT = ResourceLocationUtil.mod("input_icons");

    private static final String RIGHT_CLICK_GLYPH = "\uE000";
    private static final String SNEAK_GLYPH = "\uE001";

    public static Component rightClick() {
        return glyph(RIGHT_CLICK_GLYPH);
    }

    public static Component sneak() {
        return glyph(SNEAK_GLYPH);
    }

    /**
     * Forced white because a glyph is multiplied by the line's text color, which would otherwise drain the
     * authored art toward whatever gray the surrounding hint is drawn in.
     * Bold is cleared because bold text draws twice, and these appear in a headline that sets it.
     */
    private static Component glyph(@Nonnull String codepoint) {
        return Component.literal(codepoint)
                .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE).withBold(false));
    }

}
