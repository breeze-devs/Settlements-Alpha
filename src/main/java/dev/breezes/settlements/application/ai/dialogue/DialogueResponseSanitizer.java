package dev.breezes.settlements.application.ai.dialogue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

/**
 * Defensive cleanup for generated literal text before rendering it in a FLAVOR bubble.
 * <p>
 * The inference service owns primary sanitization, but the client still guards the final render path:
 * <ol>
 *   <li>Trim surrounding whitespace.</li>
 *   <li>Strip Minecraft formatting codes ({@code §} followed by any character).</li>
 *   <li>Clamp to {@code bubbleCharCap} characters.</li>
 * </ol>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogueResponseSanitizer {

    private static final char CHAT_FORMATTING_CHAR = '§';

    /**
     * Applies a final render-side guard to generated literal text.
     *
     * @param raw           the raw model output string
     * @param bubbleCharCap maximum character length for the rendered bubble text
     * @return sanitized displayable text, or empty if the result is blank
     */
    public static Optional<String> sanitize(String raw, int bubbleCharCap) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String text = raw.trim();

        text = stripMinecraftFormattingCodes(text);

        text = text.trim();

        if (text.isBlank()) {
            return Optional.empty();
        }

        // Clamp to the configured bubble character cap.
        if (text.length() > bubbleCharCap) {
            text = text.substring(0, bubbleCharCap).trim();
        }

        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private static String stripMinecraftFormattingCodes(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == CHAT_FORMATTING_CHAR && i + 1 < text.length()) {
                // Skip the § and the following code character.
                i += 2;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

}
