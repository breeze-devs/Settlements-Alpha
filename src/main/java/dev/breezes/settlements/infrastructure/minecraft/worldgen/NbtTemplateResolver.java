package dev.breezes.settlements.infrastructure.minecraft.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.breezes.settlements.domain.generation.building.TemplateResolutionContext;
import dev.breezes.settlements.domain.generation.building.TemplateResolver;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.FootprintConstraint;
import dev.breezes.settlements.domain.generation.model.building.ResolvedTemplate;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.Builder;
import lombok.CustomLog;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves NBT structure template variants for a given building type.
 *
 * <p>Three distinct resource identifiers are in play here:
 *
 * <ul>
 *   <li><b>Resource path</b> — the raw path returned by {@code ResourceManager.listResources()},
 *       e.g. {@code settlements:structure/buildings/fish_market.nbt}.
 *       This includes the NeoForge-reserved {@code structure/} root and the .nbt extension.</li>
 *
 *   <li><b>Template ID</b> — the identifier {@code StructureTemplateManager.get()} accepts,
 *       e.g. {@code settlements:buildings/fish_market}.
 *       Same as the resource path but with {@code structure/} and {@code .nbt} stripped.
 *       NeoForge re-adds the prefix internally. No file at this path exists on disk.</li>
 *
 *   <li><b>Building definition ID</b> — the logical building type this template belongs to,
 *       e.g. {@code settlements:building_definitions/fish_market}. Declared in the companion {@code .meta.json}
 *       {@code building_definition} field. This is the catalog key used during resolution.
 *       Completely independent of the NBT file's location or name.</li>
 * </ul>
 */
@CustomLog
public final class NbtTemplateResolver implements TemplateResolver {

    // NeoForge stores all structure templates under "structure/" in the data pack.
    // ResourceManager.listResources() returns full paths including this prefix.
    private static final String NEOFORGE_STRUCTURE_ROOT = "structure/";

    // The subfolder within the structure root where settlement templates live.
    private static final String TEMPLATE_DISCOVERY_ROOT = NEOFORGE_STRUCTURE_ROOT + "buildings";

    private static final String NBT_EXTENSION = ".nbt";

    private static final Gson GSON = new GsonBuilder().create();

    private static volatile NbtTemplateResolver INSTANCE = null;
    private static volatile StructureTemplateManager lastTemplateManager = null;
    private static volatile ResourceManager lastResourceManager = null;
    private static volatile List<TemplateDescriptor> lastDiscoveredTemplates = List.of();

    private final Map<String, List<TemplateEntry>> catalog;

    NbtTemplateResolver(@Nonnull Map<String, List<TemplateEntry>> catalog) {
        Map<String, List<TemplateEntry>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<TemplateEntry>> entry : catalog.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.catalog = Map.copyOf(immutable);
    }

    public static NbtTemplateResolver getInstance(@Nonnull StructureTemplateManager templateManager) {
        lastTemplateManager = templateManager;
        if (INSTANCE == null) {
            synchronized (NbtTemplateResolver.class) {
                if (INSTANCE == null) {
                    ResourceManager resourceManager = lastResourceManager;
                    List<TemplateDescriptor> descriptors = lastDiscoveredTemplates;
                    if (!descriptors.isEmpty()) {
                        INSTANCE = new NbtTemplateResolver(loadCatalog(templateManager, descriptors));
                    } else if (resourceManager == null) {
                        log.resourceLoadingWarn("Settlement template resolver initialized before ResourceManager was available; template catalog will be empty until reload");
                        INSTANCE = new NbtTemplateResolver(Map.of());
                    } else {
                        descriptors = discoverTemplateDescriptors(resourceManager);
                        lastDiscoveredTemplates = descriptors;
                        INSTANCE = new NbtTemplateResolver(loadCatalog(templateManager, descriptors));
                    }
                }
            }
        }
        return INSTANCE;
    }

    public static void refresh(@Nonnull ResourceManager resourceManager) {
        lastResourceManager = resourceManager;
        List<TemplateDescriptor> descriptors = discoverTemplateDescriptors(resourceManager);
        lastDiscoveredTemplates = descriptors;
        StructureTemplateManager templateManager = lastTemplateManager;
        synchronized (NbtTemplateResolver.class) {
            INSTANCE = null;
        }
        log.resourceLoadingStatus("Settlement template resolver descriptors reloaded from ResourceManager (templates discovered={})", descriptors.size());
        if (templateManager == null) {
            log.resourceLoadingStatus("Settlement template resolver rebuild deferred until StructureTemplateManager becomes available");
            return;
        }
        getInstance(templateManager);
    }

    @Override
    public Optional<ResolvedTemplate> resolve(BuildingDefinition building, Random random, TemplateResolutionContext context) {
        List<TemplateEntry> candidates = this.catalog.get(building.id());
        if (candidates == null || candidates.isEmpty()) {
            log.info("Template resolution found no catalog entries for building {}", building.id());
            return Optional.empty();
        }

        FootprintConstraint footprint = building.footprint();
        List<TemplateEntry> footprintMatches = candidates.stream()
                .filter(entry -> footprint.fits(entry.width(), entry.depth()))
                .toList();
        if (footprintMatches.isEmpty()) {
            log.info("Template resolution found no footprint matches for building {} in {} candidates", building.id(), candidates.size());
            return Optional.empty();
        }

        Set<String> requestedTags = new LinkedHashSet<>(context.allRequestedTags());
        requestedTags.addAll(building.preferredTags());

        TemplateEntry selected;
        String resolutionPath;

        if (requestedTags.isEmpty()) {
            List<TemplateEntry> untagged = footprintMatches.stream()
                    .filter(entry -> entry.tags().isEmpty())
                    .toList();
            if (untagged.isEmpty()) {
                log.error("No untagged fallback for building {}, selecting randomly from {} candidates", building.id(), footprintMatches.size());
                selected = pickRandom(footprintMatches, random);
                resolutionPath = "error-random-fallback";
            } else {
                selected = pickRandom(untagged, random);
                resolutionPath = "untagged-fallback";
            }
        } else {
            List<TemplateEntry> fullMatches = footprintMatches.stream()
                    .filter(entry -> entry.tags().containsAll(requestedTags))
                    .toList();
            if (!fullMatches.isEmpty()) {
                selected = pickRandom(fullMatches, random);
                resolutionPath = "full-match";
            } else {
                int bestOverlap = footprintMatches.stream()
                        .mapToInt(entry -> overlapCount(entry.tags(), requestedTags))
                        .max()
                        .orElse(0);

                if (bestOverlap > 0) {
                    List<TemplateEntry> partialMatches = footprintMatches.stream()
                            .filter(entry -> overlapCount(entry.tags(), requestedTags) == bestOverlap)
                            .toList();
                    selected = pickRandom(partialMatches, random);
                    resolutionPath = "partial-match";
                } else {
                    List<TemplateEntry> untagged = footprintMatches.stream()
                            .filter(entry -> entry.tags().isEmpty())
                            .toList();
                    if (!untagged.isEmpty()) {
                        selected = pickRandom(untagged, random);
                        resolutionPath = "untagged-fallback";
                    } else {
                        log.error("No untagged fallback for building {}, no tag match for {}", building.id(), requestedTags);
                        selected = pickRandom(footprintMatches, random);
                        resolutionPath = "error-random-fallback";
                    }
                }
            }
        }

        log.info("Resolved template for building {} via {} (candidates={}, requestedTags={}, selected={}, templateTags={})",
                building.id(), resolutionPath, footprintMatches.size(), requestedTags, selected.templateId(), selected.tags());

        return Optional.of(ResolvedTemplate.builder()
                .templatePath(selected.templateId().toString())
                .width(selected.width())
                .depth(selected.depth())
                .tags(selected.tags())
                .build());
    }

    private static List<TemplateDescriptor> discoverTemplateDescriptors(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> nbtResources = resourceManager.listResources(TEMPLATE_DISCOVERY_ROOT,
                path -> path.getPath().endsWith(NBT_EXTENSION));
        log.resourceLoadingStatus("Discovering settlement structure templates...");

        List<TemplateDescriptor> descriptors = new ArrayList<>();

        for (ResourceLocation resourcePath : nbtResources.keySet()) {
            ResourceLocation templateId = toMinecraftTemplateId(resourcePath);
            MetaEntry metadata = readMetadata(resourceManager, resourcePath).get();

            TemplateDescriptor descriptor = TemplateDescriptor.builder()
                    .templateId(templateId)
                    .buildingDefinitionId(metadata.buildingDefinition())
                    .tags(metadata.tags().stream()
                            .filter(tag -> !tag.isBlank())
                            .collect(Collectors.toUnmodifiableSet()))
                    .author(metadata.author())
                    .build();
            descriptors.add(descriptor);
            log.resourceLoadingStatus("Discovered template descriptor {}", descriptor);
        }

        if (descriptors.isEmpty()) {
            log.resourceLoadingWarn("No settlement building templates were discovered under {}", TEMPLATE_DISCOVERY_ROOT);
            return List.of();
        }

        return List.copyOf(descriptors);
    }

    private static Map<String, List<TemplateEntry>> loadCatalog(StructureTemplateManager templateManager,
                                                                Collection<TemplateDescriptor> descriptors) {
        Map<String, List<TemplateEntry>> entriesByBuilding = new LinkedHashMap<>();
        log.resourceLoadingStatus("Materializing settlement structure template catalog...");

        for (TemplateDescriptor descriptor : descriptors) {
            Optional<Vec3i> sizeOptional = loadTemplateSize(templateManager, descriptor.templateId());
            if (sizeOptional.isEmpty()) {
                continue;
            }

            Vec3i size = sizeOptional.get();
            TemplateEntry entry = new TemplateEntry(descriptor.templateId(), descriptor.buildingDefinitionId(), size.getX(), size.getZ(), descriptor.tags());
            log.resourceLoadingStatus("Loaded template {}: {}", descriptor.templateId(), entry);

            entriesByBuilding.computeIfAbsent(entry.buildingDefinitionId(), ignored -> new ArrayList<>()).add(entry);
        }

        if (entriesByBuilding.isEmpty()) {
            log.resourceLoadingWarn("No settlement building templates could be materialized from {} discovered descriptors", descriptors.size());
            return Map.of();
        }

        int totalTemplates = entriesByBuilding.values().stream()
                .mapToInt(List::size)
                .sum();
        for (Map.Entry<String, List<TemplateEntry>> entry : entriesByBuilding.entrySet()) {
            boolean hasUntaggedFallback = entry.getValue().stream().anyMatch(templateEntry -> templateEntry.tags().isEmpty());
            if (!hasUntaggedFallback) {
                log.resourceLoadingError("Template catalog entry '{}' has no untagged fallback template", entry.getKey());
            }
            log.resourceLoadingStatus("Template catalog type '{}' loaded with {} variants", entry.getKey(), entry.getValue().size());
        }

        log.resourceLoadingStatus("Loaded {} settlement building templates across {} building types", totalTemplates, entriesByBuilding.size());
        return entriesByBuilding;
    }

    private static Optional<Vec3i> loadTemplateSize(StructureTemplateManager templateManager, ResourceLocation templateId) {
        Optional<StructureTemplate> templateOptional = templateManager.get(templateId);
        if (templateOptional.isEmpty()) {
            log.resourceLoadingWarn("Skipping template {} because StructureTemplateManager could not load it", templateId);
            return Optional.empty();
        }

        Vec3i size = templateOptional.get().getSize();
        if (size.getX() <= 0 || size.getZ() <= 0) {
            log.resourceLoadingWarn("Skipping template {} because it has invalid size {}", templateId, size);
            return Optional.empty();
        }

        return Optional.of(size);
    }

    private static Optional<MetaEntry> readMetadata(ResourceManager resourceManager, ResourceLocation nbtResource) {
        ResourceLocation metadataPath = ResourceLocation.fromNamespaceAndPath(nbtResource.getNamespace(),
                nbtResource.getPath().replaceAll("\\.nbt$", ".meta.json"));

        Optional<Resource> resourceOptional = resourceManager.getResource(metadataPath);
        if (resourceOptional.isEmpty()) {
            return Optional.empty();
        }

        try (BufferedReader reader = resourceOptional.get().openAsReader()) {
            MetaEntry meta = GSON.fromJson(reader, MetaEntry.class);
            if (meta == null || meta.buildingDefinition() == null || meta.buildingDefinition().isBlank()) {
                log.resourceLoadingWarn("Ignoring template metadata {} because building_definition is missing", metadataPath);
                return Optional.empty();
            }

            return Optional.of(meta);
        } catch (Exception e) {
            log.resourceLoadingWarn("Failed to read template metadata {}: {}", metadataPath, e.getMessage());
            return Optional.empty();
        }
    }

    @VisibleForTesting
    public static ResourceLocation toMinecraftTemplateId(ResourceLocation resourcePath) {
        String fullPath = resourcePath.getPath();
        if (!fullPath.startsWith(NEOFORGE_STRUCTURE_ROOT) || !fullPath.endsWith(NBT_EXTENSION)) {
            throw new IllegalStateException("Unexpected structure resource path: " + resourcePath);
        }

        // Strip the implicit NeoForge root prefix and the file extension
        String templatePath = fullPath.substring(NEOFORGE_STRUCTURE_ROOT.length(), fullPath.length() - NBT_EXTENSION.length());
        return ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), templatePath);
    }

    private static TemplateEntry pickRandom(List<TemplateEntry> entries, Random random) {
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

    protected record TemplateEntry(
            ResourceLocation templateId,
            String buildingDefinitionId,
            int width,
            int depth,
            Set<String> tags
    ) {
        protected TemplateEntry {
            tags = tags == null ? Set.of() : tags;
        }
    }

    @VisibleForTesting
    static Map<String, List<TemplateEntry>> materializeCatalogForTesting(Collection<TemplateDescriptor> descriptors,
                                                                         Function<ResourceLocation, Optional<Vec3i>> sizeLoader) {
        Map<String, List<TemplateEntry>> entriesByBuilding = new LinkedHashMap<>();
        for (TemplateDescriptor descriptor : descriptors) {
            Optional<Vec3i> sizeOptional = sizeLoader.apply(descriptor.templateId());
            if (sizeOptional.isEmpty()) {
                continue;
            }

            Vec3i size = sizeOptional.get();
            if (size.getX() <= 0 || size.getZ() <= 0) {
                continue;
            }

            TemplateEntry entry = new TemplateEntry(descriptor.templateId(), descriptor.buildingDefinitionId(), size.getX(), size.getZ(), descriptor.tags());
            entriesByBuilding.computeIfAbsent(entry.buildingDefinitionId(), ignored -> new ArrayList<>()).add(entry);
        }

        return Map.copyOf(entriesByBuilding);
    }

    @VisibleForTesting
    @Builder
    protected record TemplateDescriptor(
            ResourceLocation templateId,
            String buildingDefinitionId,
            Set<String> tags,
            @Nullable String author
    ) {
        protected TemplateDescriptor {
            tags = tags == null ? Set.of() : tags;
        }
    }

    private record MetaEntry(@SerializedName("building_definition") String buildingDefinition,
                             List<String> tags,
                             @Nullable String author) {
    }

}
