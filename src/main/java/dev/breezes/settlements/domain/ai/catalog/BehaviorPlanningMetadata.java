package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.GameTicks;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Collections;
import java.util.Set;

/**
 * Metadata that describes a behavior to the planning system.
 * <p>
 * Paired with a factory in the behavior catalog: the planning system queries descriptors
 * to filter and rank behaviors for inclusion in a {@link dev.breezes.settlements.domain.ai.planning.DayPlan};
 * the LLM planner serializes them via {@link #toPromptLine()} for the prompt's available-actions section.
 * <p>
 * This class is profession-agnostic. Which professions can use a behavior is declared in
 * {@link ProfessionBehaviorPool}, not here. The catalog is a library of capabilities;
 * the pool is the job description that references them.
 * <p>
 * Behavior {@link #key}s must be stable — they are persisted inside serialized plans and
 * renaming a key will silently break existing saved plans.
 */
@Builder
@Getter
public class BehaviorPlanningMetadata {

    private final BehaviorKey key;
    private final String displayName;
    private final String description;
    private final BehaviorCategory category;
    private final WorkIntensity intensity;

    @Singular
    private final Set<BehaviorChannel> requiredChannels;

    @Singular
    private final Set<String> relevantMemoryHints;

    @Singular
    private final Set<String> producedObservationHints;

    private final GameTicks estimatedDuration;

    /**
     * Per-behavior ceiling on real elapsed run time. When the plan runner's behavior-wide
     * safety net reaches this duration the behavior is force-stopped and the slot skipped,
     * so the villager's remaining day can continue. Expressed in {@link ClockTicks} (real
     * elapsed time) rather than {@link GameTicks} (in-game time) because the ceiling is
     * anchored to wall-clock feel, not to how fast the Minecraft day advances.
     * <p>
     * Long-running behaviors (e.g. smelting, enchanting, fishing) should override this
     * with a generous value; the default of one minute is appropriate for navigation and
     * interaction behaviors.
     */
    @Builder.Default
    private final ClockTicks maxRunDuration = ClockTicks.minutes(1);

    private final String preconditionSummary;
    private final boolean interruptible;

    @Builder.Default
    private final CooldownRange cooldown = CooldownRange.ofSeconds(1, 1);

    /**
     * Returns {@code true} if this behavior and {@code other} could run concurrently —
     * i.e. their required {@link BehaviorChannel} sets are disjoint.
     */
    public boolean isChannelCompatibleWith(BehaviorPlanningMetadata other) {
        return Collections.disjoint(this.requiredChannels, other.requiredChannels);
    }

    /**
     * TODO: [agent] tune this
     * Formats this descriptor as a single-line string for the LLM planner prompt's
     * available-actions section.
     * <p>
     * Format: {@code [INTENSITY_TAG] key — description (~N minutes)}
     */
    public String toPromptLine() {
        return "[%s] %s — %s (~%d minutes)"
                .formatted(this.intensityTag(), this.key.id(), this.description, this.estimatedDuration.getAsGameMinutes());
    }

    private String intensityTag() {
        return switch (this.intensity) {
            case HEAVY -> "HEAVY WORK";
            case LIGHT -> "LIGHT WORK";
            case NONE -> this.category.name().replace('_', ' ');
        };
    }

}
