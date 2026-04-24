package dev.breezes.settlements.infrastructure.minecraft.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.breezes.settlements.domain.generation.building.TemplateResolutionContext;
import dev.breezes.settlements.domain.generation.building.TemplateResolver;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.building.ResolvedTemplate;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves NBT structure template variants for a given building type.
 */
@Singleton
@ServerSide
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class NbtTemplateResolver extends SimplePreparableReloadListener<Map<String, List<TemplateMetadata>>> implements TemplateResolver {

    private static final String NEOFORGE_STRUCTURE_ROOT = "structure/";
    private static final String TEMPLATE_DISCOVERY_ROOT = NEOFORGE_STRUCTURE_ROOT + "buildings";
    private static final String NBT_EXTENSION = ".nbt";
    private static final String STRUCTURE_SIZE_KEY = "size";

    private static final Gson GSON = new GsonBuilder().create();

    private volatile Map<String, List<TemplateMetadata>> catalog = Map.of();

    @Override
    protected Map<String, List<TemplateMetadata>> prepare(@Nonnull ResourceManager resourceManager,
                                                          @Nonnull ProfilerFiller profiler) {
        log.resourceLoadingStatus("Discovering settlement structure NBT templates...");
        Map<ResourceLocation, Resource> nbtResources = resourceManager.listResources(TEMPLATE_DISCOVERY_ROOT,
                path -> path.getPath().endsWith(NBT_EXTENSION));

        Map<String, List<TemplateMetadata>> entriesByBuilding = new LinkedHashMap<>();

        for (ResourceLocation resourcePath : nbtResources.keySet()) {
            Optional<TemplateMetadata> metadataOptional = readCompanionMetadata(resourceManager, resourcePath);
            if (metadataOptional.isEmpty()) {
                continue;
            }
            TemplateMetadata metadata = metadataOptional.get();

            ResourceLocation resourceLocation = toMinecraftTemplateId(resourcePath);
            metadata.setResourceLocation(resourceLocation);

            Optional<int[]> structureSizeOptional = readStructureSize(resourceManager, resourcePath);
            if (structureSizeOptional.isEmpty()) {
                continue;
            }

            int[] structureSize = structureSizeOptional.get();
            if (!matchesMetadataDimensions(metadata, structureSize)) {
                log.resourceLoadingError("Skipping template {} because metadata dimensions (width={}, height={}, depth={},) do not match NBT size {}",
                        resourceLocation, metadata.getWidth(), metadata.getDepth(), metadata.getHeight(), structureSize);
                continue;
            }

            log.resourceLoadingStatus("Loaded building template {}: {}", resourceLocation, metadata);
            entriesByBuilding.computeIfAbsent(metadata.getBuildingDefinition(), ignored -> new ArrayList<>()).add(metadata);
        }

        if (entriesByBuilding.isEmpty()) {
            log.resourceLoadingWarn("No settlement building templates were discovered under {}", TEMPLATE_DISCOVERY_ROOT);
            return Map.of();
        }

        int totalTemplates = entriesByBuilding.values().stream().mapToInt(List::size).sum();
        for (Map.Entry<String, List<TemplateMetadata>> entry : entriesByBuilding.entrySet()) {
            boolean hasUntaggedFallback = entry.getValue().stream()
                    .anyMatch(template -> template.getTags().isEmpty());
            if (!hasUntaggedFallback) {
                log.resourceLoadingError("Template catalog entry '{}' has no untagged fallback template", entry.getKey());
            }

            log.resourceLoadingStatus("Template catalog type '{}' loaded with {} variants", entry.getKey(), entry.getValue().size());
        }
        log.resourceLoadingStatus("Loaded {} settlement building templates across {} building types", totalTemplates, entriesByBuilding.size());

        return entriesByBuilding.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    @Override
    protected void apply(@Nonnull Map<String, List<TemplateMetadata>> preparedCatalog,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        this.catalog = preparedCatalog;

        int totalTemplates = preparedCatalog.values().stream()
                .mapToInt(List::size)
                .sum();
        log.resourceLoadingStatus("Applied settlement template catalog with {} templates across {} building types",
                totalTemplates, preparedCatalog.size());
    }

    @Override
    public Optional<ResolvedTemplate> resolve(BuildingDefinition definition,
                                              Random random,
                                              TemplateResolutionContext context) {
        List<TemplateMetadata> candidates = this.catalog.get(definition.id());
        if (candidates == null || candidates.isEmpty()) {
            log.info("Template resolution found no catalog entries for building {}", definition.id());
            return Optional.empty();
        }

        BuildingFootprint footprint = definition.footprint();
        Set<String> contextTags = context.allRequestedTags();
        Set<String> prefTags = definition.preferredTags();

        Set<String> requestedTags;
        if (contextTags.isEmpty() && prefTags.isEmpty()) {
            requestedTags = Set.of();
        } else {
            requestedTags = new LinkedHashSet<>(contextTags);
            requestedTags.addAll(prefTags);
        }

        // Pre-allocate buckets for our single-pass categorization
        List<TemplateMetadata> allFootprintMatches = new ArrayList<>();
        List<TemplateMetadata> fullMatches = new ArrayList<>();
        List<TemplateMetadata> bestPartialMatches = new ArrayList<>();
        List<TemplateMetadata> untaggedMatches = new ArrayList<>();

        int bestPartialOverlapCount = 0;
        int targetOverlapCount = requestedTags.size();

        for (TemplateMetadata entry : candidates) {
            if (!footprint.fits(entry.getWidth(), entry.getDepth())) {
                continue;
            }
            allFootprintMatches.add(entry);

            boolean isUntagged = entry.getTags().isEmpty();
            if (isUntagged) {
                untaggedMatches.add(entry);
            }

            if (targetOverlapCount > 0 && !isUntagged) {
                int currentOverlapCount = overlapCount(entry.getTags(), requestedTags);

                // If overlap equals target overlap, it contains all requested tags
                if (currentOverlapCount == targetOverlapCount) {
                    fullMatches.add(entry);
                } else if (currentOverlapCount > 0) {
                    if (currentOverlapCount > bestPartialOverlapCount) {
                        bestPartialOverlapCount = currentOverlapCount;
                        bestPartialMatches.clear();
                        bestPartialMatches.add(entry);
                    } else if (currentOverlapCount == bestPartialOverlapCount) {
                        bestPartialMatches.add(entry);
                    }
                }
            }
        }

        if (allFootprintMatches.isEmpty()) {
            log.info("Template resolution found no footprint matches for building {} in {} candidates", definition.id(), candidates.size());
            return Optional.empty();
        }

        // Select template
        TemplateMetadata selected;
        String resolutionPath;

        if (requestedTags.isEmpty()) {
            if (!untaggedMatches.isEmpty()) {
                selected = pickRandom(untaggedMatches, random);
                resolutionPath = "untagged-fallback";
            } else {
                log.error("No untagged fallback for building {}, selecting randomly from {} candidates", definition.id(), allFootprintMatches.size());
                selected = pickRandom(allFootprintMatches, random);
                resolutionPath = "error-random-fallback";
            }
        } else {
            if (!fullMatches.isEmpty()) {
                selected = pickRandom(fullMatches, random);
                resolutionPath = "full-match";
            } else if (!bestPartialMatches.isEmpty()) {
                selected = pickRandom(bestPartialMatches, random);
                resolutionPath = "partial-match";
            } else if (!untaggedMatches.isEmpty()) {
                selected = pickRandom(untaggedMatches, random);
                resolutionPath = "untagged-fallback";
            } else {
                log.error("No untagged fallback for building {}, no tag match for {}", definition.id(), requestedTags);
                selected = pickRandom(allFootprintMatches, random);
                resolutionPath = "error-random-fallback";
            }
        }

        log.info("Resolved template for building {} via {} (candidates={}, requestedTags={}, selected={}, templateTags={})",
                definition.id(), resolutionPath, allFootprintMatches.size(), requestedTags, selected.resourceLocationOrThrow(), selected.getTags());

        return Optional.of(ResolvedTemplate.builder()
                .templatePath(selected.resourceLocationOrThrow().toString())
                .width(selected.getWidth())
                .depth(selected.getDepth())
                .tags(selected.getTags())
                .build());
    }

    private static Optional<TemplateMetadata> readCompanionMetadata(ResourceManager resourceManager, ResourceLocation nbtResource) {
        ResourceLocation metadataPath = ResourceLocation.fromNamespaceAndPath(nbtResource.getNamespace(),
                nbtResource.getPath().replaceAll("\\.nbt$", ".meta.json"));

        Optional<Resource> resourceOptional = resourceManager.getResource(metadataPath);
        if (resourceOptional.isEmpty()) {
            log.resourceLoadingWarn("Ignoring template {} because required companion metadata {} is missing", nbtResource, metadataPath);
            return Optional.empty();
        }

        try (BufferedReader reader = resourceOptional.get().openAsReader()) {
            TemplateMetadata metadata = GSON.fromJson(reader, TemplateMetadata.class);
            if (metadata == null) {
                log.resourceLoadingWarn("Ignoring template metadata {} because it is empty or malformed", metadataPath);
                return Optional.empty();
            }
            if (!metadata.isValid()) {
                log.resourceLoadingWarn("Ignoring template metadata {} because required fields are missing or invalid", metadataPath);
                return Optional.empty();
            }
            return Optional.of(metadata);
        } catch (Exception e) {
            log.resourceLoadingWarn("Failed to read template metadata {}: {}", metadataPath, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<int[]> readStructureSize(ResourceManager resourceManager, ResourceLocation nbtResource) {
        Optional<Resource> resourceOptional = resourceManager.getResource(nbtResource);
        if (resourceOptional.isEmpty()) {
            log.resourceLoadingWarn("Skipping template {} because resource stream could not be opened", nbtResource);
            return Optional.empty();
        }

        try (InputStream inputStream = resourceOptional.get().open()) {
            CompoundTag root = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            ListTag sizeTag = root.getList(STRUCTURE_SIZE_KEY, Tag.TAG_INT);

            return Optional.of(new int[]{sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2)});
        } catch (Exception e) {
            log.resourceLoadingWarn("Failed to read structure size from {}", nbtResource, e);
            return Optional.empty();
        }
    }

    private static boolean matchesMetadataDimensions(TemplateMetadata metadata, int[] size) {
        return size[0] == metadata.getWidth()
                && size[2] == metadata.getDepth()
                && size[1] == metadata.getHeight();
    }

    private static ResourceLocation toMinecraftTemplateId(ResourceLocation resourcePath) {
        String fullPath = resourcePath.getPath();
        if (!fullPath.startsWith(NEOFORGE_STRUCTURE_ROOT) || !fullPath.endsWith(NBT_EXTENSION)) {
            throw new IllegalStateException("Unexpected structure resource path: " + resourcePath);
        }

        String templatePath = fullPath.substring(NEOFORGE_STRUCTURE_ROOT.length(), fullPath.length() - NBT_EXTENSION.length());
        return ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), templatePath);
    }

    private static TemplateMetadata pickRandom(List<TemplateMetadata> entries, Random random) {
        return entries.get(random.nextInt(entries.size()));
    }

    private static int overlapCount(Set<String> templateTags, Set<String> requestedTags) {
        int overlap = 0;
        for (String requestedTag : requestedTags) {
            if (templateTags.contains(requestedTag)) {
                overlap++;
            }
        }
        return overlap;
    }

}
