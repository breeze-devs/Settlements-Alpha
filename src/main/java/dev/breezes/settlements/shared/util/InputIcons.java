package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Input icons that participate in normal text layout through a custom private font.
 * Icons depict the default controls and do not follow key re-bindings.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputIcons {

    private static final ResourceLocation FONT = ResourceLocationUtil.mod("input_icons");

    private static final String RIGHT_CLICK_GLYPH = "\uE000";
    private static final String SNEAK_GLYPH = "\uE001";
    private static final String SCROLL_GLYPH = "\uE002";

    public static Component rightClick() {
        return glyph(RIGHT_CLICK_GLYPH);
    }

    public static Component sneak() {
        return glyph(SNEAK_GLYPH);
    }

    public static Component scroll() {
        return glyph(SCROLL_GLYPH);
    }

    private static Component glyph(@Nonnull String codepoint) {
        // Forced white to preserve the artwork's colors and weight when surrounding text is tinted or bold
        return Component.literal(codepoint)
                .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE).withBold(false));
    }

}
