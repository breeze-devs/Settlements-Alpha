package dev.breezes.settlements.logging.levels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LogLevel {

    TRACE("Trace"),
    DEBUG("Debug"),
    INFO("Info"),
    WARN("Warn"),
    ERROR("Error");

    private final String prefix;

}
