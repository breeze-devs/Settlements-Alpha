# Settlement Generation — Overview

## Table of Contents

- [1. Vision and Design Philosophy](#1-vision-and-design-philosophy)
- [2. Architecture Overview](#2-architecture-overview)

---

## 1. Vision and Design Philosophy

> *Every settlement has scars and trophies from its past.*

The settlement generation system produces living, logical villages in Minecraft. Every settlement is
shaped by the terrain it spawns on, flavored by its history and events, and assembled from a
trait-driven building system that ensures no two villages feel the same — while all of them feel
*real*.

### Core Tenets

**Terrain drives identity.** A village in a forest near rivers naturally leans toward lumber and
fishing. The land dictates what's possible.

**Traits, not archetypes.** Villages are not typed. There is no "lumber village" archetype. Instead, each
village carries a weighted bag of traits (LUMBER, FISHING, MINING, DEFENSE, etc.) sampled from a
probability distribution shaped by terrain and history. The same terrain can produce different villages
on different world seeds.

**Every settlement has a story.** Before buildings are placed, the generator rolls history events
that modify trait weights and leave visual marks on the village. These are mechanical — they change
the output — not just flavor text.

**Logical spatial layout.** Buildings are placed by a rule system, not randomly. Taverns fight for
road-front plots near the center. Sawmills go to the forest edge. Guard towers sit at the perimeter.

**Compound buildings, not singular blocks.** A "blacksmith" is not one structure. It's a compound:
storefront, forge, living quarters. *(Architecturally planned but not yet implemented; see
[future_work.md](future_work.md).)*

**Exposed for extension.** The building blocks of a village — trait weights, history event
probabilities, building placement rules — are exposed through datapack JSON files. See
[datapack_extensions.md](datapack_extensions.md).

### Settlement Scale Tiers

| Tier    | Trait Slots | Buildings | Population | Area Radius | Notes                                      |
|---------|-------------|-----------|------------|-------------|--------------------------------------------|
| Hamlet  | 1-2         | 5-8       | 10-20      | ~32 blocks  | No formal roads, shared clearing           |
| Village | 2-4         | 10-20     | 30-60      | ~56 blocks  | Main road, civic buildings, optional walls |
| Town    | 4-5         | 25-40     | 80-150     | ~80 blocks  | Crossroads, full zoning, stone walls       |
| City    | --          | --        | --         | --          | *Future: multi-district expansion*         |

Scale tier is selected via weighted random during profile generation (40% Village, 30% Town, 30% Hamlet).

---

## 2. Architecture Overview

### Engine-Agnostic Design

A non-negotiable principle: all generation logic — from terrain survey to layout — operates on
abstract data types with **zero Minecraft API imports**. Only the trigger (structure registration)
and build (piece emission) phases are MC-coupled. This enables thorough unit testing without
launching Minecraft.

### Pipeline

The generation pipeline is strictly linear. Each phase consumes only the output of the previous
phase.

| Phase    | Input                                | Output                                      | MC Dependency              |
|----------|--------------------------------------|---------------------------------------------|----------------------------|
| Trigger  | Worldgen event                       | Chunk region claim                          | Yes                        |
| Survey   | TerrainGrid + SurveyBounds           | SiteReport                                  | No (grid built by adapter) |
| Score    | SiteReport                           | Map\<TraitId, Float\>                       | No                         |
| Sample   | Trait scores + RNG seed              | SettlementProfile                           | No                         |
| History  | SettlementProfile + SiteReport + RNG | Updated SettlementProfile + VisualMarkerSet | No                         |
| Manifest | Profile + BuildingRegistry           | BuildingManifest                            | No                         |
| Layout   | SiteReport + Profile + Manifest      | LayoutResult                                | No                         |
| Build    | GenerationResult                     | Blocks in world                             | Yes                        |

See [pipeline.md](pipeline.md) for a detailed walkthrough of each phase.

### Package Structure

```
domain/          Engine-agnostic business logic. Survey, scoring, sampling, manifest,
                 layout engines all live here. No Minecraft imports.
infrastructure/  Minecraft/NeoForge-coupled implementations. TerrainGridFactory,
                 NbtTemplateResolver, structure pieces, datapack reload listeners.
bootstrap/       Registration and wiring. Structure type registration, reload event hookup.
presentation/    Commands and player-facing surface. /stest generate debug command.
```

Only `infrastructure/` and `bootstrap/` import Minecraft/NeoForge APIs.

### Orchestration

`GenerationPipeline` is the top-level domain orchestrator. It receives a `TerrainGrid`,
`SurveyBounds`, and seed, runs the full chain, and returns a `GenerationResult` bundling all phase
outputs.

Constructor dependencies (all injected via Dagger):

| Dependency             | Role                                          |
|------------------------|-----------------------------------------------|
| `BiomeSurveyLookup`    | Biome-to-resource mapping (Survey + Layout)   |
| `TraitScorerRegistry`  | Registered trait scoring functions            |
| `BuildingRegistry`     | Building definition catalog                   |
| `HistoryEventRegistry` | History event definitions                     |
| `TraitRegistry`        | Known trait definitions (validates modifiers) |

All randomness flows from a single seed derived from `worldSeed XOR hash(chunkPosition)`.
Settlements are fully reproducible given the same seed and terrain.

### Key Terminology

- **Anchor** — The site/worldgen origin, derived from the build area center. Starting point for hub
  search and the reference used by the structure system.
- **Planning center** — The placed town hall's center position. Once the hub is placed, this becomes
  the reference for zoning, road routing, orientation, and all spatial decisions. May differ from the
  anchor if the anchor position is unsuitable (e.g., waterlogged).
