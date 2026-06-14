package dev.breezes.settlements.domain.animation;

import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.List;

public final class InMemoryIdleLifeAnimationLibrary implements IdleLifeAnimationLibrary {

    private final KeyframeAnimation baseIdle;
    private final KeyframeAnimation blink;
    private final List<KeyframeAnimation> fidgets;

    @Builder
    private InMemoryIdleLifeAnimationLibrary(@Nonnull KeyframeAnimation baseIdle,
                                             @Nonnull KeyframeAnimation blink,
                                             @Nonnull List<KeyframeAnimation> fidgets) {
        this.baseIdle = baseIdle;
        this.blink = blink;
        this.fidgets = List.copyOf(fidgets);
    }

    @Override
    public KeyframeAnimation baseIdle() {
        return this.baseIdle;
    }

    @Override
    public KeyframeAnimation blink() {
        return this.blink;
    }

    @Override
    public List<KeyframeAnimation> fidgets() {
        return this.fidgets;
    }

}
