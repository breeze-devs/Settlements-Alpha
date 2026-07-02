# Datapack System

This document covers the **framework** that loads every Settlements datapack file: how JSON on disk
becomes an immutable, queryable snapshot, how a loader is registered, and how to add one. It is about
the *mechanism*, not any one feature's schema — the individual handlers are used here only as worked
examples. For the field-level schema of a specific feature (e.g. building definitions, biome survey),
follow the link in the [handler catalog](#handler-catalog) to that feature's own doc.

> **What this is.** Every datapack file in the mod is JSON that gets decoded by a Mojang **`Codec<T>`**
> into a domain record, folded into an immutable snapshot, and served through a domain registry port.
> A pre-launch migration collapsed 15 hand-rolled `SimpleJsonResourceReloadListener` copies — each with
> its own Gson DTO, `parseFile`, and `loadForTest(entries) → apply(entries, null, null)` hack — into a
> **four-layer base stack** under [`data/framework`](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/).
> Adding a loader is now *one base subclass + one `@Binds` line*, and forgetting to register one is no
> longer possible to do silently.

---

## Overview

The framework rests on a single idea:

> **The parse step is the only thing that varies between loaders — so make it a value.** Once parsing
> is a `Codec<T>` passed *into* a base class instead of code written *into* each subclass, the base can
> own the entire load loop generically. Codecs are the enabler that finally made a shared base possible.

This is why an annotation processor (à la `@BehaviorConfig`) was rejected: that works because behavior
config is a flat, closed set of scalars. Datapack files are **nested, polymorphic, and cross-validated**
(a recipe holds a list of ingredients, each an item-xor-tag matcher; a trade holds fanned-out stock
policies). Describing that with per-field annotations would mean re-inventing a serialization DSL — and
that DSL already ships with the game. It is `Codec`.

### The load pipeline

Every loader rides the same five stages. The framework splits Minecraft's two-arg reload contract so the
load logic is a pure, server-free method that tests call directly.

| Stage | Thread | What happens |
|---|---|---|
| `prepare(rm, profiler)` | off-thread worker | Vanilla scans the loader's directory → `Map<ResourceLocation, JsonElement>` |
| `apply(entries, rm, profiler)` | main | `final` in the base; the framework args are **ignored** and it delegates to `reload` |
| `reload(entries)` | main (or a unit test) | Pure: sort entries by id → `codec.parse` each → collect successes, isolate failures → call `onReloaded` |
| `onReloaded(values)` | main | Build the immutable snapshot / derived indexes from the successfully decoded records |
| query | any | The domain registry port reads the frozen snapshot |

Three invariants hold for every loader because the base enforces them: **strict decode** (a malformed
entry fails wholesale, never half-loads), **per-entry error isolation** (one bad file cannot abort the
reload), and **deterministic order** (entries are id-sorted, so later-file-wins is stable).

---

## On-disk layout

Datapack files live under the mod namespace with a deliberately redundant nesting:

```
src/main/resources/data/settlements/settlements/<loader-dir>/….json
                        └── namespace ──┘└─ datapack root ─┘
```

The **outer** `settlements` is the resource namespace; the **inner** `settlements` is the datapack root
folder that the loaders actually scan. A loader's `super(directory, …)` string is rooted at that inner
folder — e.g. `"settlements/enchantments/costs"` resolves to
`data/settlements/settlements/enchantments/costs/`.

A few things that use *vanilla* datapack formats (recipes, loot tables, structures, tags) sit directly
under `data/settlements/` and are **not** part of this framework — they are loaded by Minecraft itself.

The loader directories (verbatim scan strings, rooted at the datapack root):

| Directory | Files | Loader family |
|---|---|---|
| `settlements/biomes/survey` | ~42 | keyed catalog |
| `settlements/enchantments/costs` | ~40 | keyed catalog |
| `settlements/mining/ore_weights` | ~16 | bespoke (Layer 0) |
| `settlements/traits/definitions` | ~15 | keyed catalog |
| `settlements/trade_catalog` | ~15 | profession catalog |
| `settlements/buildings/definitions` | ~15 | bespoke (Layer 0) |
| `settlements/history/events` | ~12 | keyed catalog |
| `settlements/craft_catalog` | ~10 | profession catalog |
| `settlements/traits/scoring` | ~5 | bespoke (Layer 0) |
| `settlements/mason/excavate_substrate` | ~4 | weighted yield |
| `settlements/fishing/catches` | ~4 | bespoke (Layer 0) |
| `settlements/cultivation_crops` | ~4 | bespoke (Layer 0, server-only) |
| `settlements/specializations` | ~2 | keyed catalog |
| `settlements/farming/collect_honey` | 1 (`default.json`) | weighted yield |
| `settlements/farming/harvest_honeycomb` | 1 (`default.json`) | weighted yield |

---

## The base-class stack

Four classes, all in [`infrastructure/minecraft/data/framework`](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/):

```
SimpleJsonResourceReloadListener            (Minecraft)
        └── CodecJsonDataManager<T>         Layer 0 — decode loop, error isolation, apply/reload split
              ├── KeyedCatalogDataManager<K,T>      Layer 1a — Map<K,T>, last-file-wins
              ├── ProfessionCatalogDataManager<T>   Layer 1b — Map<Profession, List<T>>, per-profession id-merge
              └── WeightedYieldDataManager          Layer 1c — Map<block, drop table> + roll engine
```

**Pick the lowest layer that fits.** A loader that fits 1a/1b/1c writes almost nothing; a loader with a
bespoke snapshot shape (a CDF, dual indexes, a two-phase hook) stops at Layer 0 and writes its own
`onReloaded`.

### Layer 0 — `CodecJsonDataManager<T>`

**File:** [CodecJsonDataManager.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/CodecJsonDataManager.java)

Owns everything mechanical. The framework `apply(...)` is `final` and delegates to a pure `reload(...)`,
so **the old `null, null` test hack is gone** — tests call `reload(entries)` and never see the reload args.

The decode loop ([:48](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/CodecJsonDataManager.java:48)) id-sorts the entries, then per entry:

- On **complete** success (`result.result()` is present) the value is kept. Note it gates on `result()`,
  **not `resultOrPartial(...)`** — a `listOf()` codec reports a failed element as an error carrying a
  *salvaged partial*; taking that partial would silently load a half-decoded file and make the error
  count lie. `resultOrPartial` is used only on the failure branch to surface the error message.
- The whole loop is inside a per-entry `try/catch (Exception)`. This is load-bearing: a domain record's
  compact constructor throws `IllegalArgumentException` on an invariant violation, and **DFU does not wrap
  a thrown constructor exception into a `DataResult` error** — so without the catch, one bad file would
  abort the entire reload instead of failing just itself.

```java
public abstract class CodecJsonDataManager<T> extends SimpleJsonResourceReloadListener {

    protected CodecJsonDataManager(@Nonnull String directory, @Nonnull Codec<T> codec) { … }

    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> entries, …) { this.reload(entries); }

    @VisibleForTesting
    public final void reload(@Nonnull Map<ResourceLocation, JsonElement> entries) { … }

    protected abstract String label();                                  // for logs
    protected abstract void onReloaded(@Nonnull Map<ResourceLocation, T> values);   // build the snapshot
}
```

| A Layer-0 subclass supplies | It inherits |
|---|---|
| ctor: `super(directory, codec)` | the framework `apply`, the pure `reload`, id-sort, per-entry error isolation, strict `result()` gating, the loaded/errors log |
| `label()` | |
| `onReloaded(values)` — its snapshot / derived indexes | |

### Layer 1a — `KeyedCatalogDataManager<K, T>`

**File:** [KeyedCatalogDataManager.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/KeyedCatalogDataManager.java)

For the common case: a flat `Map<K, T>` keyed by an id **extracted from the value**, later file replaces
earlier, frozen to an unmodifiable map. `onReloaded` is `final` here — a keyed subclass cannot post-process.

| Supplies | Inherits |
|---|---|
| ctor, `label()`, `keyOf(value) → K` | the merge, `find(key) → Optional<T>`, `all() → Map<K,T>` |

Typically the subclass also `implements` its domain registry interface, delegating it to `find`/`all`.

### Layer 1b — `ProfessionCatalogDataManager<T>`

**Files:** [ProfessionCatalogDataManager.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/ProfessionCatalogDataManager.java),
[ProfessionCatalogFile.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/ProfessionCatalogFile.java)

For catalogs shaped as `Map<VillagerProfessionKey, List<T>>`, where **each file is `(profession, values)`**.
The shared record `ProfessionCatalogFile<T>(profession, values)` captures that shape; its
`codec(valueCodec, valuesField)` factory wraps a value codec into the file codec (the profession field is
fixed to `"profession"`; the list field name is caller-supplied, e.g. `"recipes"` or `"stock"`). The base
merges per profession with an inner id-dedupe (last-write-wins, order preserved), then calls an overridable
hook.

```java
protected ProfessionCatalogDataManager(String directory, Codec<T> valueCodec, String valuesField) {
    super(directory, ProfessionCatalogFile.codec(valueCodec, valuesField));
}
protected abstract String idOf(T value);
protected void onCatalogReloaded(Map<VillagerProfessionKey, List<T>> byProfession) { }   // no-op default
```

| Supplies | Inherits |
|---|---|
| ctor `(dir, valueCodec, listFieldName)`, `label()`, `idOf(value)` | the two-level merge, `valuesFor(profession)`, `byProfession()` |
| *optionally* override `onCatalogReloaded` to build derived projections | |

Trade overrides `onCatalogReloaded` to derive its offer / demand / supply projections; Craft takes the
inherited map as-is. A future catalog that needs *file-level* fields beyond `(profession, values)` should
extend Layer 0 directly rather than grow this base.

### Layer 1c — `WeightedYieldDataManager`

**File:** [WeightedYieldDataManager.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/framework/WeightedYieldDataManager.java)

For per-block weighted drop tables (the "harvest yields" family). This is the most opinionated base: the
value codec is **hard-wired** to `WeightedYieldTableCodec`, so a subclass supplies *only a directory and a
label* and inherits the entire roll engine.

The snapshot is `Map<String, WeightedYieldTable>` keyed by `block`. A `"default"` block is **mandatory** —
if no `default.json` is present the base logs an error (it does not throw; the loader still loads, but all
rolls will be empty). Lookups fall back to `default` at both the block and the expertise-pool level.

The engine:
- `rollDrops(expertise, blockId) → List<ItemStack>` — the public entry point behaviors call.
- `rollEntries(expertise, blockId) → List<WeightedYieldItem>` — rolls each pool `rolls` times via
  `RandomUtil.weightedChoice(...)`.
- `selectionWeight(blockId) → double` — used to weight which block a behavior targets.

| Supplies | Inherits |
|---|---|
| ctor: `super(directory, label)` | the codec, the block-keyed merge + `default` check, `rollDrops` / `rollEntries` / `selectionWeight` / `allBlockData` |

The JSON is a `block` id, an optional `selection_weight` (defaulted to `1.0`; the honey files omit it), and
`expertise_pools` keyed by rank, each a `rolls` count and an `items[]` list of weighted stacks:

```json
{
  "block": "default",
  "selection_weight": 1.0,
  "expertise_pools": {
    "novice":     { "rolls": 1, "items": [ { "item": "minecraft:gravel", "weight": 1.0, "min_count": 1, "max_count": 1 } ] },
    "apprentice": { "rolls": 1, "items": [ { "item": "minecraft:gravel", "weight": 1.0, "min_count": 1, "max_count": 1 } ] },
    "master":     { "rolls": 1, "items": [ { "item": "minecraft:gravel", "weight": 1.0, "min_count": 1, "max_count": 2 } ] }
  }
}
```

The decoded records live in [`domain/common/yields`](../../src/main/java/dev/breezes/settlements/domain/common/yields/)
— `WeightedYieldItem`, `WeightedYieldPool`, `WeightedYieldTable`, each a Lombok `@Builder` record whose
compact constructor **clamps benign mistakes but throws on structural ones**:

| Record | Clamps (silent) | Throws (strict) |
|---|---|---|
| `WeightedYieldItem` | `minCount ≥ 1`, `maxCount ≥ minCount` | negative `weight` |
| `WeightedYieldPool` | `rolls ≥ 1`, defensive-copies `items` | empty `items`; no item with `weight > 0` |
| `WeightedYieldTable` | defensive-copies `pools` | blank `block`; empty `pools`; `selectionWeight ≤ 0` |

Adding a new yield loader is a ~15-line subclass passing `(directory, label)` — item ids resolve lazily at
roll time (an id resolving to `AIR` is skipped with a warning). The planned Scavenge behavior's loader is
just a fourth subclass here, not a new base.

---

## Registration — the two-phase `@IntoSet` multibinding

Registration used to be the most-forgotten seam: a `@Provides` in `DataManagerModule`, an accessor on
`SettlementsComponent`, **and** an `event.addListener(...)` line in `CommonModEvents` — and missing the
last one silently disabled the loader with no error. That is now impossible.

Every loader is contributed into a Dagger multibinding in
[ReloadListenerModule.java](../../src/main/java/dev/breezes/settlements/di/modules/ReloadListenerModule.java),
split into two qualified sets:

- [`@DataReloadListeners`](../../src/main/java/dev/breezes/settlements/di/DataReloadListeners.java) — the
  **producers** (all 15 loaders + `NbtTemplateResolver`). Mutually order-independent.
- [`@PostReloadListeners`](../../src/main/java/dev/breezes/settlements/di/PostReloadListeners.java) — the
  **validators/consumers** that read the populated producers (see [Two-phase validation](#two-phase-validation)).

```java
@Module
public interface ReloadListenerModule {
    @Binds @IntoSet @DataReloadListeners
    PreparableReloadListener craftCatalog(CraftCatalogDataManager m);
    // … one line per loader …
    @Binds @IntoSet @PostReloadListeners
    PreparableReloadListener craftCatalogValidation(CraftCatalogValidationReloadListener l);
}
```

[CommonModEvents.registerReloadListeners](../../src/main/java/dev/breezes/settlements/bootstrap/event/CommonModEvents.java:75)
is now a frozen two-line drain — **producers first, post-listeners second**:

```java
component.dataReloadListeners().forEach(event::addListener);
component.postReloadListeners().forEach(event::addListener);
```

Because NeoForge runs reload listeners in registration order, draining all producers before any post-listener
is what guarantees a validator observes fully populated managers. **Intra-set order is neither guaranteed nor
needed** — producers do not depend on each other.

> **The `@Singleton` that makes it correct.** The `@Binds` methods carry *no scope*. The concrete
> `@Provides @Singleton FooDataManager` in
> [DataManagerModule](../../src/main/java/dev/breezes/settlements/di/modules/DataManagerModule.java) is what
> makes the object in the multibound set the **same instance** that gets injected elsewhere as a registry
> port. Drop the `@Singleton` and the reload would populate one instance while queries read a different,
> empty one. This is why `DataManagerModule`'s concrete providers **stay** — the multibinding *adds*
> bindings, it does not replace them.

`CommonModEvents` is now closed: a new loader is one `@Binds` line here, never an event-handler edit.

---

## Codec conventions

**Per-type companion codecs, co-located with the type.** There is deliberately **no `SettlementsCodecs`
god-class**. Each domain type gets a sibling `FooCodec` in the same package exposing a `public static final
Codec<Foo> CODEC` (house style: [ItemMatchCodec](../../src/main/java/dev/breezes/settlements/domain/economy/catalog/ItemMatchCodec.java),
[CraftRecipeCodec](../../src/main/java/dev/breezes/settlements/domain/crafting/catalog/CraftRecipeCodec.java)).

**Strict decode.** An unknown enum value, malformed id, or unknown tag *anywhere* — including inside a list
or map — fails the **whole entry**, not just the offending element. Strict enum/id codecs use
`comapFlatMap(... → DataResult.error(...))` on the unknown case. This is a deliberate reversal of the older
"drop the bad element, keep the entry" leniency: a datapack typo should fail loudly, not load a subtly
wrong record.

**Validation ownership.** The domain record's compact constructor stays the authority on its invariants;
codecs do **not** duplicate them. The codec surfaces *shape* errors (missing field, wrong type, unknown
enum) as decode errors; the constructor surfaces *semantic* errors as thrown exceptions, which Layer 0's
`try/catch` turns into a skipped-and-logged entry.

**Absent, not `null`.** `optionalFieldOf(name)` models an **absent** field, not a present-`null` one. An
explicit JSON `null` on an optional field is a present-but-invalid value and fails strict decode. Datapack
files therefore **omit** optional fields entirely — there are no `"field": null` lines anywhere in the
shipped data (historically `"water_type": null` and `"npc_profession": null` had to be stripped).

**Reusable value codecs** (author your top-level record codec by composing these, don't re-derive them):

| Codec | Type | Package |
|---|---|---|
| [`ItemMatchCodec`](../../src/main/java/dev/breezes/settlements/domain/economy/catalog/ItemMatchCodec.java) | `ItemMatch` (item **xor** tag) | `domain.economy.catalog` |
| [`VillagerProfessionKeyCodec`](../../src/main/java/dev/breezes/settlements/domain/entities/VillagerProfessionKeyCodec.java) | `VillagerProfessionKey` | `domain.entities` |
| [`ExpertiseCodec`](../../src/main/java/dev/breezes/settlements/domain/entities/ExpertiseCodec.java) | `Expertise` (enum) | `domain.entities` |
| [`BiomeIdCodec`](../../src/main/java/dev/breezes/settlements/domain/common/BiomeIdCodec.java) | `BiomeId` | `domain.common` |
| [`TraitIdCodec`](../../src/main/java/dev/breezes/settlements/domain/generation/model/profile/TraitIdCodec.java) / [`TraitSlotCodec`](../../src/main/java/dev/breezes/settlements/domain/generation/model/profile/TraitSlotCodec.java) | `TraitId` / `TraitSlot` | `domain.generation.model.profile` |
| [`ResourceTagCodec`](../../src/main/java/dev/breezes/settlements/domain/generation/model/survey/ResourceTagCodec.java) / [`WaterFeatureTypeCodec`](../../src/main/java/dev/breezes/settlements/domain/generation/model/survey/WaterFeatureTypeCodec.java) | `ResourceTag` / `WaterFeatureType` | `domain.generation.model.survey` |

---

## The `match` shape — item-or-tag

Anywhere a datapack file needs to **match** an item that could be an item id *or* a tag, it uses a nested
`"match"` object holding exactly one of `item` or `tag` (an XOR — supplying both, or neither, fails decode).
This is [ItemMatchCodec](../../src/main/java/dev/breezes/settlements/domain/economy/catalog/ItemMatchCodec.java),
reused via `.fieldOf("match")`:

```json
{ "match": { "item": "minecraft:egg" } }
{ "match": { "tag":  "c:foods" } }
```

> **Rule:** a **matcher** input (craft ingredient, trade stock predicate) uses the nested `"match"` object;
> a concrete **product/yield** item (a craft `output`, a drop-table `items[]` entry, an ore-regen `block`)
> stays **flat** `"item"`. The two are different things — one asks "does this stack qualify?", the other
> names a specific stack to produce.

```json
"inputs":  [ { "match": { "item": "minecraft:paper" }, "count": 8 } ],
"output":  { "item": "minecraft:map", "count": 1 }
```

---

## Two-phase validation

Some data is only valid *relative to other data* — a trait scorer that references an unknown trait, a
building whose affinity names a deleted trait, a craft recipe whose output nothing will ever buy. Those
checks need every producer loaded first, which is exactly what the producers-before-post-listeners drain
guarantees. There are two validators today, both in [`bootstrap/event`](../../src/main/java/dev/breezes/settlements/bootstrap/event/):

- **[GenerationDataValidationReloadListener](../../src/main/java/dev/breezes/settlements/bootstrap/event/GenerationDataValidationReloadListener.java)**
  (delegates to [GenerationDataValidator](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/data/validation/GenerationDataValidator.java)):
  takes the trait catalog as the known-trait set, reads the **raw** scorer and building snapshots, drops
  any entry referencing an unknown trait, and **writes back** the pruned active snapshots. It *mutates*.
- **[CraftCatalogValidationReloadListener](../../src/main/java/dev/breezes/settlements/bootstrap/event/CraftCatalogValidationReloadListener.java)**:
  reads `CraftCatalogDataManager.loadedRecipes()` and the trade catalog's supply projection, and **warns**
  about any recipe whose output has no `dump` rung (it would silently never craft). It only *diagnoses*.

Validators that mutate require the producer to expose a **two-phase hook** — a raw getter and an active
replacer — so the validator can read the unfiltered load and install a filtered one:

| Producer | Raw getter | Active replacer |
|---|---|---|
| `TraitScorerDataManager` | `rawScorers()` | `replaceActiveScorers(...)` |
| `BuildingDefinitionDataManager` | `rawDefinitions()` | `replaceActiveDefinitions(...)` |
| `CraftCatalogDataManager` | `loadedRecipes()` | *(read-only — its validator only warns)* |

---

## Handler catalog

The complete set of loaders, grouped by base family. For the field-level schema of a feature-owned loader,
follow the schema link; loaders without a dedicated feature doc are framework-simple enough to read from
their codec. Directories below are shown relative to the datapack root — a loader's actual scan string
prefixes `settlements/` (e.g. `enchantments/costs` → `"settlements/enchantments/costs"`; see
[On-disk layout](#on-disk-layout)).

**Keyed catalogs** (`KeyedCatalogDataManager<K, T>`):

| Loader | Directory | Role | Schema |
|---|---|---|---|
| `EnchantmentCostDataManager` | `enchantments/costs` | per-enchantment cost tuning | codec |
| `SpecializationDataManager` | `specializations` | villager specialization profiles | codec |
| `BiomeSurveyDataManager` | `biomes/survey` | per-biome resource density + water type | [generation](generation/datapack_extensions.md#biome-survey-data) |
| `TraitDefinitionDataManager` | `traits/definitions` | trait id + display metadata | [generation](generation/datapack_extensions.md#trait-definitions) |
| `HistoryEventDataManager` | `history/events` | settlement history events | [generation](generation/datapack_extensions.md#history-events) |

**Profession catalogs** (`ProfessionCatalogDataManager<T>`):

| Loader | Directory | Role | Schema |
|---|---|---|---|
| `TradeCatalogDataManager` | `trade_catalog` | per-profession stock/trade policies (restock/offer/dump) | codec |
| `CraftCatalogDataManager` | `craft_catalog` | per-profession craft recipes | codec |

**Weighted yields** (`WeightedYieldDataManager` — see [Layer 1c](#layer-1c--weightedyielddatamanager)):

| Loader | Directory | Role |
|---|---|---|
| `CollectHoneyYieldDataManager` | `farming/collect_honey` | honey-bottle drops |
| `HarvestHoneycombYieldDataManager` | `farming/harvest_honeycomb` | honeycomb drops |
| `ExcavateSubstrateYieldDataManager` | `mason/excavate_substrate` | shovel substrate drops (adds `selection_weight`) |

**Bespoke** (extend Layer 0 directly for a custom snapshot shape):

| Loader | Directory | Why bespoke | Schema |
|---|---|---|---|
| `FishCatchDataManager` | `fishing/catches` | builds a cumulative-weight CDF for weighted rolls | codec |
| `TraitScorerDataManager` | `traits/scoring` | two-phase (validated by generation validator) | [generation](generation/datapack_extensions.md#trait-scorer-configurations) |
| `BuildingDefinitionDataManager` | `buildings/definitions` | 6 derived indexes + two-phase | [generation](generation/datapack_extensions.md#building-definitions) |
| `OreRegenDataManager` | `mining/ore_weights` | per-host precomputed weighted candidate maps | codec |
| `CultivationCropDataManager` | `cultivation_crops` | dual index; **server-only** resolving codec (see below) | codec |

**Non-codec:** [`NbtTemplateResolver`](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/worldgen/NbtTemplateResolver.java)
is a producer in the same multibinding, but it extends `SimplePreparableReloadListener` directly rather than
this framework — it scans binary `.nbt` templates plus a companion `.meta.json` sidecar (plain Gson), not a
single JSON directory. See [generation → NBT Templates](generation/datapack_extensions.md#nbt-templates).

---

## Adding a new loader

1. **Pick the lowest base that fits** ([decision guide](#choosing-a-base) below).
2. **Author the domain record(s)** — a `record` (or Lombok `@Builder` record) with a compact constructor
   that enforces its own invariants. This is the source of truth for validity.
3. **Author the codec(s)** — a co-located `FooCodec` with `public static final Codec<Foo> CODEC`, built with
   `RecordCodecBuilder`. Compose the [reusable value codecs](#codec-conventions); decode enums/ids strictly.
4. **Write the loader** — extend the chosen base, with a no-arg constructor calling
   `super(directory, FooCodec.CODEC)` (or just `super(directory, label)` for a yield loader). Implement the
   base's hooks (`label`, `keyOf`/`idOf`, or `onReloaded`). `implements` its domain registry interface.
5. **Provide it** — a `@Provides @Singleton FooDataManager` in
   [DataManagerModule](../../src/main/java/dev/breezes/settlements/di/modules/DataManagerModule.java), plus a
   `@Binds`/`@Provides` in the relevant feature module binding it to its **domain registry port** wherever a
   consumer injects it as that interface.
6. **Multibind it** — **one** line in
   [ReloadListenerModule](../../src/main/java/dev/breezes/settlements/di/modules/ReloadListenerModule.java):
   `@Binds @IntoSet @DataReloadListeners PreparableReloadListener foo(FooDataManager m);`. That is the entire
   registration — do **not** touch `CommonModEvents`.
7. **Drop the JSON** under `data/settlements/settlements/<directory>/`.
8. **Test it** by calling `manager.reload(entries)` directly with a hand-built `Map<ResourceLocation,
   JsonElement>` — no nulls, no Dagger, no Minecraft mocking. (Minecraft objects are not mockable, so codecs
   that resolve to live registry objects can't be unit-tested this way — see the server-only note below.)

### Choosing a base

- **Per-block weighted drops with expertise pools?** → a `WeightedYieldDataManager` subclass. Just pass
  `(directory, label)`.
- **A flat `Map<K, T>` keyed by an id inside the value, last-file-wins?** → `KeyedCatalogDataManager<K, T>`.
- **`Map<profession, List<T>>` from `(profession, values)` files?** → `ProfessionCatalogDataManager<T>`
  (override `onCatalogReloaded` if you need derived projections).
- **Anything else** — a CDF, multiple derived indexes, a dual key, a two-phase hook → extend **Layer 0**
  and write your own `onReloaded`. Don't contort a 1x base to fit; the bespoke tail is legitimate.

### When you need more than a base

- **Cross-registry validation** (your data references another loader's ids): add a `@PostReloadListeners`
  validator and expose a `raw*` / `replaceActive*` two-phase pair on your loader. See
  [Two-phase validation](#two-phase-validation).
- **Decoding to live registry objects** (`Block`, `Item`, a `CropBlock`): a resolving codec
  (`BuiltInRegistries.BLOCK.byNameCodec()`, `.flatXmap(...)`) is cleaner than a manual AIR-guard, **but** it
  makes the loader depend on populated registries — it becomes **server-only and loses `reload()`
  testability**. `CultivationCropDataManager` accepts that trade for fail-fast validation; `OreRegenDataManager`
  takes the other path — decode to `ResourceLocation`, resolve lazily at use-time — and stays unit-testable.
  Prefer the lazy split unless load-time fail-fast is worth the lost test.

---

## Gotchas

- **Optional fields are omitted, never `null`.** `optionalFieldOf` = absence; an explicit `null` fails
  strict decode.
- **Strict decode fails the whole entry**, not the bad element — one unknown enum sinks the file.
- **Gate on `result()`, not `resultOrPartial()`** — the latter salvages a partial and lies about errors.
  (The base already does this; matters if you hand-roll a nested `listOf` decode.)
- **A `WeightedYieldDataManager` needs a `default.json`** — missing it logs an error and every roll comes
  back empty.
- **Don't fold a validator into the producer set** — it must be `@PostReloadListeners`, or it may run before
  the data it validates is loaded.
- **Keep the concrete provider `@Singleton`** — it's what makes the multibound listener and the injected
  registry port the same object.
- **Resolving codecs are server-only and untestable via `reload()`** — decode to `ResourceLocation` + resolve
  lazily unless you specifically want load-time fail-fast.
