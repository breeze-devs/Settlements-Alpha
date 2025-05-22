package dev.breezes.settlements.models.behaviors.states.registry;

import dev.breezes.settlements.models.behaviors.states.BehaviorState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SpeechBubbleState implements BehaviorState {

    @Nullable
    private UUID bubbleId;

    public static SpeechBubbleState of(@Nonnull UUID bubbleId) {
        return new SpeechBubbleState(bubbleId);
    }

    public static SpeechBubbleState empty() {
        return new SpeechBubbleState(null);
    }

    @Override
    public void reset() {
        this.bubbleId = null;
    }

}
