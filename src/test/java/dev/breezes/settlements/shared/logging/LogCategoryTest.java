package dev.breezes.settlements.shared.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LogCategoryTest {

    // The markers NeoForge's log4j2.xml filters at the configuration level, ahead of every appender.
    private static final Set<String> HOST_FILTERED_MARKERS = Set.of(
            "NETWORK_PACKETS", "CLASSLOADING", "LAUNCHPLUGIN", "CLASSDUMP", "AXFORM", "EVENTBUS", "DISTXFORM", "SCAN",
            "REGISTRIES", "REGISTRYDUMP", "SPLASH", "RESOURCE-CACHE", "FORGEMOD", "LOADING", "CORE");

    @ParameterizedTest
    @EnumSource(LogCategory.class)
    void forTopic_resolvesEachCategoryFromItsOwnTopic(LogCategory category) {
        // Counterexample: a copied constant keeps its source's topic, so classes naming it log in the other category.
        assertEquals(Optional.of(category), LogCategory.forTopic(category.getTopic()));
    }

    @ParameterizedTest
    @EnumSource(LogCategory.class)
    void markerName_isNoneTheHostFilters(LogCategory category) {
        // Counterexample: a category marked LOADING is accepted or denied by the host's filter before appender levels apply.
        category.getMarkerName().ifPresent(markerName ->
                assertFalse(HOST_FILTERED_MARKERS.contains(markerName), markerName));
    }

    @Test
    void markerNames_areDistinct() {
        // Counterexample: a copied constant keeps its source's marker, so two categories look the same in normal logs.
        List<String> markerNames = Arrays.stream(LogCategory.values())
                .flatMap(category -> category.getMarkerName().stream())
                .toList();

        assertEquals(markerNames.size(), Set.copyOf(markerNames).size(), markerNames.toString());
    }

}
