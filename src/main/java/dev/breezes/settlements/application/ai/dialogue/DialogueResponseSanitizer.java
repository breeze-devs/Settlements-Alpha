package dev.breezes.settlements.application.ai.dialogue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

/**
 * TODO:CONFIRM -- another llm interface we need to iron out
 * Sanitizes raw LLM text output before rendering it in a FLAVOR bubble.
 * <p>
 * The contract (§4) specifies that {@code choices[0].message.content} is untrusted
 * display text. This class applies the full normalization pipeline so callers never
 * need to remember the rules:
 * <ol>
 *   <li>Trim surrounding whitespace.</li>
 *   <li>Strip a single layer of wrapping quotes (straight or typographic).</li>
 *   <li>Take only the first line — collapse embedded newlines into spaces.</li>
 *   <li>Strip Minecraft formatting codes ({@code §} followed by any character).</li>
 *   <li>Clamp to {@code bubbleCharCap} characters.</li>
 *   <li>Return empty if the result is blank after all of the above.</li>
 * </ol>
 * <p>
 * The model output is <em>never</em> parsed as JSON, tool calls, or commands. This class
 * only produces display text. A dropped or garbage response changes nothing in the sim.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogueResponseSanitizer {

    private static final char CHAT_FORMATTING_CHAR = '§';

    /**
     * Sanitizes {@code raw} text coming directly from {@code choices[0].message.content}.
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

        // Strip a single layer of wrapping quotes so the model's tendency to wrap replies
        // in quotes (common with instruction-following fine-tunes) doesn't leak through.
        text = stripWrappingQuotes(text);

        // Collapse embedded newlines to spaces and take only the first logical line.
        // The stop sequence [\n] in the request should prevent most multi-line outputs,
        // but we guard here against backends that ignore the stop sequence.
        text = collapseToFirstLine(text);

        // Strip Minecraft formatting codes — they arrive as literal § + char from the model.
        // A model output of "§6Golden §rHello" becomes "Golden Hello".
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

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static String stripWrappingQuotes(String text) {
        if (text.length() < 2) {
            return text;
        }

        char first = text.charAt(0);
        char last = text.charAt(text.length() - 1);

        // Only strip when the same quote character wraps both ends.
        boolean isStraightQuotes = (first == '"' && last == '"') || (first == '\'' && last == '\'');
        // Typographic curly quotes (U+201C / U+201D for double; U+2018 / U+2019 for single).
        boolean isCurlyDoubleQuotes = (first == '“' && last == '”');
        boolean isCurlySingleQuotes = (first == '‘' && last == '’');

        if (isStraightQuotes || isCurlyDoubleQuotes || isCurlySingleQuotes) {
            return text.substring(1, text.length() - 1).trim();
        }

        return text;
    }

    private static String collapseToFirstLine(String text) {
        // Replace all newline variants with a space, then trim runs of whitespace.
        String collapsed = text.replace('\n', ' ').replace('\r', ' ');
        // Collapse repeated spaces that result from the replacement.
        collapsed = collapsed.replaceAll(" +", " ").trim();
        return collapsed;
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
