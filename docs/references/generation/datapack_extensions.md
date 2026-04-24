# Settlement Generation — Datapack Extension Points

The generation system is data-driven from day one. All configuration lives in JSON files loadable
via Minecraft's datapack system. Modpack authors and addon developers can customize behavior without
touching source code.

All datapack managers implement `SimpleJsonResourceReloadListener` and are registered in
`DataReloadEvents`. On `/reload`, all JSON is reparsed and the NBT template catalog is rebuilt.

## Table of Contents

- [Biome Survey Data](#biome-survey-data)
- [Trait Scorer Configurations](#trait-scorer-configurations)
- [Trait Definitions](#trait-definitions)
- [History Events](#history-events)
- [Building Definitions](#building-definitions)
- [NBT Templates](#nbt-templates)

---

## Biome Survey Data

**Path:** `data/<namespace>/settlements/biomes/survey/<biome_namespace>/<biome_path>.json`

Defines per-biome resource contributions and water classification. The mod ships with defaults for
40+ vanilla overworld biomes. Unlisted biomes default to zero resources and no water type.

`template_tags` flows into `TemplateResolutionContext` when this biome is dominant at the settlement
site, influencing which template variants are selected.

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

---

## Trait Scorer Configurations

**Path:** `data/<namespace>/settlements/traits/scoring/<trait>.json`

Defines the additive scoring formula for a trait. Supports:

- `base_score` — Starting value before any contributions
- `resource_tag_weights` — Map of `ResourceTag → float` (positive or negative)
- `water_feature_bonuses` — Map of `WaterFeatureType → float`
- `biome_weights` — Map of `BiomeId → float`
- `elevation_delta_weight` — Contribution from normalized elevation range
- `required_tags` — If any are missing, score is forced to 0.0
- `veto_tags` — If any are present, score is forced to 0.0

---

## Trait Definitions

**Path:** `data/<namespace>/settlements/traits/definitions/<trait>.json`

Registers a trait's namespaced ID and display metadata in `TraitRegistry`. The mod ships with 15
built-in trait definitions.

**Built-in traits:**

- **P0 (Core):** `settlements:settlement_traits/lumber`, `mining`, `farming`, `fishing`, `defense`
- **P1 (Economy & Social):** `pastoral`, `trade`, `honey`, `craft`, `spiritual`, `scholarly`,
  `waypoint`, `arcane`, `ancient`, `seafaring`

Custom traits can be added by third-party datapacks by providing a definition file.

---

## History Events

**Path:** `data/<namespace>/settlements/history/events/<event>.json`

Defines a history event. Time horizon bands: founding era 100–199, mid-life 200–299. Values below
100 are reserved for modpack/datapack use.

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

**Field reference:**

| Field                  | Description                                                                                |
|------------------------|--------------------------------------------------------------------------------------------|
| `id`                   | Unique namespaced identifier                                                               |
| `category`             | Grouping label (e.g., `"disaster"`, `"discovery"`)                                         |
| `time_horizon_min/max` | Integer range defining the event's era. Used for chronological ordering.                   |
| `exclusive_tags`       | Events sharing an exclusive tag are mutually exclusive; earlier event (lower horizon) wins |
| `probability_weight`   | Relative draw weight during weighted sampling                                              |
| `preconditions`        | Conditions evaluated against the current profile and site before the event can fire        |
| `trait_modifiers`      | Additive float deltas applied to `adjustedWeights`, clamped to [0.0, 1.0]                  |
| `visual_markers`       | Opaque string tags deposited into `VisualMarkerSet`; interpretation is a content decision  |
| `narrative_text`       | Human-readable flavor string, display purposes only                                        |

---

## Building Definitions

**Path:** `data/<namespace>/settlements/buildings/definitions/<building>.json`

Defines a building's identity, trait affinities, placement rules, footprint constraints, and spatial
relationships. The full `BuildingDefinition` contract includes:

| Field                    | Description                                                                    |
|--------------------------|--------------------------------------------------------------------------------|
| `id`                     | Namespaced building identifier                                                 |
| `display_info`           | Name, description, icon                                                        |
| `trait_affinities`       | Map of `TraitId → float`. Empty map = universal building (house, well).        |
| `minimum_rank`           | Lowest trait slot that qualifies: PRIMARY, SECONDARY, or FLAVOR                |
| `placement_priority`     | Higher value = picks a plot first                                              |
| `zone_tier_preference`   | Min/max `ZoneTier` (CORE, DOWNTOWN, MIDTOWN, OUTER, SUBURB)                    |
| `requires_road_frontage` | If true, must be placed adjacent to a road                                     |
| `requires_resources`     | Hard gate on local resources (OR logic)                                        |
| `forbidden_resources`    | Hard veto — building cannot be placed where these resources are present        |
| `footprint`              | Fixed plot width and depth                                                     |
| `preferred_tags`         | Building-specific string tags merged with biome/history tags during resolution |
| `proximity_affinities`   | Distance-decaying probability bonus toward specific buildings                  |
| `global_affinities`      | Existence-triggered probability boost from other buildings or traits           |
| `npc_profession`         | Planned, currently unused                                                      |
| `npc_count`              | Planned, currently unused                                                      |

---

## NBT Templates

**Path:** `data/settlements/structure/buildings/<template>.nbt`

Structure templates in NBT format. Each template **must** have a `.meta.json` companion at the same
path.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "building_definition": "settlements:house",
  "tags": [
    "taiga"
  ],
  "authors": [
    "breeze"
  ],
  "notes": "Cold biome residential variant.",
  "width_blocks": 7,
  "depth_blocks": 9,
  "height_blocks": 6,
  "min_minecraft_version": "1.21.1"
}
```

**Required fields:**

| Field                 | Description                                                           |
|-----------------------|-----------------------------------------------------------------------|
| `id`                  | UUIDv4 — globally unique contributor/content identity                 |
| `building_definition` | Building type this template belongs to                                |
| `tags`                | Flat string array for variant matching; must be present even if empty |
| `width_blocks`        | Exact authored width (validated against actual NBT size at load time) |
| `depth_blocks`        | Exact authored depth                                                  |
| `height_blocks`       | Exact authored height                                                 |

**Optional fields:**

| Field                   | Description                                           |
|-------------------------|-------------------------------------------------------|
| `authors`               | List of content contributors                          |
| `notes`                 | Author-facing notes, ignored by runtime logic         |
| `min_minecraft_version` | Minimum game version expected by the template content |

At load time, the resolver validates declared dimensions against actual NBT structure size. Templates
with missing metadata, invalid UUIDs, missing required fields, or size mismatches are skipped from
the catalog.

The catalog validates at startup that each building type has at least one **untagged** template
(tags: `[]`). This serves as the universal fallback during template resolution.
