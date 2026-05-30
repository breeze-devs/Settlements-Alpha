# Behavior Sensor / Precondition Refactor Checklist

Quick reference for migrating legacy resource behaviors to the pumpkin canary pattern from
`HarvestPumpkinBehavior`.

The goal is to keep discovery, candidacy, target resolution, and action safety separate:

- **Sensors discover** world facts with the wide scan and write memory.
- **Preconditions gate** behavior start from memory and cheap confirmation only.
- **Resolvers pick** concrete targets and write `BehaviorContext` state.
- **Action steps guard** against last-moment world changes before mutating the world.

## Non-negotiable rules

- [ ] `ICondition.test()` must not mutate memory, behavior state, target caches, or the world.
- [ ] No behavior should read targets from a condition instance with `getTargets()`.
- [ ] No behavior should refresh a target list by calling `condition.test()` from a loop or `onEnd` hook.
- [ ] No behavior precondition should run the wide discovery scan.
- [ ] Sensors are the only wide-discovery layer.
- [ ] Target resolvers may write `BehaviorStateType.TARGET` and behavior-local visited state.
- [ ] Runtime action steps should keep a final guard because the world can change after targeting.
- [ ] If memory self-healing/pruning returns later, keep it outside `ICondition.test()` as an explicit command path.

## Tenets

- Remove any legacy files. we do not want temporary hacks. Think big think long term.
- Always update this doc to check off items that are completed

## Modern pumpkin pattern to copy

Reference files:

- `BlockResourceSensor.java` — shared wide scan, writes one memory per registered `BlockResource`.
- `KnownBlockSitesPrecondition.java` — reads configured memory and confirms remembered sites only.
- `BlockMemoryTargetResolver.java` — reads memory, excludes visited sites, confirms a target, writes `TARGET`.
- `HarvestPumpkinBehavior.java` — uses staged `PICK_TARGET -> APPROACH -> CHOP -> SETTLE -> PICKUP -> LOOP -> AWARD`.

Checklist:

- [x] Store a `BlockMatcher` field for the resource kind.
- [x] Store `BlockScanBox.confirm()` and `BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS`.
- [x] Inject `BlockMemoryTargetResolver` into the behavior constructor.
- [x] Replace `Nearby...Condition` with a known-memory precondition.
- [x] In `PICK_TARGET`, call the resolver instead of reading condition targets.
- [x] Initialize `BehaviorStateType.VISITED_BLOCK_SITES` in `onBehaviorStart` for multi-target loops.
- [x] After successful world mutation, add the consumed site to `VISITED_BLOCK_SITES`.
- [ ] Keep `INTERACTION_OUTCOME` initialization if the behavior already uses it for reward/result flow.

## Per-behavior migration steps

Use this checklist for each block-resource behavior.

### 1. Extract or reuse the matcher

- [ ] Move condition-specific block logic into `BlockMatchers`.
- [ ] Preserve semantic filters from the old condition.
- [ ] Prefer a single matcher used by the sensor, precondition, resolver, and final action guard.

Examples from legacy conditions:

- Melon: `Blocks.MELON` with adjacent `Blocks.ATTACHED_MELON_STEM`.
- Sweet berries: `Blocks.SWEET_BERRY_BUSH` with `SweetBerryBushBlock.AGE == MAX_AGE`.
- Ripe crops: `state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)`.
- Full hive: `BlockTags.BEEHIVES` with `BeehiveBlock.HONEY_LEVEL == MAX_HONEY_LEVELS`.
- Sugar cane: middle cane block with sugar cane above and below; action target semantics need care.
- Soul sand: `SOUL_SAND` with air above or max-age nether wart above.
- Ore: ore tags, with replacement block selected at action time.

### 2. Add memory for known sites

- [ ] Add a `MemoryModuleType<List<GlobalPos>>` in `MemoryModuleTypeRegistry`.
- [ ] Add the matching `MemoryType<List<GlobalPos>>` in `MemoryTypeRegistry`.
- [ ] Name the memory by observed actionable site, not by behavior implementation detail.

Suggested names:

- `RIPE_MELON_SITES`
- `RIPE_SWEET_BERRY_BUSH_SITES`
- `RIPE_CROP_SITES`
- `HARVESTABLE_SUGARCANE_SITES`
- `FULL_HIVE_SITES`
- `NETHER_WART_FARM_SITES` or `HARVESTABLE_SOUL_SAND_SITES`
- `ORE_SITES`

### 3. Add or generalize the sensor

- [ ] Prefer a shared resource sensor if multiple resources use the same scan cadence and radius.
- [ ] Use a shared unsorted wide scan for same-cohort resources; nearest ordering belongs in confirmation/resolution at
  use time.
- [ ] Write `MemoryWrite.of(memoryType, sites)` when sites are found.
- [ ] Write `MemoryWrite.clear(memoryType)` when none are found.
- [ ] Register the sensor through `SensorCatalogModule`.

Keep the sensor behavior-agnostic. It should know what exists nearby, not which behavior will use it.

### 4. Replace the precondition

- [ ] Replace `Nearby...Condition` ownership with a known-site precondition.
- [ ] The precondition may read memory and run a small confirm around remembered positions.
- [ ] The precondition must not cache targets for the behavior.
- [ ] The precondition must not prune memory.
- [ ] Inventory, hunger, and capacity gates can remain as separate pure preconditions.

If the precondition class is generalized, it should accept:

- memory type,
- matcher,
- confirm box,
- max confirms,
- description.

### 5. Replace target selection

- [ ] Delete behavior fields that mirror condition target state, such as `valid...AroundVillager`.
- [ ] Delete behavior fields that store picked block positions when `BehaviorStateType.TARGET` can carry them.
- [ ] Use `BlockMemoryTargetResolver.resolveBlockTarget(...)` in the `PICK_TARGET` stage.
- [ ] Let the resolver exclude `VISITED_BLOCK_SITES` for looped harvests.
- [ ] Transition to action/approach only when the resolver succeeds.
- [ ] Transition to award/end when the resolver fails.

### 6. Preserve action safety

- [ ] In the action keyframe, read the current block state at `TargetQueries.firstBlockPos(ctx)`.
- [ ] Verify the block still matches the resource matcher or behavior-specific action predicate.
- [ ] If it no longer matches, no-op or end cleanly; do not crash and do not ghost-harvest.
- [ ] Only award experience after a successful world mutation.
- [ ] Mark the consumed `GlobalPos` as visited after successful mutation.

### 7. Remove legacy leftovers

- [ ] Remove `Nearby...Condition` fields from the behavior.
- [ ] Remove `getTargets()` calls.
- [ ] Remove `stillMatches()` calls.
- [ ] Remove mid-loop `condition.test(...)` calls.
- [ ] Remove comments such as "Precondition scan already ran, pull from cached results".
- [ ] Remove random target selection from condition snapshots unless it is reintroduced inside a resolver.

## Suggested migration order

### 1. `HarvestMelonBehavior.java`

Closest match to pumpkin.

- [x] Add `BlockMatchers.HARVESTABLE_MELON` from `NearbyRipeMelonCondition`.
- [x] Add `RIPE_MELON_SITES` memory.
- [x] Add sensor support for melons.
- [x] Replace `NearbyRipeMelonCondition` with known-site precondition.
- [x] Replace `nearbyMelonCondition.getTargets().stream().filter(stillMatches)` with resolver call.
- [x] Replace final `state.is(Blocks.MELON)` guard with matcher/self-confirm so decorative melons remain protected by
  stem adjacency.
- [x] Add visited-site initialization and marking after successful chop.

### 2. `HarvestSweetBerriesBehavior.java`

Same staged shape as pumpkin/melon, different action mutation.

- [x] Add `BlockMatchers.RIPE_SWEET_BERRY_BUSH`.
- [x] Add `RIPE_SWEET_BERRY_BUSH_SITES` memory.
- [x] Replace `NearbyRipeSweetBerryBushCondition` with known-site precondition.
- [x] Replace condition target cache and `stillMatches()` with resolver call.
- [x] Keep final guard for max-age bush.
- [x] Preserve reset to `POST_HARVEST_AGE` instead of destroying the bush.
- [x] Add visited-site tracking after successful reset.

### 3. `HarvestRipeCropsBehavior.java`

Same staged shape as berries, polymorphic crop matcher.

- [x] Add `BlockMatchers.RIPE_CROP` using the `CropBlock` max-age predicate.
- [x] Add `RIPE_CROP_SITES` memory.
- [x] Replace `NearbyRipeCropCondition` with known-site precondition.
- [x] Replace condition target cache and `stillMatches()` with resolver call.
- [x] Keep final guard for `CropBlock` max age.
- [x] Preserve reset to `crop.getStateForAge(0)`.
- [x] Add visited-site tracking after successful reset.

### 4. `HarvestSugarCaneBehavior.java`

Legacy single-stage behavior; refactor to the modern staged loop.

- [ ] Add `BlockMatchers.HARVESTABLE_SUGARCANE` from `NearbySugarCaneExistsCondition`.
- [ ] Decide whether memory stores the middle cane block, the break block, or the navigation target.
- [ ] Add `HARVESTABLE_SUGARCANE_SITES` memory.
- [ ] Replace `NearbySugarCaneExistsCondition` with known-site precondition.
- [ ] Replace `sugarCanePos` and `validSugarCaneAroundVillager` with resolver + `TARGET` state.
- [ ] Convert from `HARVEST_SUGAR_CANE -> END` to `PICK_TARGET -> APPROACH -> HARVEST -> PICKUP/LOOP/AWARD -> END` if
  drops should be collected like modern harvests.
- [ ] Keep final guard that cane is still harvestable before `destroyBlock`.
- [ ] Mark the harvested site visited.

Sugar cane needs an explicit target convention because the old matcher finds a cane block with cane above and below,
then destroys that position. Document the convention in the matcher or resolver notes.

### 5. `CollectHoneyBehavior.java`

Legacy single-stage loop with condition re-scan in `selectFreshTarget`.

- [ ] Add `BlockMatchers.FULL_HIVE` from `NearbyFullHiveExistsCondition`.
- [ ] Add shared `FULL_HIVE_SITES` memory.
- [ ] Add or reuse full-hive sensor support.
- [ ] Replace `NearbyFullHiveExistsCondition` with known-site precondition.
- [ ] Keep bottle and inventory capacity preconditions as separate pure gates.
- [ ] Replace `targetHivePos` with `TARGET` state where practical.
- [ ] Replace `selectFreshTarget(...)` condition re-scan with resolver call.
- [ ] Preserve final honey-level guard in `performHarvest`.
- [ ] Preserve bottle consumption and yield-table behavior.
- [ ] Mark hive visited after successful honey collection so the loop does not retarget the emptied hive.

### 6. `HarvestHoneycombBehavior.java`

Same full-hive target model as collect honey, different inventory/action requirements.

- [ ] Reuse `BlockMatchers.FULL_HIVE` and `FULL_HIVE_SITES`.
- [ ] Replace `NearbyFullHiveExistsCondition` with known-site precondition.
- [ ] Keep shears and inventory capacity preconditions as separate pure gates.
- [ ] Replace `targetHivePos` and `selectFreshTarget(...)` with resolver-driven `TARGET` state.
- [ ] Preserve final honey-level guard in `performHarvest`.
- [ ] Preserve yield-table behavior and sound.
- [ ] Mark hive visited after successful honeycomb harvest.

### 7. `HarvestSoulSandBehavior.java`

Legacy single-stage behavior with mixed harvest/plant behavior.

- [ ] Extract a matcher for valid nether wart farm sites from `NearbySoulSandExistsCondition`.
- [ ] Decide whether memory stores the soul sand base position or the actionable `above()` wart/plant position.
- [ ] Add memory for nether wart farm sites.
- [ ] Replace `NearbySoulSandExistsCondition` with known-site precondition.
- [ ] Replace `netherWartPos` and `validSoulSandAroundVillager` with resolver + `TARGET` state.
- [ ] Convert to explicit stages if continuing modern staged cleanup:
  `PICK_TARGET -> APPROACH -> HARVEST_OR_PLANT -> AWARD -> END`.
- [ ] Keep final guard for mature wart, air-above planting, and seed availability.
- [ ] Mark site visited after successful harvest or plant.

This behavior is not a pure harvest. Keep the target convention very explicit because the old condition scans soul sand
but the action uses `blockPos.above()`.

### 8. `HarvestOreBehavior.java`

Legacy single-stage behavior; lower priority than farming but follows the same target-cache removal.

- [ ] Add ore matcher from `NearbyOreExistsCondition`.
- [ ] Add `ORE_SITES` memory if ore remains sensor-driven.
- [ ] Replace `NearbyOreExistsCondition` with known-site precondition.
- [ ] Replace `orePos` and `validOresAroundVillager` with resolver + `TARGET` state.
- [ ] Keep final guard that the target still matches ore tags.
- [ ] Preserve replacement block selection before destroying ore.
- [ ] Mark site visited after successful ore replacement.

## Quick review checklist for pull requests

- [ ] Does the behavior constructor avoid owning a scanning `Nearby...Condition`?
- [ ] Does the behavior have no calls to `condition.getTargets()`?
- [ ] Does the behavior have no calls to `condition.stillMatches()`?
- [ ] Does the behavior have no calls to `condition.test()` outside normal precondition ticking?
- [ ] Is the wide scan isolated to a sensor?
- [ ] Is target memory read through `IBrain` / `MemoryType`?
- [ ] Is target selection performed by a resolver, not by a predicate?
- [ ] Does the action step guard the live world state immediately before mutation?
- [ ] Are visited sites tracked for looped behaviors?
- [ ] Are inventory and capacity checks still present as pure gates or action-time guards?
- [ ] Does the behavior end cleanly when memory is stale or another actor changed the target?
