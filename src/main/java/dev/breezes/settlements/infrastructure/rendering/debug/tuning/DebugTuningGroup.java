package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Knobs that one consumer reads together, named after that consumer.
 */
@ClientSide
@Getter
public final class DebugTuningGroup {

    private final String name;
    private final List<DebugTuningKnob> knobs;

    private DebugTuningGroup(@Nonnull String name, @Nonnull List<DebugTuningKnob> knobs) {
        this.name = name;
        this.knobs = List.copyOf(knobs);
    }

    public static DebugTuningGroup of(@Nonnull String name, @Nonnull DebugTuningKnob... knobs) {
        return new DebugTuningGroup(name, List.of(knobs));
    }

}
