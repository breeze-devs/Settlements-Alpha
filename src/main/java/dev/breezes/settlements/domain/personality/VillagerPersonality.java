package dev.breezes.settlements.domain.personality;

import java.util.List;

/**
 * The persisted "persona card" for a villager: adjectives, a short characterSketch, and an optional speech style,
 * plus the {@link PersonalityStatus} lifecycle marker.
 */
public record VillagerPersonality(PersonalityStatus status, List<String> adjectives, String characterSketch, String speechStyle) {

    public VillagerPersonality {
        if (status == null) {
            status = PersonalityStatus.PENDING;
        }

        adjectives = adjectives == null ? List.of() : List.copyOf(adjectives);
        if (characterSketch == null) {
            characterSketch = "";
        }

        if (speechStyle == null) {
            speechStyle = "";
        }
    }

    public static VillagerPersonality pending() {
        return new VillagerPersonality(PersonalityStatus.PENDING, List.of(), "", "");
    }

    public static VillagerPersonality ready(List<String> adjectives, String characterSketch, String speechStyle) {
        return new VillagerPersonality(PersonalityStatus.READY, adjectives, characterSketch, speechStyle);
    }

    public static VillagerPersonality failed() {
        return new VillagerPersonality(PersonalityStatus.FAILED, List.of(), "", "");
    }

}
