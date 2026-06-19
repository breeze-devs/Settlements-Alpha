package dev.breezes.settlements.application.ai.dialogue;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provider-facing dialogue carrier that keeps text selection separate from bubble styling.
 */
public sealed interface DialogueLine permits DialogueLine.Literal, DialogueLine.Translatable {

    static DialogueLine literal(@Nonnull String text) {
        return new Literal(text);
    }

    static DialogueLine translatable(@Nonnull String key) {
        return new Translatable(key, List.of());
    }

    static DialogueLine translatable(@Nonnull String key, @Nonnull List<String> args) {
        return new Translatable(key, args);
    }

    record Literal(@Nonnull String text) implements DialogueLine {

        public Literal {
            if (text.isBlank()) {
                throw new IllegalArgumentException("text must not be blank");
            }
        }

    }

    record Translatable(@Nonnull String key, @Nonnull List<String> args) implements DialogueLine {

        public Translatable {
            if (key.isBlank()) {
                throw new IllegalArgumentException("key must not be blank");
            }
            args = List.copyOf(args);
        }

    }

}
