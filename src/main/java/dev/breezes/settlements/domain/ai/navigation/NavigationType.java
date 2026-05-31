package dev.breezes.settlements.domain.ai.navigation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NavigationType {

    STROLL(0.4F),
    WALK(0.5F),
    RUN(0.7F),
    SPRINT(0.9F),
    ;

    private final float baseModifier;

}
