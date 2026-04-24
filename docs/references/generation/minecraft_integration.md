# Settlement Generation — Minecraft Integration

**Purpose:** Bridge the abstract `GenerationResult` into the Minecraft world by resolving templates,
emitting structure pieces, and registering the settlement as a naturally-spawning structure.

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

The settlement is registered as a proper NeoForge structure set:

- `SettlementStructure` extends `SettlementsStructure` with a codec for worldgen integration.
- Structure set JSON configures random spread placement (spacing: 34, separation: 8 — matching
  vanilla village patterns).
- A biome tag (`has_settlement`) controls where settlements spawn (plains, forest, savanna, taiga,
  meadow, etc.; excluding ocean, desert, mushroom fields, nether, end).

---

## Generation Flow

When Minecraft's world-gen triggers settlement placement:

1. Derive seed from `worldSeed XOR hash(chunkPosition)`.
2. Build `SurveyBounds` from chunk position and default radius.
3. Create `TerrainGrid` via `TerrainGridFactory.fromGenerationContext()`.
4. Fetch registry singletons (biome survey data, trait scorers, building definitions).
5. Run `GenerationPipeline.generate()` to produce `GenerationResult`.
6. Build a `TemplateResolutionContext` from the dominant biome's template tags (via
   `BiomeSurveyDataManager`) and the history phase's `VisualMarkerSet`.
7. Resolve templates for each `BuildingAssignment` via the warmed `NbtTemplateResolver`.
8. Emit `SettlementBuildingPiece` for each resolved assignment.
9. Emit `SettlementRoadPiece` for each road segment.
