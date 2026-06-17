package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.llm.DialogRequest;
import dev.breezes.settlements.application.ai.dialogue.llm.LlmMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:LLM TODO:CONFIRM -- this should be handled via the dialog service, not the mod
 * Assembles a {@link DialogRequest} from a {@link DialogueContext} or {@link VillagerDialogueContext}.
 * <p>
 * The system block is always assembled by the mod (contract §3): persona card, grounding seeds
 * (top 3–5 by relevance, already curated by the caller), and the fixed instruction. The model only
 * phrases what it is given; it does not decide effects.
 * <p>
 * Prompt budget: target ≤ ~512 tokens. The instruction line and persona card together are ~50
 * tokens; each grounding seed is ~20 tokens; a 5-seed grounding block is ~100 tokens. That leaves
 * ample space for the stimulus and stay well under the limit.
 */
public final class PromptAssembler {

    private static final String INSTRUCTION =
            "Reply with one short, in-character sentence. No quotation marks. Stay in character.";

    private PromptAssembler() {
        // Static utility — not instantiated.
    }

    /**
     * Builds a LIVE / single-completion request from a {@link DialogueContext}.
     *
     * @param context the structured prompt context
     * @param config  dialogue configuration (model, temperature, token cap, n)
     * @return a fully-assembled request ready to send
     */
    public static DialogRequest buildSingleRequest(DialogueContext context, DialogueConfig config) {
        String systemBlock = buildSystemBlock(context.getPersonaCard(), context.getGroundingSeeds());

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(systemBlock));

        // Stimulus (player text or situational description) is the user turn.
        // When null the model treats the instruction as its sole cue.
        if (context.getStimulus() != null && !context.getStimulus().isBlank()) {
            messages.add(LlmMessage.user(context.getStimulus()));
        }

        return DialogRequest.builder()
                .model(config.model())
                .messages(messages)
                .maxTokens(config.maxOutputTokens())
                .temperature(config.temperature())
                .n(1)
                .build();
    }

    /**
     * Builds a PACKS batch request for the evening sweep.
     * Requests {@code n} completions so the backend can batch-generate candidate lines in
     * a single round-trip. Falls back to n=1 semantics if the backend does not support n.
     *
     * @param context  the per-villager context for this sweep entry
     * @param config   dialogue configuration
     * @param packSize how many lines to request ({@link DialogueConfig#packLinesPerVillager})
     * @return a fully-assembled batch request
     */
    public static DialogRequest buildPacksRequest(VillagerDialogueContext context, DialogueConfig config, int packSize) {
        String systemBlock = buildSystemBlock(context.getPersonaCard(), context.getGroundingSeeds());

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(systemBlock));
        // No user stimulus for ambient packs; the model generates from persona + seeds alone.

        return DialogRequest.builder()
                .model(config.model())
                .messages(messages)
                .maxTokens(config.maxOutputTokens())
                .temperature(config.temperature())
                .n(packSize)
                .build();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static String buildSystemBlock(String personaCard, List<String> groundingSeeds) {
        StringBuilder sb = new StringBuilder();
        sb.append(personaCard).append("\n");

        if (!groundingSeeds.isEmpty()) {
            sb.append("Recent context:\n");
            for (String seed : groundingSeeds) {
                sb.append("- ").append(seed).append("\n");
            }
        }

        sb.append(INSTRUCTION);
        return sb.toString();
    }

}
