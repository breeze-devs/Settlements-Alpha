# Settlement Generation — Minecraft Integration

**Purpose:** Bridge the abstract `GenerationResult` into the Minecraft world by resolving templates,
emitting structure pieces, and integrating with Minecraft's structure system.

## Table of Contents

- [Template Resolution](#template-resolution)
- [Structure Pieces](#structure-pieces)
- [Structure Registration](#structure-registration)
- [Generation Flow](#generation-flow)

---

## Template Resolution

The domain layer defines a `TemplateResolver` interface. The infrastructure layer implements it as
`NbtTemplateResolver` (`infrastructure/minecraft/worldgen/NbtTemplateResolver.java`).

### Catalog Warming

At server startup, `NbtTemplateResolver` builds an in-memory catalog from the template directory:

1. Scan `data/settlements/structure/buildings/` for `.nbt` files. Each template **must** have a
   `.meta.json` companion. See [datapack_extensions.md § NBT Templates](datapack_extensions.md#nbt-templates)
   for the full schema.
2. Read each file's actual size via `StructureTemplate.getSize()` and validate against declared
   dimensions in the metadata. Mismatches are skipped with a warning.
3. Cache validated entries indexed by `building_definition` ID.

We do not support `/reload` command. The server must be restarted for template changes to take effect.

### Resolution Logic

On `resolve()`, given a `TemplateResolutionContext` carrying biome tags and visual markers:

1. Look up the building's catalog entries by definition ID; filter by footprint dimensions.
2. Merge `BuildingDefinition.preferredTags`, biome tags, and visual markers into a combined request
   tag set.
3. **Tiered matching:**
    - **Full match** — Entries carrying *all* requested tags → pick randomly from this group.
    - **Partial match** — Pick randomly from the group with the highest tag overlap count.
    - **Untagged fallback** — Pick randomly from entries with no tags.
    - **No fallback** — Return `Optional.empty()` with a logged warning. The catalog validates at
      startup that each building type has at least one untagged template, so this branch should not
      occur under normal operation.

---

## Structure Pieces

### `SettlementBuildingPiece`

Extends `TemplateStructurePiece`. Pastes the resolved `.nbt` template at the plot's target Y
position with rotation derived from the building's facing direction. No terraforming yet — buildings
paste at surface level (acceptable for now; see [future_work.md](future_work.md)).

### `SettlementRoadPiece`

Extends `StructurePiece` directly (not template-based). Places road blocks along each segment using
Bresenham line rasterization. Each block's Y is determined at `postProcess()` time by reading the
chunk's heightmap, so roads follow the terrain surface.

**Road block material by wealth level:**

| Wealth Level        | Material    |
|---------------------|-------------|
| Poor (< 0.33)       | Dirt path   |
| Modest (0.33–0.66)  | Gravel      |
| Prosperous (> 0.66) | Cobblestone |

**Road width by type:**

| Road Type | Width    |
|-----------|----------|
| MAIN      | 9 blocks |
| SECONDARY | 5 blocks |
| SIDE      | 3 blocks |

**Future work:**

- Add road block material palettes according to the biome instead of a single block
- Wealth level and road width should be configurable

---

## Structure Registration

Natural settlement generation is disabled: no active structure set is configured.
`SettlementStructure` remains registered as a structure type. Enabling natural generation requires
a structure-set JSON under `data/settlements/worldgen/structure_set/`.

The `has_settlement` biome tag controls eligible biomes. Its definition is in
`src/main/resources/data/settlements/tags/worldgen/biome/has_settlement.json`.

---

## Generation Flow

When a settlement structure is generated:

1. Derive seed from `worldSeed XOR hash(chunkPosition)`.
2. Build the survey area with `SurveyBounds.aroundSettlementAt`, centered on the start chunk. A
   settlement founded at a bell is surveyed over the same area around its bell, so the two kinds
   score alike.
3. Create `TerrainGrid` via `TerrainGridFactory.fromGenerationContext()`.
4. Fetch registry singletons (biome survey data, trait scorers, building definitions).
5. Run `GenerationPipeline.generate()` to produce `GenerationResult`.
6. Build a `TemplateResolutionContext` from the dominant biome's template tags (via
   `BiomeSurveyDataManager`) and the history phase's `VisualMarkerSet`.
7. Resolve templates for each `BuildingAssignment` via the warmed `NbtTemplateResolver`.
8. Emit `SettlementBuildingPiece` for each resolved assignment.
9. Emit `SettlementRoadPiece` for each road segment.
10. Emit one `SettlementRecordPiece`, which places nothing and carries the settlement's id, claimed
    chunks, profile, and drawn history. A start that placed no building or road emits nothing.

Missing required building definitions, a manifest without a town hall, or a site that cannot fit
the town hall cause generation to decline. The pipeline returns no result, and `SettlementStructure`
emits no pieces, so Minecraft does not create a valid structure start.

When the start chunk loads, [GeneratedSettlementRecordServerEvents][record-events] hands its
generated metadata to [GeneratedSettlementRecorder][recorder] for server-side record admission.
[Settlement Identity and Lifecycle](../settlements.md) owns the store's admission, recovery, and
disbanding guarantees.

[record-events]: ../../../src/main/java/dev/breezes/settlements/bootstrap/event/GeneratedSettlementRecordServerEvents.java
[recorder]: ../../../src/main/java/dev/breezes/settlements/application/settlement/persistence/GeneratedSettlementRecorder.java
