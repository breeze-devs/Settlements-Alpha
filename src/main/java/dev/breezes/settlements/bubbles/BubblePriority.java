package dev.breezes.settlements.bubbles;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BubblePriority {

    LOWEST(-1000),
    LOWER(-100),
    LOW(-10),
    DEFAULT(0),
    HIGH(10),
    HIGHER(100),
    HIGHEST(1000);

    private final int priority;

}
