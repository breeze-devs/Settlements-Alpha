package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * One named scalar a developer moves while the game is running, carrying its own adjustment step and its
 * own default value.
 */
@ClientSide
@Getter
public final class DebugTuningKnob {

    private final String name;
    private final float defaultValue;
    private final float step;

    private float value;

    private DebugTuningKnob(@Nonnull String name, float defaultValue, float step) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.step = step;
        this.value = defaultValue;
    }

    public static DebugTuningKnob of(@Nonnull String name, float defaultValue, float step) {
        return new DebugTuningKnob(name, defaultValue, step);
    }

    public void nudge(int steps) {
        this.value += steps * this.step;
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    /**
     * Whether this knob has been moved off the value it was declared with.
     */
    public boolean isMoved() {
        return Float.compare(this.value, this.defaultValue) != 0;
    }

}
