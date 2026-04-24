package dev.breezes.settlements.infrastructure.minecraft.data.building;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class BuildingDefinitionEntry {

    @SerializedName("id")
    String id;

    @SerializedName("placement_priority")
    int placementPriority;

    @SerializedName("zone_tier_min")
    int zoneTierMin;

    @SerializedName("zone_tier_max")
    int zoneTierMax;

    @SerializedName("requires_road_frontage")
    boolean requiresRoadFrontage;

    @SerializedName("requires_resources")
    List<String> requiresResources;

    @SerializedName("forbidden_resources")
    List<String> forbiddenResources;

    @SerializedName("trait_affinities")
    Map<String, Float> traitAffinities;

    @SerializedName("minimum_rank")
    String minimumRank;

    @SerializedName("preferred_tags")
    List<String> preferredTags;

    @SerializedName("footprint_width")
    int footprintWidth;

    @SerializedName("footprint_depth")
    int footprintDepth;

    @SerializedName("npc_profession")
    String npcProfession;

    @SerializedName("npc_count")
    int npcCount;

}
