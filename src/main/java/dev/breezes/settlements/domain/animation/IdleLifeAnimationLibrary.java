package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;
import java.util.List;

public interface IdleLifeAnimationLibrary {

    KeyframeAnimation baseIdle();

    KeyframeAnimation blink();

    List<KeyframeAnimation> fidgets();

}
