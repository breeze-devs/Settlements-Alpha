# Settlement Generation

## Table of Contents

- [1. Vision and Design Philosophy](#1-vision-and-design-philosophy)
- [2. Architecture Overview](#2-architecture-overview)
- [3. Data Model](#3-data-model)
- [4. Pipeline Walkthrough](#4-pipeline-walkthrough)
    - [4.1 Survey Phase](#41-survey-phase)
    - [4.2 Trait Scoring Phase](#42-trait-scoring-phase)
    - [4.3 Profile Sampling Phase](#43-profile-sampling-phase)
    - [4.4 History Event Phase](#44-history-event-phase)
    - [4.5 Building Registry and Manifest](#45-building-registry-and-manifest)
    - [4.6 Layout and Assignment](#46-layout-and-assignment)
- [5. Minecraft Integration](#5-minecraft-integration)
- [6. Datapack Extension Points](#6-datapack-extension-points)
- [7. Future Work](#7-future-work)

---

## 1. Vision and Design Philosophy

> *Every settlement has scars and trophies from its past.*

The settlement generation system produces living, logical villages in Minecraft. Every settlement is
shaped by the terrain it spawns on, flavored by its history and events, and assembled from a
trait-driven building system that ensures no two villages feel the same -- while all of them feel
*real*.

### Core Tenets

**Terrain drives identity.** A village in a forest near rivers naturally leans toward lumber and
fishing. A village on an ocean coast leans toward trade and seafaring. The player should look at the
terrain and think "yeah, this village makes sense here." The land dictates what's possible.

**Traits, not archetypes.** Villages are not typed. There is no "lumber village" enum. Instead, each
village carries a weighted bag of traits (LUMBER, FISHING, MINING, DEFENSE, etc.) sampled from a
probability distribution shaped by terrain and history. The same terrain can produce different villages
on different world seeds.

**Every settlement has a story.** Before buildings are placed, the generator rolls history events
that modify trait weights and leave visual marks on the village. A "great fire" event reduces lumber
dependence and adds charred ruins. A "trade route discovered" event widens the main road and adds a
signpost. These are mechanical -- they change the output -- not just flavor text.

**Logical spatial layout.** Buildings are placed by a rule system, not randomly. Taverns fight for
road-front plots near the center. Sawmills go to the forest edge. Farmers live on the outskirts.
Guard towers sit at the perimeter. A player walking through the village should feel an intuitive
spatial logic without needing it explained.

**Compound buildings, not singular blocks.** A "blacksmith" is not one structure. It's a compound:
a storefront facing the road, a forge behind it, living quarters beside. A farm is a farmhouse plus
a barn plus fenced fields. Buildings are clusters with internal spatial rules. *(Compound buildings
are architecturally planned but not yet implemented; see [Future Work](#7-future-work).)*

**Exposed for extension.** Mod-pack friendliness is a core requirement, not an afterthought. The
building blocks of a village -- its potential traits, the weights of history events, the rules of
building placement, etc. -- are exposed externally through JSON files. By adopting a datapack-first
philosophy, anyone can seamlessly inject custom lore, new village economies, or entirely new
architectural rulesets without touching the source code.

### Settlement Scale Tiers

Settlements exist on a spectrum of size and complexity:

| Tier    | Trait Slots | Buildings | Population | Area Radius | Notes                                      |
|---------|-------------|-----------|------------|-------------|--------------------------------------------|
| Hamlet  | 1-2         | 5-8       | 10-20      | ~32 blocks  | No formal roads, shared clearing           |
| Village | 2-4         | 10-20     | 30-60      | ~56 blocks  | Main road, civic buildings, optional walls |
| Town    | 4-5         | 25-40     | 80-150     | ~80 blocks  | Crossroads, full zoning, stone walls       |
| City    | --          | --        | --         | --          | *Future: multi-district expansion*         |

Scale tier is selected via weighted random during profile generation. The tier determines trait
slot count, building count ranges, road complexity, and zone thresholds.

### Engine-Agnostic Design

A non-negotiable architectural principle: all generation logic -- from terrain survey to layout --
operates on abstract data types with zero Minecraft API imports. If someone ported this to a
different voxel game, only the trigger (structure registration) and build (piece emission) phases
would need rewriting. This enables thorough unit testing without launching Minecraft.

---

## 2. Architecture Overview

### Pipeline

The generation pipeline is strictly linear. Each phase consumes only the output of the previous
phase. No phase reaches back or skips ahead. This enables independent testing, iteration, and
replacement of any single phase.

| Phase    | Input                                | Output                                      | Minecraft Dependency       |
|----------|--------------------------------------|---------------------------------------------|----------------------------|
| Trigger  | Worldgen event                       | Chunk region claim                          | Yes                        |
| Survey   | TerrainGrid + SurveyBounds           | SiteReport                                  | No (grid built by adapter) |
| Score    | SiteReport                           | Map<TraitId, Float>                         | No                         |
| Sample   | Trait scores + RNG seed              | SettlementProfile                           | No                         |
| History  | SettlementProfile + SiteReport + RNG | Updated SettlementProfile + VisualMarkerSet | No                         |
| Manifest | Profile + BuildingRegistry           | BuildingManifest                            | No                         |
| Layout   | SiteReport + Profile + Manifest      | LayoutResult                                | No                         |
| Build    | GenerationResult                     | Blocks in world                             | Yes                        |

### Package Structure

The codebase follows clean architecture conventions:

- **`domain/`** -- Engine-agnostic business logic, data objects, pure functions. The survey engine,
  scoring engine, sampling engine, manifest calculator, and layout engine all live here.
- **`infrastructure/`** -- Minecraft/NeoForge-coupled implementations. The terrain grid factory,
  datapack reload listeners, NBT template resolver, and structure pieces live here.
- **`bootstrap/`** -- Registration and wiring. Structure type registration, datapack reload event
  hookup, and server startup initialization.
- **`presentation/`** -- Commands and player-facing surface. The `/stest generate` debug command
  lives here.

Only `infrastructure/` and `bootstrap/` import Minecraft/NeoForge APIs.

### Key Terminology

Two spatial concepts are critical throughout the pipeline:

- **Anchor** -- The site/worldgen origin, derived from the build area center. This is the starting
  point for hub search and the reference used by the structure system.
- **Planning center** -- The placed town hall's center position. Once the hub is placed, the
  planning center becomes the reference for zoning, road rooting, orientation, and spatial semantics.
  It may differ from the anchor if the anchor position is unsuitable (e.g., waterlogged).

### Orchestration

`GenerationPipeline` is the top-level domain orchestrator. It receives a `TerrainGrid`,
`SurveyBounds`, and seed, then runs the full chain: Survey → Score → Sample → History → Manifest → Layout.
It returns a `GenerationResult` bundling all phase outputs.

The pipeline takes the following dependencies via constructor injection:

- `BiomeSurveyLookup` -- biome-to-resource mapping (used by Survey and Layout)
- `TraitScorerRegistry` -- registered trait scoring functions
- `BuildingRegistry` -- building definition catalog
- `HistoryEventRegistry` -- history event definitions
- `TraitRegistry` -- known trait definitions (used to validate event modifiers)

All randomness flows from a single seed derived from the world seed XOR a hash of the chunk
position. Settlements are fully reproducible given the same seed and terrain.

---

## 3. Data Model

This section describes the key data contracts that flow between pipeline phases. These types live in
`domain/generation/model/` and carry no Minecraft imports.

### Core Geometry

- **`BlockPosition(int x, int y, int z)`** -- Lightweight position record. Created by the thousands
  during layout; uses value semantics and avoids boxing.
- **`BoundingRegion(BlockPosition min, BlockPosition max)`** -- Axis-aligned bounding box. Supports
  intersection tests, expansion, and area computation. Most operations are XZ-plane.
- **`Direction`** -- Cardinal directions (NORTH, SOUTH, EAST, WEST) with rotation and offset
  helpers.

### Biome Identity

**`BiomeId`** is a scoped interned wrapper for biome identifiers (e.g., `"minecraft:plains"`).
It uses a bounded `ConcurrentHashMap` pool specific to biomes, enabling identity-based equality
(`==`) and fast hash lookups without per-call string allocation. The pool is bounded because the
set of loaded biomes is static per server session.

### Terrain Data

- **`TerrainSample(int height, BiomeId biome, float temperature)`** -- A single terrain sample
  point.
- **`TerrainGrid`** -- 2D array of `TerrainSample` instances sampled at a configurable interval
  (typically every 4 blocks). The grid carries its world origin and provides coordinate conversion
  between world space and grid indices. This is the single source of truth for terrain data --
  everything else (flatness, forest density, resource tags) is derived by querying the grid.
- **`SurveyBounds`** -- Defines the build area (where buildings can go) and a larger sample area
  (build area + ~30 blocks of padding). The padding provides terrain context beyond the build edges,
  enabling the survey to distinguish narrow rivers from wide oceans and to see forest beyond the
  settlement boundary.

### Survey Output

**`SiteReport`** bundles the terrain grid with precomputed analysis:

- `ElevationStats` -- min, max, mean height, and the high-point position
- `Map<ResourceTag, Float> resourceDensities` -- weighted average density per resource tag across
  the sample area
- `Map<BiomeId, Float> biomeDistribution` -- biome coverage percentages, sorted descending
- `Set<WaterFeatureType> waterFeatureTypes` -- detected water bodies (RIVER, OCEAN, LAKE)
- `Set<ResourceTag> resourceTags` -- resource tags that exceed their activation threshold

**`ResourceTag`** represents an observation about what the land provides. Each tag has a coverage
threshold for activation:

| Tag         | Threshold | Derivation                                |
|-------------|-----------|-------------------------------------------|
| LUMBER      | 0.15      | Coverage of forest biome family           |
| FRESHWATER  | 0.01      | Coverage of river biome family            |
| COASTAL     | 0.05      | Coverage of ocean biome family            |
| STONE       | 0.20      | Coverage of mountain/stony biome family   |
| ORE_BEARING | 0.20      | Coverage of mountain/mesa/badlands family |
| FLORAL      | 0.10      | Coverage of flower_forest/meadow          |
| ARID        | 0.30      | Coverage of desert/badlands               |
| FROZEN      | 0.30      | Coverage of snowy/ice biome family        |

Resource tags are the "observation" layer between raw biome data and trait scoring.
They answer "what does this plot of land provide?" based on world data.

### Trait and Profile Objects

**`TraitId`** is a string-based interned value object (e.g., `"settlements:settlement_traits/lumber"`).
It uses the same bounded pool pattern as `BiomeId` -- values are interned on first use via a
`ConcurrentHashMap` pool, enabling identity-based equality (`==`) and fast hash lookups without
per-call string allocation. Known traits are loaded from JSON datapack files at server startup
and registered in `TraitRegistry`.

The mod ships with the following built-in trait definitions:

- **P0 (Core):**
    - `settlements:settlement_traits/lumber`
    - `settlements:settlement_traits/mining`
    - `settlements:settlement_traits/farming`
    - `settlements:settlement_traits/fishing`
    - `settlements:settlement_traits/defense`
- **P1 (Economy & Social):**
    - `settlements:settlement_traits/pastoral`
    - `settlements:settlement_traits/trade`
    - `settlements:settlement_traits/honey`
    - `settlements:settlement_traits/craft`
    - `settlements:settlement_traits/spiritual`
    - `settlements:settlement_traits/scholarly`
    - `settlements:settlement_traits/waypoint`
    - `settlements:settlement_traits/arcane`
    - `settlements:settlement_traits/ancient`
    - `settlements:settlement_traits/seafaring`

**`SettlementProfile`** is the settlement's identity card:

- `primary` -- The one and only dominant trait
- `secondary` -- Supporting trait(s)
- `flavor` -- Minor accent trait(s)
- `adjustedWeights` -- Raw scores modified by slot assignment
- `scaleTier` -- HAMLET, VILLAGE, TOWN, etc
- `estimatedPopulation` -- Random within the tier's population range
- `wealthLevel` -- Float 0.0-1.0
- `defenseLevel` -- NONE, LOW, MODERATE, or HIGH
- `seed` -- Stored for downstream reproducibility
- `historyEventIds` -- Ordered list of IDs for events that fired during the history phase

**Wealth** is consumed downstream as material quality tiers: Poor (< 0.33), Modest (0.33-0.66),
Prosperous (> 0.66). These tiers affect road materials and will eventually affect building
block palettes.

### History Event Objects

**`HistoryEventDefinition`** is the data record for a single history event loaded from a datapack:

- `id` -- Unique namespaced identifier
- `category` -- Grouping label (e.g., `"disaster"`, `"discovery"`)
- `timeHorizonMin` / `timeHorizonMax` -- Integer range defining the event's era in settlement
  history. Founding-era events use 100-199, mid-life 200-299; values below 100 are reserved for
  modpack use.
- `exclusiveTags` -- Events sharing an exclusive tag are mutually exclusive; the chronologically
  earlier event (lower time horizon) wins
- `probabilityWeight` -- Relative draw weight during weighted sampling
- `preconditions` -- Conditions evaluated against the current profile and site: minimum trait
  weights, required resource tags, required water features, minimum population
- `traitModifiers` -- Map of `TraitId` to additive float delta, applied after the event fires;
  clamped to [0.0, 1.0]
- `visualMarkers` -- Flat string tags deposited into the `VisualMarkerSet` (e.g., `"charred"`,
  `"taiga"`). Opaque to the domain; their interpretation is a content decision.
- `narrativeText` -- Human-readable flavor string, for display purposes only

**`HistoryEventResult`** carries the output of the history phase:

- `eventIds` -- Ordered list of IDs for events that fired
- `modifiedWeights` -- Final `Map<TraitId, Float>` after all modifier applications
- `visualMarkers` -- Merged `VisualMarkerSet` accumulated across all fired events. Provides a
  static `EMPTY` sentinel for the no-events passthrough case.

**`VisualMarkerSet`** is an immutable value object wrapping `Set<String>`. Supports `merge()` for
combining two sets, and exposes a static `EMPTY` sentinel. Visual markers are intentionally opaque
strings -- the domain does not define what any specific marker means; that is left to template
content authors.

### Building Objects

**`BuildingDefinition`** is the registry entry for a building type. Key fields:

- **Identity:** `id`, `displayInfo` (name, description, icon)
- **Trait association:** `traitAffinities` -- many-to-many map of TraitId to float weight. A tavern
  might have `{TRADE: 0.5, DEFENSE: 0.3}`. Empty map means universal building (house, well).
- **Eligibility:** `minimumRank` -- lowest trait slot that qualifies (PRIMARY, SECONDARY, or FLAVOR)
- **Placement:** `placementPriority` (higher = picks first), `zoneTierPreference` (min/max zone),
  `requiresRoadFrontage`, `requiresResources` (hard gate on local resources, OR logic),
  `forbiddenResources` (hard veto)
- **Template:** `footprint` (min/max width and depth), `preferredTags` -- building-specific string
  tags merged with biome tags and history visual markers during template resolution to select the
  most contextually appropriate variant
- **Spatial relationships:** `proximityAffinities` (distance-decaying bonus toward specific
  buildings), `globalAffinities` (existence-triggered probability boost from buildings or traits)
- **NPCs:** `npcProfession`, `npcCount` (planned, currently unused)

**`BuildingManifest`** is a flat, sorted list of `BuildingDefinition` instances representing the
exact buildings to place. May contain duplicates (e.g., multiple houses). Sorted with constrained
buildings first, then unconstrained, each group by priority descending.

**`BuildingAssignment`** pairs a building definition with a placed plot, facing direction, and the
trait that caused the building's inclusion. Template resolution happens later in the infrastructure
layer.

### Layout Objects

- **`RoadSegment(BlockPosition start, BlockPosition end, RoadType type)`** -- A straight-line road
  section. Types: MAIN, SECONDARY, SIDE.
- **`ZoneTier`** -- Distance-based zones from the planning center: CORE (0), DOWNTOWN (1),
  MIDTOWN (2), OUTER (3), SUBURB (4). Zone names describe location, not function -- any building
  type can occupy any zone.
- **`Plot`** -- The fundamental buildable unit: bounding region, facing direction, target Y,
  elevation delta, road frontage flag, and local resource tags computed from the terrain grid.
- **`LayoutResult(BlockPosition anchor, List<RoadSegment> roads, List<BuildingAssignment> assignments)`**
  -- The complete spatial plan.

### Pipeline Result

**`GenerationResult`** bundles all phase outputs: `SiteReport`, `SettlementProfile`,
`HistoryEventResult`, `LayoutResult`, and `generationSeed`. This is the handoff from the domain
pipeline to the Minecraft integration layer.

---

## 4. Pipeline Walkthrough

### 4.1 Survey Phase

**Purpose:** Analyze terrain to produce a factual data snapshot. No decisions are made -- only
observations and measurements.

**Input:** `TerrainGrid` + `SurveyBounds`
**Output:** `SiteReport`
**Engine:** `SurveyEngine` (domain, pure Java)

#### How It Works

The survey runs two passes over the terrain grid:

**Pass 1 -- Elevation, biome, and resource accumulation.** A single iteration over every grid cell
computes elevation statistics (min/max/mean/high point), tallies biome coverage percentages, and
accumulates weighted resource densities using per-biome survey data. Each biome has a datapack-driven
`BiomeSurveyData` record specifying its resource contributions (e.g., `dark_forest` contributes
`LUMBER: 0.9`). Water cells are also marked during this pass based on biome water type annotations.

**Pass 2 -- Water feature clustering.** If any water cells were found, a Union-Find algorithm with
4-connected adjacency groups contiguous water cells into clusters. Each cluster is classified:
edge-touching clusters use their majority biome type (RIVER or OCEAN), while enclosed clusters are
classified as LAKE. The result is a set of water feature types present in the area.

After both passes, resource tags are derived by comparing averaged densities against each tag's
activation threshold.

#### Minecraft Bridge

`TerrainGridFactory` translates Minecraft's `GenerationContext` into a domain `TerrainGrid`. It
samples height via `ChunkGenerator.getBaseHeight()`, biome via `BiomeSource.getNoiseBiome()`, and
temperature from the sampled biome. All biome identifiers are interned as `BiomeId` instances.

#### Extension Point

Per-biome resource densities and water types are defined in datapack JSON files. Adding or
overriding biome data requires no code changes -- just a JSON file in
`data/<namespace>/settlements/biomes/survey/`.

---

### 4.2 Trait Scoring Phase

**Purpose:** Evaluate every registered trait's compatibility with a specific site. Each trait scores
independently -- no trait knows about other traits.

**Input:** `SiteReport`
**Output:** `Map<TraitId, Float>` (raw scores, 0.0 to 1.0)
**Engine:** `ScoreEngine` + per-trait `TraitScorer` implementations

#### How It Works

Each trait has a `TraitScorer` -- a functional interface with a single `float score(SiteReport)`
method. The default implementation (`ConfiguredTraitScorer`) executes an additive formula driven by
a JSON configuration:

1. **Veto check:** If any veto tag is present in the site's resource tags, return 0.0 immediately.
   (e.g., FARMING is vetoed by FROZEN.)
2. **Required tag check:** If any required tag is missing, return 0.0.
3. **Additive accumulation:** Start with a base score, then add weighted contributions from:
    - Resource tag densities (positive or negative weights)
    - Water feature presence (binary bonuses)
    - Biome coverage percentages (per-biome weights)
    - Normalized elevation delta (max height - min height)
4. **Clamp** the result to [0.0, 1.0].

Negative weights are intentionally supported for penalties -- for example, FARMING can penalizes high
LUMBER density (forests crowd out farmland).

#### P0 Trait Scoring Signals

| Trait   | Primary Signals                                                                       |
|---------|---------------------------------------------------------------------------------------|
| LUMBER  | LUMBER resource density + FRESHWATER support                                          |
| FARMING | Plains/meadow/savanna biome coverage + FRESHWATER, penalizes LUMBER, vetoed by FROZEN |
| FISHING | Water feature presence + FRESHWATER/COASTAL density                                   |
| MINING  | STONE + ORE_BEARING density + elevation delta                                         |
| DEFENSE | Base score + STONE density + elevation delta                                          |

Only traits with registered scorers appear in the output. Missing traits are excluded entirely, not
emitted as 0.0.

#### Extension Points

The `TraitScorer` interface is the open extension seam. The default additive model covers most
cases via JSON configuration, but future special traits (e.g., ARCANE, ANCIENT) can provide custom
Java implementations with non-linear or context-dependent logic without modifying the pipeline.

Scorer configurations are datapack JSON files in `data/<namespace>/settlements/traits/scoring/`.

---

### 4.3 Profile Sampling Phase

**Purpose:** Turn raw scores into a concrete settlement identity. This is where randomness lives --
the same terrain can produce different settlements on different seeds.

**Input:** `Map<TraitId, Float>` + `Random`
**Output:** `SettlementProfile`
**Engine:** `SamplingEngine` (domain, pure Java, zero constructor dependencies)

#### How It Works

1. **Scale tier selection.** Weighted random: 40% Village, 30% Town, 30% Hamlet.

2. **Trait slot count.** Random integer within the tier's slot range (e.g., Village: 2-4 slots).

3. **Trait drawing.** Weighted random sampling without replacement from traits with positive scores.
   After each draw, the drawn trait is removed and weights are re-normalized. If the pool exhausts
   before all slots are filled, drawing stops early.

4. **Slot assignment** from the draw order:
    - 1 draw: 1 primary
    - 2 draws: 1 primary, 1 secondary
    - 3 draws: 1 primary, 1 secondary, 1 flavor
    - 4 draws: 1 primary, 2 secondary, 1 flavor
    - 5 draws: 1 primary, 2 secondary, 2 flavor

5. **Metadata derivation:**
    - Population: random within tier range
    - Wealth: `random.nextFloat()` (pure random for now)
    - Defense level: derived from the DEFENSE trait's slot (absent -> NONE, flavor -> LOW,
      secondary -> MODERATE, primary -> HIGH)
    - Adjusted weights: copy of raw scores with the primary trait boosted by 1.5x

6. **Downstream seed.** A `random.nextLong()` is stored on the profile for deterministic
   reproducibility in later phases.

#### Fallback Behavior

If all trait scores are zero or no traits have positive scores, the engine falls back to FARMING
as the primary trait with a synthetic score of 1.0. This guarantees every settlement has at least
one trait and non-zero adjusted weights.

---

### 4.4 History Event Phase

**Purpose:** Roll a set of history events that shaped the settlement before the player arrived.
Events modify trait weights and deposit visual markers that downstream phases use to select
contextually appropriate building variants.

**Input:** `SettlementProfile` + `SiteReport` + `Random`
**Output:** Updated `SettlementProfile` + `HistoryEventResult`
**Engine:** `HistoryEventEngine` (domain, pure Java)

#### How It Works

1. **Eligible event filtering.** All registered events are checked against their preconditions
   (minimum trait weights, required resource tags, required water features, minimum population).
   Events whose preconditions are not satisfied are excluded. An empty eligible pool is a valid
   passthrough -- the engine returns `HistoryEventResult.EMPTY` without error.

2. **Weighted sampling without replacement.** Up to 32 candidate events are drawn from the
   eligible pool using their `probabilityWeight` values. Each draw removes the selected event
   from the pool, preventing duplicates.

3. **Time horizon assignment.** Each drawn event receives a concrete era value via random roll
   within its `[timeHorizonMin, timeHorizonMax]` range.

4. **Chronological ordering.** Events are sorted ascending by time horizon. Ties break by
   event ID for determinism.

5. **Exclusive tag filtering.** Events are walked in chronological order. If an event claims
   an exclusive tag already claimed by an earlier event, it is dropped. Earlier events win.

6. **Scale truncation.** Survivors are capped to `scaleTier.maxHistoryEvents()`. Larger
   settlements accumulate richer histories.

7. **Trait modifier application.** Modifiers are applied in chronological order. Each modifier
   is an additive delta to an `adjustedWeights` entry, clamped to [0.0, 1.0]. Unknown trait IDs
   in a modifier map are skipped with a warning.

8. **Result assembly.** The engine returns a `HistoryEventResult` containing the fired event IDs,
   the final modified weight map, and the merged `VisualMarkerSet` accumulated across all events.

#### Integration

After the history phase, `GenerationPipeline` reconstructs `SettlementProfile` with the updated
`adjustedWeights` and populated `historyEventIds`. The updated profile flows unchanged into the
Building Manifest phase. The `HistoryEventResult` is carried in `GenerationResult` and consumed
by the Minecraft integration layer when constructing `TemplateResolutionContext`.

---

### 4.5 Building Registry and Manifest

**Purpose:** Determine the catalog of available buildings (registry) and the specific buildings this
settlement needs (manifest).

#### Building Registry

The registry is a JSON-driven building catalog loaded at server startup via datapack reload. Each
building is defined by a JSON file specifying its identity, trait affinities, placement constraints,
footprint dimensions, and spatial relationships.

The domain layer exposes a `BuildingRegistry` interface with queries for all buildings, constrained
vs unconstrained partitions, and trait-filtered lookups. The infrastructure layer implements this via
`BuildingDefinitionDataManager`, following the same `SimpleJsonResourceReloadListener` pattern used
by biome survey data and trait scorers.

The current P0 registry includes 15 buildings across 5 traits plus universals:

| Building         | Traits  | Constrained By     | Notes                                               |
|------------------|---------|--------------------|-----------------------------------------------------|
| town_hall        | --      | --                 | Mandatory hub                                       |
| tavern           | --      | --                 | Village+ only // TODO: this is currently hard coded |
| market_stall     | --      | --                 | Count scales with tier                              |
| house            | --      | --                 |                                                     |
| well             | --      | --                 |                                                     |
| sawmill          | LUMBER  | LUMBER resource    | Near forests                                        |
| log_storage      | LUMBER  | LUMBER resource    | Near forests                                        |
| farmhouse        | FARMING | --                 |                                                     |
| barn             | FARMING | --                 |                                                     |
| dock             | FISHING | FRESHWATER/COASTAL | Near water                                          |
| fish_drying_rack | FISHING | FRESHWATER/COASTAL | Near water                                          |
| mine_entrance    | MINING  | STONE resource     | Near mountains                                      |
| smelter          | MINING  | --                 |                                                     |
| watchtower       | DEFENSE | --                 |                                                     |
| barracks         | DEFENSE | --                 |                                                     |

#### Building Manifest Calculator

Given a `SettlementProfile` and `BuildingRegistry`, the manifest calculator determines exactly which
buildings the settlement gets:

1. **Universal buildings.** Always included: town_hall (1), tavern (1, Village+ only), market_stalls
   (count scales with tier: 0 for Hamlet, 1-3 for Village, 2-5 for Town).

2. **Trait buildings.** For each trait in the profile, draw buildings weighted by
   `traitAffinity * placementPriority`. Count depends on slot: PRIMARY -> 2-4, SECONDARY -> 1-2,
   FLAVOR -> 0-1. Drawing is without replacement for distinct types, but the same type can appear
   multiple times if the count exceeds available types.

3. **Houses.** Fill the remaining slots up to the tier's max building count with house definitions.

4. **Sort.** Constrained buildings first (they need specific terrain), then unconstrained. Within
   each partition, sorted by placement priority descending.

This ordering is critical: the layout engine places buildings in manifest order, so constrained
buildings get first pick of the limited terrain-matched positions.

---

### 4.6 Layout and Assignment

**Purpose:** Decide where everything goes -- roads, buildings, zones -- producing the complete
spatial plan. This is the most algorithmically complex phase.

**Input:** `SiteReport` + `SettlementProfile` + `BuildingManifest` + `BiomeSurveyLookup`
**Output:** `LayoutResult`
**Engine:** `SettlementLayoutEngine` orchestrating multiple subcomponents

#### Design Philosophy

The key insight is that **buildings drive road generation, not the other way around.** Important
buildings are placed first as "nuclei," then roads are generated to connect them, and remaining
buildings fill in along those roads. This produces organic layouts where roads exist because they
connect meaningful destinations.

#### Four Sub-Phases

**Phase A -- Pre-Road Placements (Nuclei)**

Before any roads exist, key buildings are placed to establish the settlement skeleton:

1. **Hub placement.** The mandatory town_hall is placed near the anchor via radial search. Its center
   becomes the planning center, which roots all subsequent spatial decisions.

2. **Constrained buildings.** Buildings with resource requirements (sawmill needs LUMBER, dock needs
   FRESHWATER/COASTAL) are placed via spiral search outward from the anchor. At each candidate
   position, `LocalResourceScanner` checks whether the terrain within a small radius provides the
   required resources. Valid candidates are selected by proximity to the planning center. Buildings
   that can't find valid terrain are demoted to later phases (losing their resource constraint).

3. **Signature buildings.** For each trait in the profile, the highest-affinity building not already
   placed gets positioned at a moderate distance (10-25 blocks) from the anchor. These create
   recognizable "districts" around the settlement.

4. **Bonus nuclei.** a few additional unconstrained buildings are placed at random valid positions for
   road network redundancy.

All Phase A placements become "nuclei" -- the nodes that the road network will connect.

**Phase B -- Road Network Generation**

(This is currently a work in progress feature)
The road generator connects the planning center and all nuclei:

1. **Minimum spanning tree.** Prim's algorithm computes the MST over all nuclei using
   distance-squared weights. This guarantees all nuclei are reachable.

2. **Extra edges.** a few random non-MST edges are added for redundancy, creating intersections and
   alternate routes. Only edges shorter than 2x the longest MST edge qualify.

3. **Edge classification.** Edges touching the planning center become MAIN roads, other MST edges
   become SECONDARY, and extra edges become SIDE roads.

4. **Midpoint displacement.** Each straight edge is curved via perpendicular offset at the midpoint.
   The offset scales with edge length. Longer edges (> 40 blocks) recurse once on each half,
   producing 2-4 segments per logical edge. This creates organic, natural-feeling road paths.

5. **Grid registration.** Road segments are rasterized and registered in the placement grid. Roads
   act as inter-building padding -- a building can sit flush against a road edge, and two buildings
   on opposite sides of a road use the road width itself as their separation.

**Phase C -- Road-Guided Infill**

Remaining buildings are placed alongside generated roads:

1. Walk along each road segment (MAIN first, then SECONDARY, then SIDE), attempting placements on
   both sides.
2. For each candidate position, roll footprint dimensions from the building's constraint, check
   for conflicts, and validate terrain flatness.
3. Buildings face the road. Zone is assigned based on road-graph distance from the planning center
   (computed via BFS flood-fill before Phase C begins).
4. Higher-priority buildings claim spots on earlier (more central) road segments.

Zone thresholds vary by scale tier:

| Zone     | Hamlet | Village | Town  |
|----------|--------|---------|-------|
| CORE     | 0-5    | 0-8     | 0-10  |
| DOWNTOWN | 5-15   | 8-25    | 10-30 |
| MIDTOWN  | 15-25  | 25-45   | 30-55 |
| OUTER    | 25-32  | 45-56   | 55-80 |
| SUBURB   | 32+    | 56+     | 80+   |

**Phase D -- Scatter**

Buildings that couldn't be placed along roads get scattered into remaining space:

1. For each remaining building, try up to 20 random positions within the build area.
2. No road frontage. Buildings face toward the planning center.
3. All scatter placements are assigned zone SUBURB. // TODO: confirm if this is correct
4. If all 20 attempts fail, the building is dropped (logged as a warning).

These are farms, isolated homes, and outpost buildings at the settlement fringe.

#### Key Infrastructure Components

- **`LocalResourceScanner`** -- Reuses survey logic at a plot scale. Scans the terrain grid within a
  small radius around a candidate position to determine what resources are locally available. Used by
  constrained placement and by Phase C/D for per-plot resource tagging.

- **`PlacementGrid`** -- Tracks all occupied regions (buildings and roads). Enforces two conflict
  rules: buildings require a minimum spacing buffer of 4 blocks between each other, but buildings
  can sit flush against roads. Simple list scan implementation -- with ~50 buildings, conflict
  checking is sub-microsecond.

- **`PlacementValidator`** -- Centralized validation: build-area containment, terrain flatness
  (elevation delta <= 5 blocks within footprint), spacing conflicts, resource requirements, and
  water avoidance.

---

## 5. Minecraft Integration

**Purpose:** Bridge the abstract `GenerationResult` into the Minecraft world by resolving templates,
emitting structure pieces, and registering the settlement as a naturally-spawning structure.

### Template Resolution

The domain layer defines a `TemplateResolver` interface. The infrastructure layer implements it as
`NbtTemplateResolver`, which maintains a singleton in-memory catalog of `.nbt` files:

1. At server startup, scan `data/settlements/structure/buildings/` for `.nbt` files. Each template
   should have a `.meta.json` companion declaring its `building_definition` (the building
   type it belongs to) and `tags` (a flat string set for variant matching). Templates without a
   `.meta.json` use a filename-derived fallback and are treated as untagged.
2. Read each file's footprint via `StructureTemplate.getSize()`.
3. Cache as `TemplateEntry(ResourceLocation, buildingDefinitionId, width, depth, tags)`, indexed
   by building definition ID.

On `resolve()`, given a `TemplateResolutionContext` carrying biome tags and visual markers:

1. Look up the building's catalog entry by definition ID; filter by footprint dimensions.
2. Merge `BuildingDefinition.preferredTags`, biome tags, and visual markers into a combined
   request tag set.
3. Apply tiered matching:
    - **Full match** -- If any entries carry all requested tags, pick randomly from that group.
    - **Partial match** -- Otherwise, pick randomly from the group with the highest tag overlap
      count.
    - **Untagged fallback** -- If no tagged entries qualify, pick randomly from entries with no
      tags.
    - **No fallback** -- If the untagged pool is also empty, return `Optional.empty()` with a
      logged warning. The catalog validates at startup that each building type has at least one
      untagged template, so this branch should not occur under normal operation.

The catalog is eagerly warmed at server startup and invalidated/rebuilt on `/reload`.

### Structure Pieces

**`SettlementBuildingPiece`** -- Extends `TemplateStructurePiece`. Pastes the resolved `.nbt`
template at the plot's target Y position with rotation derived from the building's facing direction.
No terraforming (yet) -- buildings paste at surface level, which may result in floating or clipping on
uneven terrain (acceptable for now; addressed in future work).

**`SettlementRoadPiece`** -- Extends `StructurePiece` directly (not template-based). Places road
blocks along each segment using Bresenham line rasterization. Each block's Y is determined at
`postProcess()` time by reading the chunk's heightmap, so roads follow the terrain surface.

Road block material is selected according to the wealth level:

- Poor (< 0.33): Dirt path
- Modest (0.33-0.66): Gravel
- Prosperous (> 0.66): Cobblestone

Road width varies by type: MAIN = 9 blocks, SECONDARY = 5 blocks, SIDE = 3 blocks.

### Structure Registration

The settlement is registered as a proper NeoForge structure set:

- `SettlementStructure` extends `SettlementsStructure` with a codec for worldgen integration
- Structure set JSON configures random spread placement (spacing: 34, separation: 8 -- matching
  vanilla village patterns)
- A biome tag (`has_settlement`) controls where settlements can spawn (plains, forest, savanna,
  taiga, meadow, etc.; excluding ocean, desert, mushroom fields, nether, end)

### Generation Flow

When Minecraft's world-gen triggers settlement placement:

1. Derive seed from world seed XOR chunk position hash.
2. Build `SurveyBounds` from chunk position and default radius.
3. Create `TerrainGrid` via `TerrainGridFactory.fromGenerationContext()`.
4. Fetch registry singletons (biome survey data, trait scorers, building definitions).
5. Run `GenerationPipeline.generate()` to produce `GenerationResult`.
6. Build a `TemplateResolutionContext` from the dominant biome's template tags (looked up via
   `BiomeSurveyDataManager`) and the history phase's `VisualMarkerSet`. Resolve templates for
   each `BuildingAssignment` by passing this context to the warmed `NbtTemplateResolver`.
7. Emit `SettlementBuildingPiece` for each resolved assignment.
8. Emit `SettlementRoadPiece` for each road segment.

---

## 6. Datapack Extension Points

The generation system is data-driven from day one. All configuration lives in JSON files loadable
via Minecraft's datapack system, enabling modpack authors and addon developers to customize behavior
without touching code.

### Biome Survey Data

**Path:** `data/settlements/biomes/survey/<biome_namespace>/<biome_path>.json`

Defines per-biome resource contributions and water type. Example:

```json
{
  "biome": "minecraft:dark_forest",
  "resource_densities": {
    "LUMBER": 0.9
  },
  "water_type": null,
  "template_tags": [
    "forest"
  ]
}
```

`template_tags` is a list of string tags that flow into the `TemplateResolutionContext` when this
biome is dominant at the settlement site, influencing which template variants are selected.

The mod ships with defaults for 40+ vanilla overworld biomes. Unlisted biomes default to zero resources
and no water type.

### Trait Scorer Configurations

**Path:** `data/settlements/traits/scoring/<trait>.json`

Defines the additive scoring formula for a trait. Supports base scores, resource tag weights
(positive and negative), water feature bonuses, per-biome weights, and elevation delta contribution.

### Trait Definitions

**Path:** `data/<namespace>/settlements/traits/definitions/<trait>.json`

Defines a trait's identity for the `TraitRegistry`. Each file specifies the trait's namespaced ID
and display metadata. The mod ships with 15 built-in trait definitions covering all P0, P1, and P2
traits. Custom traits can be added by third-party datapacks.

### History Events

**Path:** `data/<namespace>/settlements/history/events/<event>.json`

Defines a history event. Example:

```json
{
  "id": "settlements:history/great_fire",
  "category": "disaster",
  "time_horizon_min": 100,
  "time_horizon_max": 199,
  "exclusive_tags": [
    "fire"
  ],
  "probability_weight": 1.0,
  "preconditions": {
    "min_trait_weights": {
      "settlements:settlement_traits/lumber": 0.3
    },
    "required_resource_tags": [],
    "required_water_features": [],
    "min_population": 0
  },
  "trait_modifiers": {
    "settlements:settlement_traits/lumber": -0.3
  },
  "visual_markers": [
    "charred_ruins"
  ],
  "narrative_text": "A great fire swept through the settlement in its early years."
}
```

Time horizon bands: founding era 100-199, mid-life 200-299. Values below 100 are reserved for
modpack or datapack use.

### Building Definitions

**Path:** `data/settlements/buildings/definitions/<building>.json`

Defines a building's identity, trait affinities, placement rules, footprint constraints, and spatial
relationships. The building JSON schema supports the full `BuildingDefinition` contract including
proximity affinities and global affinities.

### NBT Templates

**Path:** `data/structure/buildings/<template>.nbt`

Structure templates in NBT format. Each template may have an optional `.meta.json` companion file:

```json
{
  "building_definition": "settlements:house",
  "tags": [
    "taiga"
  ]
}
```

`building_definition` declares which building type this template belongs to. `tags` is the flat
string set used for variant matching. Templates without a `.meta.json` use a filename-derived
fallback and are treated as untagged -- they serve as the catch-all fallback in the resolver's
tiered matching.

### Reload Lifecycle

All datapack managers implement `SimpleJsonResourceReloadListener` and are registered in
`DataReloadEvents`. On `/reload`:

1. Biome survey data, trait scorer configs, building definitions, trait definitions, and history
   event definitions are reparsed from JSON.
2. The NBT template resolver invalidates its catalog and rebuilds from the template directory.
3. Subsequent settlement generation uses the updated data.

---

## 7. Future Work

These features are architecturally planned and have hooks in the existing data model. Each
subsection provides enough conceptual foundation to seed a detailed design document.

### Compound Buildings

A compound is a cluster of 2-3 sub-structures with internal spatial rules:

- **Primary (front):** Faces the road. The public-facing structure (shop, farmhouse).
- **Secondary (back/side):** The workspace. Adjacent to primary, not road-facing (forge, barn).
- **Tertiary (back):** Storage, living quarters, or auxiliary (quarters, storage shed).

**Conceptual model:** Compound buildings would be defined as a parent `BuildingDefinition` with a
list of child definitions and relative placement rules (direction from parent, required adjacency,
shared plot vs separate plot). The layout engine would attempt to place children immediately after
placing the parent, using the parent's facing direction to determine child positions.

The current system already achieves *approximate* clustering via `ProximityAffinity` (distance-
decaying bonus), which causes related buildings to gravitate toward each other. Formal compounds
would make this explicit and guarantee adjacency.

### NPC Spawning

`BuildingDefinition` already carries `npcProfession` and `npcCount` fields (currently unused). The
spawning system would iterate placed `BuildingAssignment`s, spawn villagers with the specified
profession at or near the building's plot, and potentially assign them a home plot.

**Conceptual model:** NPCs would be spawned during or after the Build phase. Each building that
specifies an `npcProfession` and `npcCount > 0` would trigger villager spawning with the
appropriate profession. Population would be bounded by `SettlementProfile.estimatedPopulation`.
Houses without explicit professions would spawn generic villagers. The profession system would need
a mapping from settlement-specific profession IDs to Minecraft's `VillagerProfession` registry.

### Block Palette System

Currently, all buildings use the same oak-plank placeholder templates regardless of biome or wealth.
A palette system would swap blocks based on context:

**Conceptual model:** A `BlockPalette` maps abstract block roles (wall, floor, roof, accent,
pillar) to concrete Minecraft blocks. Palettes would be selected by a combination of biome family
(forest -> oak/spruce, desert -> sandstone, taiga -> spruce/stone) and wealth level (poor -> raw
logs/cobblestone, prosperous -> stone brick/dark oak). During the Build phase, template blocks
tagged with abstract roles would be replaced according to the selected palette.

This interacts with the wealth level already on `SettlementProfile` and the biome distribution
already on `SiteReport`. Templates would need to use structure block data markers or a block
substitution table.

### City Scale Tier

Cities would be the largest settlement scale, using a multi-anchor expansion model:

**Conceptual model:** A city is composed of multiple districts, each essentially a sub-village with
its own trait emphasis and planning center. The primary district generates normally. Additional
districts spawn at controlled distances, connected by major roads. Each district has its own zone
system, but the overall city shares infrastructure (walls, main roads, a central market district).

This requires multi-anchor support in the layout engine, district-level trait specialization in
the sampling engine, and inter-district road generation in the road network generator. The `ScaleTier`
enum would gain a CITY entry with appropriate ranges.

### P1 and P2 Traits

**P1 -- Economy and Social:**

- PASTORAL: Grazing animals, shepherd buildings. Triggered by grassland biomes.
- TRADE: Market-focused, wider roads, signposts. Triggered by crossroads potential.
- HONEY: Apiaries, flower gardens. Triggered by FLORAL resource tag.
- CRAFT: Workshops, artisan buildings. Triggered by combined resource availability.
- SPIRITUAL: Shrines, temples, sacred groves. Triggered by specific biome combinations.
- SCHOLARLY: Libraries, scriptoriums. Triggered by settlement size and trait diversity.
- WAYPOINT: Inns, stables, signpost networks. Triggered by road network potential.

**P2 -- Rare and Special:**

- ARCANE: Enchanting towers, mystical gardens. Would use a custom Java scorer with non-linear logic
  (e.g., checking for specific biome adjacency patterns or rare resource combinations).
- ANCIENT: Ruins, archaeological sites, weathered stone. Would check for specific terrain features
  suggesting age (elevated positions, isolated plateaus).
- SEAFARING: Harbors, shipyards, lighthouses. Would require significant COASTAL coverage and
  specific coastal terrain geometry.

Each trait needs a scorer (JSON-configured for most, custom Java for ARCANE/ANCIENT), building
definitions with trait affinities, and potentially new resource tags.

### Advanced Road Generation

Current roads use midpoint displacement for organic curves. Future improvements:

- **Bezier curves** for smoother road paths
- **Terrain-following Y** with stairs and slabs on slopes
- **Footpaths** connecting scatter-phase buildings to the nearest road
- **Bridge structures** over water features
- **Road material variety** beyond the three-tier wealth system (biome-specific materials)

### Settlement Persistence

Settlements currently exist only as structure pieces in the world-gen system. A persistence layer
using Minecraft's `SavedData` would enable:

- Runtime queries ("is this position inside a settlement?", "which settlement is nearest?")
- Villager behavior integration (villagers know their settlement's traits and buildings)
- Dynamic settlement modification (building upgrades, population changes, event triggers)
- Map/minimap integration

### Other Planned Features

- **Terraforming:** Flatten plots to buildable grade before building placement (currently buildings
  paste at surface Y with no terrain modification)
- **Detailing pass:** Post-process decorative elements -- fences, lanterns, carts, flower pots,
  item frames, banners -- that add life to placed structures
- **Biome-specific template content:** The tag-matching template resolution system is implemented
  and wired end-to-end. What remains is content -- actual tagged `.nbt` variant files for desert,
  taiga, and other biome families. The infrastructure is ready; the art pass is pending.
- **Configuration file:** Server-operator tuning parameters for generation frequency, scale
  distribution weights, spacing, and feature toggles

### Known issues (bug checklist)

1. [CRITICAL] Minecraft crashes when townhall cannot be placed in the world at world-gen time
2. We only generate in a small allowlist of biomes, ref
   src\main\resources\data\settlements\tags\worldgen\biome\has_settlement.json
