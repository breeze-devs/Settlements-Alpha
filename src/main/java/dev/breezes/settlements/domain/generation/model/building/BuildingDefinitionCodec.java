package dev.breezes.settlements.domain.generation.model.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitIdCodec;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlotCodec;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTagCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Datapack codec for {@link BuildingDefinition}. {@code display_info} is never populated from the
 * datapack — it is resolved separately and stays {@code null} here, matching the manager's existing
 * two-phase load. {@code proximity_affinities} / {@code global_affinities} are likewise not yet
 * datapack-driven and always default to an empty list. Zone-tier bounds are enforced by
 * {@link BuildingDefinition}'s constructor, so a bad range simply fails this entry's decode.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuildingDefinitionCodec {

    public static final Codec<BuildingDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(BuildingDefinition::id),
                    Codec.unboundedMap(TraitIdCodec.CODEC, Codec.FLOAT)
                            .optionalFieldOf("trait_affinities", Map.<TraitId, Float>of())
                            .forGetter(BuildingDefinition::traitAffinities),
                    TraitSlotCodec.CODEC.optionalFieldOf("minimum_rank", TraitSlot.FLAVOR).forGetter(BuildingDefinition::minimumRank),
                    Codec.INT.fieldOf("placement_priority").forGetter(BuildingDefinition::placementPriority),
                    Codec.INT.fieldOf("zone_tier_min").forGetter(definition -> definition.zoneTierPreference().minInclusive()),
                    Codec.INT.fieldOf("zone_tier_max").forGetter(definition -> definition.zoneTierPreference().maxInclusive()),
                    Codec.BOOL.fieldOf("requires_road_frontage").forGetter(BuildingDefinition::requiresRoadFrontage),
                    ResourceTagCodec.CODEC.listOf().xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("requires_resources", Set.<ResourceTag>of())
                            .forGetter(BuildingDefinition::requiresResources),
                    ResourceTagCodec.CODEC.listOf().xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("forbidden_resources", Set.<ResourceTag>of())
                            .forGetter(BuildingDefinition::forbiddenResources),
                    Codec.INT.fieldOf("footprint_width").forGetter(definition -> definition.footprint().width()),
                    Codec.INT.fieldOf("footprint_depth").forGetter(definition -> definition.footprint().depth()),
                    Codec.STRING.listOf().xmap(Set::copyOf, set -> set.stream().toList())
                            .optionalFieldOf("preferred_tags", Set.<String>of())
                            .forGetter(BuildingDefinition::preferredTags),
                    Codec.STRING.optionalFieldOf("npc_profession").forGetter(definition -> Optional.ofNullable(definition.npcProfession())),
                    Codec.INT.fieldOf("npc_count").forGetter(BuildingDefinition::npcCount)
            ).apply(instance, (id, traitAffinities, minimumRank, placementPriority, zoneTierMin, zoneTierMax,
                               requiresRoadFrontage, requiresResources, forbiddenResources, footprintWidth, footprintDepth,
                               preferredTags, npcProfession, npcCount) ->
                    BuildingDefinition.builder()
                            .id(id)
                            .displayInfo(null)
                            .traitAffinities(traitAffinities)
                            .minimumRank(minimumRank)
                            .placementPriority(placementPriority)
                            .zoneTierPreference(IntRange.of(zoneTierMin, zoneTierMax))
                            .requiresRoadFrontage(requiresRoadFrontage)
                            .requiresResources(requiresResources)
                            .forbiddenResources(forbiddenResources)
                            .footprint(BuildingFootprint.builder().width(footprintWidth).depth(footprintDepth).build())
                            .preferredTags(preferredTags)
                            .proximityAffinities(List.of())
                            .globalAffinities(List.of())
                            .npcProfession(npcProfession.orElse(null))
                            .npcCount(npcCount)
                            .build()));

}
