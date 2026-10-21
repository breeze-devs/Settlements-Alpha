package dev.breezes.settlements.domain.animation;

import java.util.List;

public interface IdleLifeAnimationLibrary {

    KeyframeAnimation baseIdle();

    KeyframeAnimation blink();

    List<KeyframeAnimation> fidgets();

}
