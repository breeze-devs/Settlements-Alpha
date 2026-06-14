package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ArmConfigurationTimeline {

    public static final ArmConfigurationTimeline EMPTY = new ArmConfigurationTimeline(List.of());

    private final List<ArmConfigurationKeyframe> keyframes;

    private ArmConfigurationTimeline(@Nonnull List<ArmConfigurationKeyframe> keyframes) {
        List<ArmConfigurationKeyframe> sortedKeyframes = keyframes.stream()
                .sorted(Comparator.comparingInt(ArmConfigurationKeyframe::tick))
                .toList();
        validateUniqueTicks(sortedKeyframes);
        this.keyframes = sortedKeyframes;
    }

    public static ArmConfigurationTimeline of(@Nonnull List<ArmConfigurationKeyframe> keyframes) {
        if (keyframes.isEmpty()) {
            return EMPTY;
        }
        return new ArmConfigurationTimeline(keyframes);
    }

    public Optional<ArmConfiguration> sample(float animationTick) {
        if (this.keyframes.isEmpty()) {
            return Optional.empty();
        }

        ArmConfiguration active = this.keyframes.getFirst().configuration();
        for (ArmConfigurationKeyframe keyframe : this.keyframes) {
            if (animationTick < keyframe.tick()) {
                return Optional.of(active);
            }
            active = keyframe.configuration();
        }
        return Optional.of(active);
    }

    public boolean isEmpty() {
        return this.keyframes.isEmpty();
    }

    public List<ArmConfigurationKeyframe> keyframes() {
        return this.keyframes;
    }

    private static void validateUniqueTicks(@Nonnull List<ArmConfigurationKeyframe> keyframes) {
        int previousTick = -1;
        for (ArmConfigurationKeyframe keyframe : keyframes) {
            if (keyframe.tick() == previousTick) {
                throw new IllegalArgumentException("Arm configuration keyframe ticks must be unique");
            }
            previousTick = keyframe.tick();
        }
    }

}
