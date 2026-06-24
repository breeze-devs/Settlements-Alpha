package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Code-defined SCRIPTED catalog
 * <p>
 * The fallback order keeps sparse authored pools useful: a profession only needs lines for
 * distinctive situations while generic pools cover the rest.
 */
public final class DialogueLineIndex {

    public static final VillagerProfessionKey GENERIC = VillagerProfessionKey.of("generic");

    private final Map<VillagerProfessionKey, Map<Occasion, List<String>>> keysByProfession;
    private final Map<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> keysByProfessionOccasionFacet;

    public DialogueLineIndex() {
        this(defaultKeys());
    }

    public DialogueLineIndex(@Nonnull Map<VillagerProfessionKey, Map<Occasion, List<String>>> keysByProfession) {
        this(keysByProfession, defaultFacetKeys());
    }

    public DialogueLineIndex(@Nonnull Map<VillagerProfessionKey, Map<Occasion, List<String>>> keysByProfession,
                             @Nonnull Map<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> keysByProfessionOccasionFacet) {
        Map<VillagerProfessionKey, Map<Occasion, List<String>>> copied = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, Map<Occasion, List<String>>> entry : keysByProfession.entrySet()) {
            Map<Occasion, List<String>> occasionMap = new EnumMap<>(Occasion.class);
            for (Map.Entry<Occasion, List<String>> occasionEntry : entry.getValue().entrySet()) {
                occasionMap.put(occasionEntry.getKey(), List.copyOf(occasionEntry.getValue()));
            }
            copied.put(entry.getKey(), Map.copyOf(occasionMap));
        }
        this.keysByProfession = Map.copyOf(copied);

        Map<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> copiedFacetKeys = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> entry : keysByProfessionOccasionFacet.entrySet()) {
            Map<Occasion, Map<DialogueFacet, List<String>>> occasionMap = new EnumMap<>(Occasion.class);
            for (Map.Entry<Occasion, Map<DialogueFacet, List<String>>> occasionEntry : entry.getValue().entrySet()) {
                Map<DialogueFacet, List<String>> facetMap = new EnumMap<>(DialogueFacet.class);
                for (Map.Entry<DialogueFacet, List<String>> facetEntry : occasionEntry.getValue().entrySet()) {
                    facetMap.put(facetEntry.getKey(), List.copyOf(facetEntry.getValue()));
                }
                occasionMap.put(occasionEntry.getKey(), Map.copyOf(facetMap));
            }
            copiedFacetKeys.put(entry.getKey(), Map.copyOf(occasionMap));
        }
        this.keysByProfessionOccasionFacet = Map.copyOf(copiedFacetKeys);
    }

    public Optional<List<String>> resolveKeys(@Nonnull VillagerProfessionKey profession, @Nonnull Occasion occasion) {
        return firstNonEmpty(
                keysFor(profession, occasion),
                keysFor(GENERIC, occasion),
                keysFor(profession, Occasion.IDLE),
                keysFor(GENERIC, Occasion.IDLE));
    }

    public Optional<List<String>> resolveKeys(@Nonnull VillagerProfessionKey profession,
                                              @Nonnull Occasion occasion,
                                              @Nonnull Set<DialogueFacet> facets) {
        Optional<List<String>> basePool = this.resolveKeys(profession, occasion);

        // Short-circuit: no facets mean identical behavior to the 2-arg method.
        if (facets.isEmpty()) {
            return basePool;
        }

        // Collect facet extras AT the requested occasion only — never fall back to IDLE here,
        // because collapsing to IDLE would surface "cured-idle" lines during, e.g., MORNING,
        // which is the occasion-bleed bug we are guarding against.
        List<String> facetExtras = facets.stream()
                .flatMap(facet -> firstNonEmpty(
                        keysFor(profession, occasion, facet),
                        keysFor(GENERIC, occasion, facet))
                        .stream()
                        .flatMap(List::stream))
                .toList();

        if (basePool.isEmpty() && facetExtras.isEmpty()) {
            return Optional.empty();
        }

        List<String> base = basePool.orElse(List.of());
        List<String> combined = new ArrayList<>(base.size() + facetExtras.size());
        combined.addAll(base);
        combined.addAll(facetExtras);
        return Optional.of(List.copyOf(combined));
    }

    private List<String> keysFor(VillagerProfessionKey profession, Occasion occasion) {
        return this.keysByProfession.getOrDefault(profession, Map.of()).getOrDefault(occasion, List.of());
    }

    private List<String> keysFor(VillagerProfessionKey profession, Occasion occasion, DialogueFacet facet) {
        return this.keysByProfessionOccasionFacet.getOrDefault(profession, Map.of())
                .getOrDefault(occasion, Map.of())
                .getOrDefault(facet, List.of());
    }

    @SafeVarargs
    private static Optional<List<String>> firstNonEmpty(List<String>... candidates) {
        for (List<String> candidate : candidates) {
            if (!candidate.isEmpty()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Map<VillagerProfessionKey, Map<Occasion, List<String>>> defaultKeys() {
        Map<VillagerProfessionKey, Map<Occasion, List<String>>> result = new LinkedHashMap<>();
        result.put(GENERIC, Map.of(
                Occasion.IDLE, numberedKeys("dialogue.settlements.generic.idle", 10),
                Occasion.WORK, numberedKeys("dialogue.settlements.generic.work", 10),
                Occasion.PANIC, numberedKeys("dialogue.settlements.generic.panic", 10),
                Occasion.MEET, numberedKeys("dialogue.settlements.generic.meet", 10),
                Occasion.MORNING, numberedKeys("dialogue.settlements.generic.morning", 10),
                Occasion.EVENING, numberedKeys("dialogue.settlements.generic.evening", 10),
                Occasion.REST_DAY, numberedKeys("dialogue.settlements.generic.rest_day", 10),
                Occasion.ZOMBIE_SIGHTED, numberedKeys("dialogue.settlements.generic.zombie_sighted", 3)));
        result.put(VillagerProfessionKey.FARMER, Map.of(
                Occasion.IDLE, numberedKeys("dialogue.settlements.farmer.idle", 10),
                Occasion.WORK, numberedKeys("dialogue.settlements.farmer.work", 10),
                Occasion.REST_DAY, numberedKeys("dialogue.settlements.farmer.rest_day", 5),
                Occasion.ZOMBIE_SIGHTED, numberedKeys("dialogue.settlements.farmer.zombie_sighted", 8)));
        result.put(VillagerProfessionKey.NITWIT, Map.of(
                Occasion.IDLE, numberedKeys("dialogue.settlements.nitwit.idle", 10)));
        return result;
    }

    private static Map<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> defaultFacetKeys() {
        Map<VillagerProfessionKey, Map<Occasion, Map<DialogueFacet, List<String>>>> result = new LinkedHashMap<>();
        result.put(GENERIC, Map.of(
                Occasion.IDLE, Map.of(DialogueFacet.WAS_CURED,
                        numberedKeys("dialogue.settlements.generic.idle.cured", 6)),
                Occasion.REST_DAY, Map.of(DialogueFacet.WAS_CURED,
                        numberedKeys("dialogue.settlements.generic.rest_day.cured", 3))));
        return result;
    }

    private static List<String> numberedKeys(String prefix, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> prefix + "." + index)
                .toList();
    }

}
