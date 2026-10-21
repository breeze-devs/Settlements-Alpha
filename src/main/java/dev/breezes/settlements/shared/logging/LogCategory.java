package dev.breezes.settlements.shared.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * Feature family a log event belongs to, independent of its severity and of the class that emits it.
 * <p>
 * Topics and marker names are unique across categories.
 */
@AllArgsConstructor
public enum LogCategory {

    /**
     * Events from code that belongs to no other family.
     */
    GENERAL("general", null),

    /**
     * Events from running a behavior: the behavior runtime and its steps, each behavior and the helpers that serve it,
     * and behavior preconditions.
     */
    BEHAVIOR("behavior", "BEHAVIOR"),

    /**
     * Events from deciding what a villager does next: building and running the day plan, arbitrating and running
     * reactive overrides, and the behavior catalog.
     */
    PLANNING("planning", "PLANNING"),

    /**
     * Events from sensing the world for villagers: the sensors and the world indexes they read.
     */
    SENSOR("sensor", "SENSOR"),

    /**
     * Events from talking to the optional Settlements Inference Service (SIS), and from selecting and supplying
     * villager dialogue.
     */
    INFERENCE("inference", "INFERENCE"),

    /**
     * Events from a settlement's lifecycle: founding, recording generated settlements, membership, naming, and
     * disbanding, and the queries and commands over settlements.
     */
    SETTLEMENT("settlement", "SETTLEMENT"),

    /**
     * Events from generating settlements in the world: the structure, its layout and placement, its generated history,
     * and its villagers.
     */
    WORLDGEN("worldgen", "WORLDGEN"),

    /**
     * Events from loading, validating, and looking up datapack data, structure templates included.
     */
    RESOURCE_LOADING("resource_loading", "RESOURCE_LOADING");

    /**
     * Identifier a class gives as the topic of @CustomLog to log in this category, in lowercase snake case.
     */
    @Getter
    private final String topic;

    /**
     * Must not name a marker the host's Log4j configuration filters on, such as NeoForge's LOADING or REGISTRIES.
     * That filter accepts or denies a marked event before any appender's level applies.
     */
    @Nullable
    private final String markerName;

    /**
     * Returns the name of the Log4j marker this category's events carry into normal logs, where the host's layouts
     * print it beside the logger name, or empty when they carry none.
     */
    public Optional<String> getMarkerName() {
        return Optional.ofNullable(this.markerName);
    }

    /**
     * Returns the category whose topic is given, or empty when no category has it.
     */
    public static Optional<LogCategory> forTopic(@Nonnull String topic) {
        return Arrays.stream(values())
                .filter(category -> category.topic.equals(topic))
                .findFirst();
    }

}
