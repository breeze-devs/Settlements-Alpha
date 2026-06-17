package dev.breezes.settlements.application.ai.dialogue.llm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TODO:CONFIRM -- llm specifics
 * Parsed Chat Completions response from the inference backend.
 * <p>
 * Only {@code choices[i].message.content} is read — everything else ({@code usage},
 * {@code finish_reason}, etc.) is optional telemetry that we ignore. The content is
 * never parsed as JSON, tool calls, or commands.
 * <p>
 * Parsed with Gson, reading only the {@code choices[].message.content} path (§4 of the
 * contracts doc). Any malformed or unexpected body degrades to an empty response.
 */
public final class LlmResponse {

    /**
     * The extracted content strings from {@code choices[*].message.content},
     * in the order they appear in the response. May be empty if the response
     * is malformed or contains no choices.
     */
    private final List<String> choices;

    private LlmResponse(List<String> choices) {
        this.choices = List.copyOf(choices);
    }

    /**
     * Returns the content of the first choice, or empty if no choices were extracted.
     * This is the entry point for LIVE mode (n=1).
     */
    public Optional<String> firstContent() {
        return this.choices.isEmpty() ? Optional.empty() : Optional.of(this.choices.get(0));
    }

    /**
     * Returns all extracted choice contents. Used by the PACKS sweep (n&gt;1),
     * where each choice is one candidate line.
     */
    public List<String> allContents() {
        return this.choices;
    }

    /**
     * Extracts {@code choices[i].message.content} values from a Chat Completions JSON body,
     * in order. Only that exact path is read — never {@code tool_calls} or any other field.
     * <p>
     * If the response body is null, blank, or unparseable, an empty LlmResponse is returned
     * rather than throwing — degradation is always silent (contract §7).
     *
     * @param responseBody raw JSON string from the backend
     * @return parsed response; never null
     */
    public static LlmResponse parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new LlmResponse(List.of());
        }

        try {
            JsonElement rootElement = JsonParser.parseString(responseBody);
            if (!rootElement.isJsonObject()) {
                return new LlmResponse(List.of());
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("choices") || !root.get("choices").isJsonArray()) {
                return new LlmResponse(List.of());
            }

            List<String> extracted = new ArrayList<>();
            for (JsonElement choiceElement : root.getAsJsonArray("choices")) {
                extractContent(choiceElement).ifPresent(extracted::add);
            }
            return new LlmResponse(extracted);
        } catch (RuntimeException e) {
            // Malformed JSON degrades silently to an empty response (contract §7).
            return new LlmResponse(List.of());
        }
    }

    /**
     * Reads {@code message.content} from a single choice element, or empty when the nested
     * shape is absent. Never inspects any field other than this one content path.
     */
    private static Optional<String> extractContent(JsonElement choiceElement) {
        if (!choiceElement.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject choice = choiceElement.getAsJsonObject();
        if (!choice.has("message") || !choice.get("message").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject message = choice.getAsJsonObject("message");
        if (!message.has("content") || !message.get("content").isJsonPrimitive()) {
            return Optional.empty();
        }
        return Optional.of(message.get("content").getAsString());
    }

}
