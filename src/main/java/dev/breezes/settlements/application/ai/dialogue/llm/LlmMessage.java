package dev.breezes.settlements.application.ai.dialogue.llm;

import lombok.Value;

/**
 * TODO:CONFIRM -- llm specifics
 * A single Chat Completions message (role + content).
 */
@Value
public class LlmMessage {

    String role;
    String content;

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

}
