# Settlement Generation — Pipeline Walkthrough

## Table of Contents

- [4.1 Survey Phase](#41-survey-phase)
- [4.2 Trait Scoring Phase](#42-trait-scoring-phase)
- [4.3 Profile Sampling Phase](#43-profile-sampling-phase)
- [4.4 History Event Phase](#44-history-event-phase)
- [4.5 Building Registry and Manifest](#45-building-registry-and-manifest)
- [4.6 Layout and Assignment](#46-layout-and-assignment)

---

## 4.1 Survey Phase

**Purpose:** Analyze terrain to produce a factual data snapshot. No decisions are made — only
observations and measurements.

**Input:** `TerrainGrid` + `SurveyBounds`  
**Output:** `SiteReport`  
**Engine:** `SurveyEngine` (`domain/`, pure Java)

### How It Works

The survey runs two passes over the terrain grid:

**Pass 1 — Elevation, biome, and resource accumulation.** A single iteration over every grid cell
computes elevation stats (min/max/mean/high point), tallies biome coverage percentages, and
accumulates weighted resource densities using per-biome `BiomeSurveyData` (datapack-driven). Water
cells are marked based on biome water type annotations.

**Pass 2 — Water feature clustering.** If water cells were found, a Union-Find algorithm with
4-connected adjacency groups contiguous water cells into clusters. Edge-touching clusters are
classified as RIVER or OCEAN by majority biome type; enclosed clusters become LAKE.

After both passes, `ResourceTag`s are derived by comparing averaged densities against each tag's
activation threshold:

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

Resource tags are the "observation" layer between raw biome data and trait scoring — they answer
"what does this plot of land provide?"

### Minecraft Bridge

`TerrainGridFactory` translates Minecraft's `GenerationContext` into a domain `TerrainGrid`. It
samples height via `ChunkGenerator.getBaseHeight()`, biome via `BiomeSource.getNoiseBiome()`, and
temperature from the sampled biome. Biome identifiers are interned as `BiomeId` instances.

### Extension Point

Per-biome resource densities and water types are defined in datapack JSON. See
[datapack_extensions.md § Biome Survey Data](datapack_extensions.md#biome-survey-data).

---

## 4.2 Trait Scoring Phase

**Purpose:** Evaluate every registered trait's compatibility with a site. Each trait scores
independently — no trait knows about other traits.

**Input:** `SiteReport`  
**Output:** `Map<TraitId, Float>` (raw scores, 0.0–1.0)  
**Engine:** `ScoreEngine` + per-trait `TraitScorer` implementations

### How It Works

Each trait has a `TraitScorer` (functional interface: `float score(SiteReport)`). The default
implementation (`ConfiguredTraitScorer`) executes a JSON-driven additive formula:

1. **Veto check:** If any veto tag is present in the site's resource tags, return 0.0 immediately.
   (e.g., FARMING is vetoed by FROZEN.)
2. **Required tag check:** If any required tag is missing, return 0.0.
3. **Additive accumulation:** Start with a base score, add weighted contributions from resource tag
   densities (positive or negative), water feature presence, per-biome coverage percentages, and
   normalized elevation delta.
4. **Clamp** result to [0.0, 1.0].

Negative weights are supported for penalties — FARMING penalizes high LUMBER density (forests crowd
out farmland).

### P0 Trait Scoring Signals

| Trait   | Primary Signals                                                                       |
|---------|---------------------------------------------------------------------------------------|
| LUMBER  | LUMBER resource density + FRESHWATER support                                          |
| FARMING | Plains/meadow/savanna biome coverage + FRESHWATER, penalizes LUMBER, vetoed by FROZEN |
| FISHING | Water feature presence + FRESHWATER/COASTAL density                                   |
| MINING  | STONE + ORE_BEARING density + elevation delta                                         |
| DEFENSE | Base score + STONE density + elevation delta                                          |

Only traits with registered scorers appear in the output. Missing traits are excluded entirely, not
emitted as 0.0.

### Extension Point

The `TraitScorer` interface is the open seam. The default additive model covers most cases via JSON
config; future special traits (e.g., ARCANE) can provide custom Java implementations. See
[datapack_extensions.md § Trait Scorer Configurations](datapack_extensions.md#trait-scorer-configurations).

---

## 4.3 Profile Sampling Phase

**Purpose:** Turn raw scores into a concrete settlement identity. This is where randomness lives —
the same terrain can produce different settlements on different seeds.

**Input:** `Map<TraitId, Float>` + `Random`  
**Output:** `SettlementProfile`  
**Engine:** `SamplingEngine` (`domain/`, pure Java, zero constructor dependencies)

### How It Works

1. **Scale tier selection.** Weighted random: 40% Village, 30% Town, 30% Hamlet.
2. **Trait slot count.** Random integer within the tier's slot range (e.g., Village: 2–4 slots).
3. **Trait drawing.** Weighted random sampling without replacement. After each draw, the drawn trait
   is removed and weights re-normalized. Drawing stops early if the pool exhausts.
4. **Slot assignment** from draw order:
    - 1 draw: 1 primary
    - 2 draws: 1 primary, 1 secondary
    - 3 draws: 1 primary, 1 secondary, 1 flavor
    - 4 draws: 1 primary, 2 secondary, 1 flavor
    - 5 draws: 1 primary, 2 secondary, 2 flavor
5. **Metadata derivation:**
    - Population: random within tier range
    - Wealth: `random.nextFloat()` (pure random for now)
    - Defense level: derived from DEFENSE trait's slot (absent→NONE, flavor→LOW, secondary→MODERATE,
      primary→HIGH)
    - Adjusted weights: copy of raw scores with primary trait boosted 1.5x
6. **Downstream seed.** A `random.nextLong()` is stored on the profile for deterministic
   reproducibility in later phases.

### Fallback Behavior

If all trait scores are zero, the engine falls back to FARMING as primary with a synthetic score of
1.0. Every settlement is guaranteed at least one trait.

---

## 4.4 History Event Phase

**Purpose:** Roll history events that shaped the settlement before the player arrived. Events modify
trait weights and deposit visual markers used downstream for template variant selection.

**Input:** `SettlementProfile` + `SiteReport` + `Random`  
**Output:** Updated `SettlementProfile` + `HistoryEventResult`  
**Engine:** `HistoryEventEngine` (`domain/`, pure Java)

### How It Works

1. **Eligible event filtering.** All registered events are checked against preconditions (minimum
   trait weights, required resource tags, required water features, minimum population). An empty
   eligible pool is a valid passthrough — returns `HistoryEventResult.EMPTY`.
2. **Weighted sampling without replacement.** Up to 32 candidate events are drawn using
   `probabilityWeight` values.
3. **Time horizon assignment.** Each drawn event receives a concrete era value via random roll
   within its `[timeHorizonMin, timeHorizonMax]` range.
4. **Chronological ordering.** Events sorted ascending by time horizon. Ties break by event ID.
5. **Exclusive tag filtering.** Events walked in order; if an event claims an exclusive tag already
   claimed by an earlier event, it is dropped. Earlier events win.
6. **Scale truncation.** Survivors capped to `scaleTier.maxHistoryEvents()`. Larger settlements
   accumulate richer histories.
7. **Trait modifier application.** Additive deltas applied in chronological order, clamped to
   [0.0, 1.0]. Unknown trait IDs are skipped with a warning.
8. **Result assembly.** Returns fired event IDs, final modified weight map, and merged
   `VisualMarkerSet`.

### Integration

After this phase, `GenerationPipeline` reconstructs `SettlementProfile` with updated
`adjustedWeights` and populated `historyEventIds`. The `HistoryEventResult` is carried in
`GenerationResult` and consumed by the MC integration layer when constructing
`TemplateResolutionContext`.

---

## 4.5 Building Registry and Manifest

**Purpose:** Determine the catalog of available buildings (registry) and the specific buildings this
settlement needs (manifest).

### Building Registry

The registry is a JSON-driven catalog loaded at server startup via datapack reload. The domain layer
exposes a `BuildingRegistry` interface; the infrastructure layer implements it via
`BuildingDefinitionDataManager`.

The P0 registry includes 15 buildings across 5 traits plus universals:

| Building         | Traits  | Constrained By     | Notes                                |
|------------------|---------|--------------------|--------------------------------------|
| town_hall        | --      | --                 | Mandatory hub                        |
| tavern           | --      | --                 | Village+ only (currently hard-coded) |
| market_stall     | --      | --                 | Count scales with tier               |
| house            | --      | --                 |                                      |
| well             | --      | --                 |                                      |
| sawmill          | LUMBER  | LUMBER resource    | Near forests                         |
| log_storage      | LUMBER  | LUMBER resource    | Near forests                         |
| farmhouse        | FARMING | --                 |                                      |
| barn             | FARMING | --                 |                                      |
| dock             | FISHING | FRESHWATER/COASTAL | Near water                           |
| fish_drying_rack | FISHING | FRESHWATER/COASTAL | Near water                           |
| mine_entrance    | MINING  | STONE resource     | Near mountains                       |
| smelter          | MINING  | --                 |                                      |
| watchtower       | DEFENSE | --                 |                                      |
| barracks         | DEFENSE | --                 |                                      |

### Building Manifest Calculator

Given a `SettlementProfile` and `BuildingRegistry`:

1. **Universal buildings.** Always included: town_hall (1), tavern (1, Village+ only),
   market_stalls (0 for Hamlet, 1–3 for Village, 2–5 for Town).
2. **Trait buildings.** For each trait in the profile, draw buildings weighted by
   `traitAffinity * placementPriority`. Count by slot: PRIMARY→2–4, SECONDARY→1–2, FLAVOR→0–1.
   Drawing is without replacement for distinct types.
3. **Houses.** Fill remaining slots up to the tier's max building count.
4. **Sort.** Constrained buildings first (need specific terrain), then unconstrained. Within each
   partition, sorted by placement priority descending.

This ordering is critical: the layout engine places buildings in manifest order, so constrained
buildings get first pick of limited terrain-matched positions.

---

## 4.6 Layout and Assignment

**Purpose:** Decide where everything goes — roads, buildings, zones — producing the complete spatial
plan.

**Input:** `SiteReport` + `SettlementProfile` + `BuildingManifest` + `BiomeSurveyLookup`  
**Output:** `LayoutResult`  
**Engine:** `SettlementLayoutEngine` orchestrating multiple subcomponents

### Design Philosophy

**Buildings drive road generation, not the other way around.** Key buildings are placed first as
"nuclei," roads are generated to connect them, and remaining buildings fill in along those roads.
This produces organic layouts where roads exist because they connect meaningful destinations.

### Phase A — Pre-Road Placements (Nuclei)

Before any roads exist, key buildings establish the settlement skeleton:

1. **Hub placement.** The mandatory town_hall is placed near the anchor via radial search. Its center
   becomes the planning center, which roots all subsequent spatial decisions.
2. **Constrained buildings.** Buildings with resource requirements (sawmill→LUMBER, dock→FRESHWATER/
   COASTAL) placed via spiral search outward from the anchor. `LocalResourceScanner` checks terrain
   within a small radius. Buildings that can't find valid terrain are demoted to later phases.
3. **Signature buildings.** For each trait in the profile, the highest-affinity building not already
   placed gets positioned 10–25 blocks from the anchor. These create recognizable "districts."
4. **Bonus nuclei.** A few additional unconstrained buildings at random valid positions for road
   network redundancy.

All Phase A placements become "nuclei" — nodes the road network will connect.

### Phase B — Road Network Generation

*(Currently a work in progress)*

1. **MST.** Prim's algorithm over all nuclei using distance-squared weights. Guarantees all nuclei
   are reachable.
2. **Extra edges.** A few random non-MST edges for redundancy (intersections, alternate routes).
   Only edges shorter than 2x the longest MST edge qualify.
3. **Edge classification.** Edges touching the planning center → MAIN; other MST edges →
   SECONDARY; extra edges → SIDE.
4. **Midpoint displacement.** Each edge is curved via perpendicular offset at the midpoint. Edges
   > 40 blocks recurse once per half (2–4 segments per logical edge) for organic paths.
5. **Grid registration.** Road segments rasterized into the placement grid. Buildings can sit flush
   against a road; two buildings on opposite sides use road width as their separation.

### Phase C — Road-Guided Infill

1. Walk each road segment (MAIN first, then SECONDARY, then SIDE), attempting placements on both
   sides.
2. For each candidate, use the building definition's fixed footprint, check conflicts, validate terrain flatness.
3. Buildings face the road. Zone assigned by BFS road-graph distance from the planning center.
4. Higher-priority buildings claim spots on earlier (more central) road segments.

Zone thresholds by scale tier:

| Zone     | Hamlet | Village | Town  |
|----------|--------|---------|-------|
| CORE     | 0-5    | 0-8     | 0-10  |
| DOWNTOWN | 5-15   | 8-25    | 10-30 |
| MIDTOWN  | 15-25  | 25-45   | 30-55 |
| OUTER    | 25-32  | 45-56   | 55-80 |
| SUBURB   | 32+    | 56+     | 80+   |

### Phase D — Scatter

Buildings that couldn't be placed along roads:

1. Up to 20 random positions tried within the build area. No road frontage; buildings face the
   planning center.
2. All scatter placements assigned zone SUBURB.
3. If all 20 attempts fail, the building is dropped (logged as a warning).

### Key Infrastructure Components

| Component              | Role                                                                                                                                                                       |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LocalResourceScanner` | Scans terrain grid within a small radius to determine locally available resources. Used by constrained placement and Phase C/D per-plot resource tagging.                  |
| `PlacementGrid`        | Tracks all occupied regions (buildings + roads). Buildings require 4-block minimum spacing from each other; can sit flush against roads.                                   |
| `PlacementValidator`   | Centralized validation: build-area containment, terrain flatness (elevation delta ≤ 5 blocks within footprint), spacing conflicts, resource requirements, water avoidance. |
