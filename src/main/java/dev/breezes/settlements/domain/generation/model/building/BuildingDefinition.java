package dev.breezes.settlements.domain.generation.model.building;

import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
public final class BuildingDefinition {

    private final String id;
    private final DisplayInfo displayInfo;
    private final Map<TraitId, Float> traitAffinities;
    private final TraitSlot minimumRank;
    private final int placementPriority;
    private final IntRange zoneTierPreference;
    private final boolean requiresRoadFrontage;
    private final Set<ResourceTag> requiresResources;
    private final Set<ResourceTag> forbiddenResources;
    private final BuildingFootprint footprint;
    private final Set<String> preferredTags;
    private final List<ProximityAffinity> proximityAffinities;
    private final List<GlobalAffinity> globalAffinities;
    private final String npcProfession;
    private final int npcCount;

    public BuildingDefinition(String id,
                              DisplayInfo displayInfo,
                              Map<TraitId, Float> traitAffinities,
                              TraitSlot minimumRank,
                              int placementPriority,
                              IntRange zoneTierPreference,
                              boolean requiresRoadFrontage,
                              Set<ResourceTag> requiresResources,
                              Set<ResourceTag> forbiddenResources,
                              BuildingFootprint footprint,
                              Set<String> preferredTags,
                              List<ProximityAffinity> proximityAffinities,
                              List<GlobalAffinity> globalAffinities,
                              String npcProfession,
                              int npcCount) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (npcCount < 0) {
            throw new IllegalArgumentException("npcCount must be >= 0");
        }
        this.id = id;
        this.displayInfo = displayInfo;
        this.traitAffinities = Map.copyOf(traitAffinities);
        this.minimumRank = minimumRank;
        this.placementPriority = placementPriority;
        this.zoneTierPreference = zoneTierPreference;
        this.requiresRoadFrontage = requiresRoadFrontage;
        this.requiresResources = Set.copyOf(requiresResources);
        this.forbiddenResources = Set.copyOf(forbiddenResources);
        this.footprint = footprint;
        this.preferredTags = Set.copyOf(preferredTags);
        this.proximityAffinities = List.copyOf(proximityAffinities);
        this.globalAffinities = List.copyOf(globalAffinities);
        this.npcProfession = npcProfession;
        this.npcCount = npcCount;
    }

    public String id() {
        return this.id;
    }

    public DisplayInfo displayInfo() {
        return this.displayInfo;
    }

    public Map<TraitId, Float> traitAffinities() {
        return this.traitAffinities;
    }

    public TraitSlot minimumRank() {
        return this.minimumRank;
    }

    public int placementPriority() {
        return this.placementPriority;
    }

    public IntRange zoneTierPreference() {
        return this.zoneTierPreference;
    }

    public boolean requiresRoadFrontage() {
        return this.requiresRoadFrontage;
    }

    public Set<ResourceTag> requiresResources() {
        return this.requiresResources;
    }

    public Set<ResourceTag> forbiddenResources() {
        return this.forbiddenResources;
    }

    public BuildingFootprint footprint() {
        return this.footprint;
    }

    public Set<String> preferredTags() {
        return this.preferredTags;
    }

    public List<ProximityAffinity> proximityAffinities() {
        return this.proximityAffinities;
    }

    public List<GlobalAffinity> globalAffinities() {
        return this.globalAffinities;
    }

    public String npcProfession() {
        return this.npcProfession;
    }

    public int npcCount() {
        return this.npcCount;
    }

    public boolean isUniversal() {
        return this.traitAffinities.isEmpty();
    }

    @Override
    public String toString() {
        return "BuildingDefinition{" +
                "id='" + this.id + '\'' +
                ", minimumRank=" + this.minimumRank +
                ", placementPriority=" + this.placementPriority +
                ", zoneTierPreference=" + this.zoneTierPreference +
                ", requiresRoadFrontage=" + this.requiresRoadFrontage +
                ", npcProfession='" + this.npcProfession + '\'' +
                ", npcCount=" + this.npcCount +
                '}';
    }

}
