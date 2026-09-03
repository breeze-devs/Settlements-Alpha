package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every knob currently open for tuning, plus the one cursor the keybinds act on.
 * <p>
 * Selection is a single index across all groups because the overlay paints every group at once.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DebugTuningBoard {

    private static final String VALUE_FORMAT = "%.4f";

    private final List<DebugTuningGroup> groups = new ArrayList<>();

    @Getter
    private boolean visible;

    private int selectedIndex;

    public void register(@Nonnull DebugTuningGroup group) {
        this.groups.add(group);
    }

    public List<DebugTuningGroup> groups() {
        return List.copyOf(this.groups);
    }

    public void toggleVisible() {
        this.visible = !this.visible;
    }

    /**
     * Walks the cursor by whole rows, wrapping at both ends so holding one direction reaches everything
     * without the reader having to notice which end they started from.
     */
    public void moveSelection(int rows) {
        int knobCount = this.knobCount();
        if (knobCount == 0) {
            return;
        }
        this.selectedIndex = Math.floorMod(this.selectedIndex + rows, knobCount);
    }

    public Optional<DebugTuningKnob> selectedKnob() {
        List<DebugTuningKnob> knobs = this.allKnobs();
        if (knobs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(knobs.get(this.selectedIndex));
    }

    /**
     * Whether the given knob is the one the cursor is on.
     */
    public boolean isSelected(@Nonnull DebugTuningKnob knob) {
        return this.selectedKnob().map(selected -> selected == knob).orElse(false);
    }

    public void nudgeSelected(int steps) {
        this.selectedKnob().ifPresent(knob -> knob.nudge(steps));
    }

    public void resetSelected() {
        this.selectedKnob().ifPresent(DebugTuningKnob::reset);
    }

    public void resetAll() {
        this.allKnobs().forEach(DebugTuningKnob::reset);
    }

    /**
     * The whole board as transcribable text, one knob per line under its group's heading.
     */
    public List<String> snapshotLines() {
        List<String> lines = new ArrayList<>();
        for (DebugTuningGroup group : this.groups) {
            lines.add("[" + group.getName() + "]");
            for (DebugTuningKnob knob : group.getKnobs()) {
                lines.add("  " + knob.getName() + " = " + format(knob.getValue()));
            }
        }
        return lines;
    }

    public static String format(float value) {
        return String.format(Locale.ROOT, VALUE_FORMAT, value);
    }

    private int knobCount() {
        return this.groups.stream().mapToInt(group -> group.getKnobs().size()).sum();
    }

    private List<DebugTuningKnob> allKnobs() {
        return this.groups.stream()
                .flatMap(group -> group.getKnobs().stream())
                .toList();
    }

}
