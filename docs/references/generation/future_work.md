# Settlement Generation — Future Work

Features that are architecturally planned with hooks in the existing data model. Each section
provides enough context to seed a detailed design document.

## Table of Contents

- [Known Issues](#known-issues)
- [Compound Buildings](#compound-buildings)
- [NPC Spawning](#npc-spawning)
- [Block Palette System](#block-palette-system)
- [City Scale Tier](#city-scale-tier)
- [P1 and P2 Traits](#p1-and-p2-traits)
- [Advanced Road Generation](#advanced-road-generation)
- [Settlement Persistence](#settlement-persistence)
- [Other Planned Features](#other-planned-features)

---

## Known Issues

1. **[CRITICAL]** Minecraft crashes when the town_hall cannot be placed in the world at world-gen
   time.
2. Settlements only generate in a small allowlist of biomes — ref
   `src/main/resources/data/settlements/tags/worldgen/biome/has_settlement.json`.

---

## Compound Buildings

A compound is a cluster of 2–3 sub-structures with internal spatial rules:

- **Primary (front):** Faces the road. The public-facing structure (shop, farmhouse).
- **Secondary (back/side):** The workspace, not road-facing (forge, barn).
- **Tertiary (back):** Storage, living quarters, or auxiliary.

**Conceptual model:** Compound buildings would be defined as a parent `BuildingDefinition` with a
list of child definitions and relative placement rules (direction from parent, required adjacency,
shared vs separate plot). The layout engine would place children immediately after the parent.

The current system achieves *approximate* clustering via `ProximityAffinity` (distance-decaying
bonus), which causes related buildings to gravitate together. Formal compounds would make this
explicit and guarantee adjacency.

---

## NPC Spawning

`BuildingDefinition` already carries `npcProfession` and `npcCount` fields (currently unused).

**Conceptual model:** NPCs would be spawned during or after the Build phase. Each building with
`npcProfession` and `npcCount > 0` triggers villager spawning with the appropriate profession.
Population is bounded by `SettlementProfile.estimatedPopulation`. Houses without explicit
professions would spawn generic villagers. Requires a mapping from settlement-specific profession
IDs to Minecraft's `VillagerProfession` registry.

---

## Block Palette System

Currently all buildings use the same oak-plank placeholder templates regardless of biome or wealth.

**Conceptual model:** A `BlockPalette` maps abstract block roles (wall, floor, roof, accent,
pillar) to concrete Minecraft blocks. Palettes would be selected by biome family (forest→oak/spruce,
desert→sandstone, taiga→spruce/stone) and wealth level (poor→raw logs/cobblestone,
prosperous→stone brick/dark oak). Template blocks tagged with abstract roles would be replaced
during the Build phase.

Hooks already in place: wealth level on `SettlementProfile`, biome distribution on `SiteReport`.
Templates would need structure block data markers or a block substitution table.

---

## City Scale Tier

Cities use a multi-anchor expansion model.

**Conceptual model:** A city is composed of multiple districts, each a sub-village with its own
trait emphasis and planning center. The primary district generates normally. Additional districts
spawn at controlled distances, connected by major roads. Each district has its own zone system;
the overall city shares infrastructure (walls, main roads, a central market district).

Requires: multi-anchor support in the layout engine, district-level trait specialization in the
sampling engine, inter-district road generation. `ScaleTier` would gain a `CITY` entry.

---

## P1 and P2 Traits

### P1 — Economy and Social

| Trait     | Trigger                           | Planned Buildings                      |
|-----------|-----------------------------------|----------------------------------------|
| PASTORAL  | Grassland biomes                  | Grazing animals, shepherd buildings    |
| TRADE     | Crossroads potential              | Market-focused, wider roads, signposts |
| HONEY     | FLORAL resource tag               | Apiaries, flower gardens               |
| CRAFT     | Combined resource availability    | Workshops, artisan buildings           |
| SPIRITUAL | Specific biome combinations       | Shrines, temples, sacred groves        |
| SCHOLARLY | Settlement size + trait diversity | Libraries, scriptoriums                |
| WAYPOINT  | Road network potential            | Inns, stables, signpost networks       |

### P2 — Rare and Special

| Trait     | Trigger                                         | Notes                                           |
|-----------|-------------------------------------------------|-------------------------------------------------|
| ARCANE    | Specific biome adjacency patterns               | Custom Java scorer (non-linear logic)           |
| ANCIENT   | Elevated/isolated terrain features              | Custom Java scorer; ruins, archaeological sites |
| SEAFARING | Significant COASTAL coverage + coastal geometry | Harbors, shipyards, lighthouses                 |

Each trait needs: a scorer (JSON-configured or custom Java), building definitions with trait
affinities, and potentially new resource tags.

---

## Advanced Road Generation

Current roads use midpoint displacement for organic curves. Planned improvements:

- **Bezier curves** for smoother road paths
- **Terrain-following Y** with stairs and slabs on slopes
- **Footpaths** connecting scatter-phase buildings to the nearest road
- **Bridge structures** over water features
- **Road material variety** beyond the three-tier wealth system (biome-specific materials)

---

## Settlement Persistence

Settlements currently exist only as structure pieces. A persistence layer via Minecraft's
`SavedData` would enable:

- Runtime queries ("is this position inside a settlement?", "which settlement is nearest?")
- Villager behavior integration (villagers know their settlement's traits and buildings)
- Dynamic settlement modification (building upgrades, population changes, event triggers)
- Map/minimap integration

---

## Other Planned Features

| Feature                      | Status          | Notes                                                                                                              |
|------------------------------|-----------------|--------------------------------------------------------------------------------------------------------------------|
| **Terraforming**             | Planned         | Flatten plots to buildable grade before placement; currently buildings paste at surface Y                          |
| **Detailing pass**           | Planned         | Post-process decorative elements — fences, lanterns, carts, banners, flower pots                                   |
| **Biome-specific templates** | Content pending | Tag-matching infrastructure is fully wired end-to-end; waiting on authored `.nbt` variants for desert, taiga, etc. |
| **Configuration file**       | Planned         | Server-operator tuning: generation frequency, scale distribution weights, spacing, feature toggles                 |
