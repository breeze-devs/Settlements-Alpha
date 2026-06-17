package dev.breezes.settlements.application.ai.dialogue.llm;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * TODO:CONFIRM -- another part of the LLM artifact we might want to re-determine the contract
 * Wire-level Chat Completions request sent to the inference backend.
 * <p>
 * Shapes the POST body described in llm-contracts.md §3. Stream is always {@code false}
 * in v1. The system block is pre-assembled by the caller — the model only phrases
 * what it receives; it does not decide effects.
 */
@Builder
@Getter
public final class DialogRequest {

    private static final Gson GSON = new Gson();

    private final String model;

    @Singular
    private final List<LlmMessage> messages;

    @SerializedName("max_tokens")
    private final int maxTokens;

    private final double temperature;

    private final int n;

    @Builder.Default
    private final List<String> stop = List.of("\n");

    @Builder.Default
    private final boolean stream = false;

    /**
     * Serializes this request to the Chat Completions POST body. Gson handles all string
     * escaping (including control characters), so player-authored content cannot produce
     * malformed JSON.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

}
