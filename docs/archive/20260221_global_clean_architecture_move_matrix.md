# Global Clean Architecture Move Matrix (Entire Project)

Date: 2026-02-21  
Scope: `src/main/java/dev/breezes/settlements/**` (non-`_v2` code)

This guide is meant for **IntelliJ Move Refactor** (so package declarations/imports are updated automatically).

---

## 1) Recommended Move Order (Low Risk)

1. `annotations`, `logging`, `util` (shared utilities first)
2. `genetics`, `inventory`, `tags`, and domain-heavy `models` subpackages
3. infra-heavy packages (`packet`, `mixins`, `datagen`, `bubbles`, most `entities`, `sounds`)
4. `configurations` split (`annotations` + `ConfigFactory` first, then constants/ui)
5. behavior engine + AI application (`models/behaviors`, `models/brain`, `models/sensors`)
6. bootstrap wiring (`registry`, `event`, `SettlementsMod`) last

Run `./gradlew.bat compileJava` after each phase.

---

## 2) Top-Level Package Move Map

| Current             | Move to                                                                                                   |
|---------------------|-----------------------------------------------------------------------------------------------------------|
| `annotations/**`    | `_v2/shared/annotations/**`                                                                               |
| `bubbles/**`        | `_v2/infrastructure/rendering/bubbles/**`                                                                 |
| `client/**`         | `_v2/presentation/client/**`                                                                              |
| `commands/**`       | `_v2/presentation/commands/**`                                                                            |
| `configurations/**` | split across `_v2/infrastructure/config/**`, `_v2/application/config/**`, `_v2/presentation/ui/config/**` |
| `datagen/**`        | `_v2/infrastructure/datagen/**`                                                                           |
| `entities/**`       | mostly `_v2/infrastructure/minecraft/entities/**` (plus 2 contracts to domain; see below)                 |
| `event/**`          | `_v2/bootstrap/event/**`                                                                                  |
| `genetics/**`       | `_v2/domain/genetics/**`                                                                                  |
| `inventory/**`      | `_v2/domain/inventory/**`                                                                                 |
| `logging/**`        | `_v2/shared/logging/**`                                                                                   |
| `mixins/**`         | `_v2/infrastructure/minecraft/mixins/**`                                                                  |
| `models/**`         | split across `_v2/domain/ai/**`, `_v2/application/ai/**`, `_v2/infrastructure/minecraft/**`               |
| `packet/**`         | `_v2/infrastructure/network/packet/**`                                                                    |
| `particles/**`      | `_v2/bootstrap/registry/particles/**`                                                                     |
| `registry/**`       | `_v2/bootstrap/registry/**`                                                                               |
| `sounds/**`         | split: infra audio + bootstrap sound registry                                                             |
| `tags/**`           | `_v2/domain/tags/**`                                                                                      |
| `util/**`           | mostly `_v2/shared/util/**` (except `Ticks`, `TimeOfDay` → `_v2/domain/time/**`)                          |

---

## 3) Explicit Non-`models` Moves

## Root

- `SettlementsMod.java` → `_v2/bootstrap/mod/SettlementsMod.java`
- `package-info.java` → keep at root for now

## configurations

- `configurations/ConfigFactory.java` → `_v2/infrastructure/config/factory/ConfigFactory.java`
- `configurations/annotations/**` → `_v2/infrastructure/config/annotations/**`
- `configurations/constants/BehaviorConfigConstants.java` →
  `_v2/application/config/constants/BehaviorConfigConstants.java`
- `configurations/ui/ConfigScreen.java` → `_v2/presentation/ui/config/ConfigScreen.java`

## entities

- `entities/ISettlementsBrainEntity.java` → `_v2/domain/ai/brain/ISettlementsBrainEntity.java`
- `entities/villager/ISettlementsVillager.java` → `_v2/domain/entities/ISettlementsVillager.java`
- all other `entities/**` → `_v2/infrastructure/minecraft/entities/**`

## registry

- `registry/CreativeTabRegistry.java` → `_v2/bootstrap/registry/tabs/CreativeTabRegistry.java`
- `registry/EntityRegistry.java` → `_v2/bootstrap/registry/entities/EntityRegistry.java`
- `registry/ItemRegistry.java` → `_v2/bootstrap/registry/items/ItemRegistry.java`

## sounds

- `sounds/SoundRegistry.java` → `_v2/bootstrap/registry/sounds/SoundRegistry.java`
- `sounds/ChainedPlayable.java` → `_v2/infrastructure/audio/ChainedPlayable.java`
- `sounds/IPlayable.java` → `_v2/infrastructure/audio/IPlayable.java`
- `sounds/SoundEventPlayable.java` → `_v2/infrastructure/audio/SoundEventPlayable.java`

## util exceptions

- `util/Ticks.java` → `_v2/domain/time/Ticks.java`
- `util/TimeOfDay.java` → `_v2/domain/time/TimeOfDay.java`
- all remaining `util/**` → `_v2/shared/util/**`

---

## 4) `models/**` Detailed Move Matrix

## models/behaviors

### domain contracts/model

- `models/behaviors/IBehavior.java` → `_v2/domain/ai/behavior/contracts/IBehavior.java`
- `models/behaviors/IBehaviorCooldown.java` → `_v2/domain/ai/behavior/contracts/IBehaviorCooldown.java`
- `models/behaviors/BehaviorDefinition.java` → `_v2/domain/ai/behavior/model/BehaviorDefinition.java`
- `models/behaviors/BehaviorStatus.java` → `_v2/domain/ai/behavior/model/BehaviorStatus.java`

### runtime

- `models/behaviors/AbstractBehavior.java` → `_v2/application/ai/behavior/runtime/AbstractBehavior.java`
- `models/behaviors/BaseVillagerBehavior.java` → `_v2/application/ai/behavior/runtime/BaseVillagerBehavior.java`
- `models/behaviors/StateMachineBehavior.java` → `_v2/application/ai/behavior/runtime/StateMachineBehavior.java`
- `models/behaviors/StopBehaviorException.java` → `_v2/application/ai/behavior/runtime/StopBehaviorException.java`

### workflow engine

- `models/behaviors/stages/StagedStep.java` → `_v2/application/ai/behavior/workflow/staged/StagedStep.java`
- `models/behaviors/steps/**` → `_v2/application/ai/behavior/workflow/steps/**`
- `models/behaviors/states/**` → `_v2/application/ai/behavior/workflow/state/**`

### adapter

- `models/behaviors/DefaultBehaviorAdapter.java` →
  `_v2/infrastructure/minecraft/behavior/adapter/DefaultBehaviorAdapter.java`

### use-cases (keep config beside behavior)

#### animals

- `BreedAnimalsBehavior.java`, `BreedAnimalsConfig.java`
- `ShearSheepBehaviorV2.java`, `ShearSheepConfig.java`
- `TameWolfBehaviorV2.java`, `TameWolfConfig.java`

→ `_v2/application/ai/behavior/usecases/villager/animals/`

#### farming

- `HarvestSugarCaneBehavior.java`, `HarvestSugarCaneConfig.java`
- `HarvestSoulSandBehavior.java`, `HarvestSoulSandConfig.java`

→ `_v2/application/ai/behavior/usecases/villager/farming/`

#### support

- `RepairIronGolemBehavior.java`, `RepairIronGolemConfig.java`
- `ThrowPotionsBehavior.java`, `ThrowPotionsConfig.java`

→ `_v2/application/ai/behavior/usecases/villager/support/`

#### crafting

- `CutStoneBehavior.java`, `CutStoneConfig.java`

→ `_v2/application/ai/behavior/usecases/villager/crafting/`

#### smelting

- `BlastOreBehavior.java`, `BlastOreConfig.java`

→ `_v2/application/ai/behavior/usecases/villager/smelting/`

## models/blocks

- `models/blocks/**` → `_v2/domain/world/blocks/**`

## models/brain

- `models/brain/IBrain.java` → `_v2/domain/ai/brain/IBrain.java`
- `models/brain/DefaultBrain.java` → `_v2/application/ai/brain/DefaultBrain.java`
- `models/brain/CustomBehaviorPackages.java` → `_v2/application/ai/brain/CustomBehaviorPackages.java`
- `models/brain/CustomMemoryModuleType.java` → `_v2/infrastructure/minecraft/ai/brain/CustomMemoryModuleType.java`

## models/conditions

- `models/conditions/**` → `_v2/domain/ai/conditions/**`

## models/entities

- `models/entities/SettlementsEntity.java` → `_v2/domain/entities/SettlementsEntity.java`

## models/exceptions

- `models/exceptions/SpawnFailedException.java` → `_v2/domain/exceptions/SpawnFailedException.java`

## models/location

- `models/location/Location.java` → `_v2/domain/world/location/Location.java`
- `models/location/Vector.java` → `_v2/domain/world/location/Vector.java`

## models/memory

- `models/memory/**` → `_v2/domain/ai/memory/**`
- includes:
    - `entry/**` → `_v2/domain/ai/memory/entry/**`
    - `serializers/**` → `_v2/domain/ai/memory/serializers/**`
    - `temp/**` → `_v2/domain/ai/memory/temp/**`

## models/misc

- `models/misc/ITickable.java` → `_v2/domain/time/ITickable.java`
- `models/misc/Tickable.java` → `_v2/domain/time/Tickable.java`
- `models/misc/RandomRangeTickable.java` → `_v2/domain/time/RandomRangeTickable.java`
- `models/misc/Expertise.java` → `_v2/domain/entities/Expertise.java`

## models/navigation

- `models/navigation/INavigationManager.java` → `_v2/domain/ai/navigation/INavigationManager.java`
- `models/navigation/VanillaBasicNavigationManager.java` →
  `_v2/infrastructure/minecraft/navigation/VanillaBasicNavigationManager.java`
- `models/navigation/VanillaMemoryNavigationManager.java` →
  `_v2/infrastructure/minecraft/navigation/VanillaMemoryNavigationManager.java`

## models/schedule

- `models/schedule/ISchedule.java` → `_v2/domain/ai/schedule/ISchedule.java`
- `models/schedule/IScheduleProvider.java` → `_v2/domain/ai/schedule/IScheduleProvider.java`
- `models/schedule/ActivityCategory.java` → `_v2/domain/ai/schedule/ActivityCategory.java`
- `models/schedule/routines/**` → `_v2/domain/ai/schedule/routines/**`
- `models/schedule/providers/VanillaScheduleProvider.java` →
  `_v2/infrastructure/minecraft/ai/schedule/VanillaScheduleProvider.java`

## models/sensors

- contracts:
    - `models/sensors/ISensor.java`
    - `models/sensors/INearbySensor.java`
    - `models/sensors/INearbyEntitySensor.java`

→ `_v2/domain/ai/sensors/`

- application impls:
    - `models/sensors/AbstractSensor.java`
    - `models/sensors/NearestSugarcaneSensor.java`

→ `_v2/application/ai/sensors/`

- result model:
    - `models/sensors/result/**` → `_v2/domain/ai/sensors/result/**`

## models/skills

- `models/skills/**` → `_v2/domain/skills/**`

---

## 5) IntelliJ Move Checklist

1. Move package group using **Refactor > Move** (do not cut/paste in file explorer).
2. Let IntelliJ update package + imports.
3. Run `./gradlew.bat compileJava`.
4. Fix red code before starting next group.
5. After all moves: run `./gradlew.bat test`.
6. Search and remove stale imports/references:
    - `dev.breezes.settlements.models.`
    - `dev.breezes.settlements.configurations.`
    - any legacy package roots no longer used.

---

## 6) Migration Completion Summary (2026-02-21)

### Status

✅ **Global package migration is complete.**

The legacy non-`_v2` Java footprint has been reduced to:

- `src/main/java/dev/breezes/settlements/SettlementsMod.java`
- `src/main/java/dev/breezes/settlements/package-info.java`

Both are acceptable to keep at root (`SettlementsMod` as entry point + `package-info` metadata).

### Final architecture state

- Domain contracts/models are now under `_v2/domain/**`
- Application behavior/brain workflow is under `_v2/application/**`
- Infrastructure concerns (minecraft adapters/entities/network/rendering/config/mixins) are under `_v2/infrastructure/**`
- Bootstrap wiring is under `_v2/bootstrap/**`
- Shared cross-cutting utilities/logging/annotations are under `_v2/shared/**`

### Notable completion points

- `models/**` has been fully migrated to `_v2` destinations.
- `entities/**` infra implementations moved to `_v2/infrastructure/minecraft/entities/**`.
- `mixins/**` moved to `_v2/infrastructure/minecraft/mixins/**`.
- `src/main/resources/settlements.mixins.json` package updated to:
  - `dev.breezes.settlements._v2.infrastructure.minecraft.mixins`

### Optional follow-up refinements (non-blocking)

1. **Condition package split** (for readability at scale):
   - `_v2/domain/ai/conditions/contracts/`
   - `_v2/domain/ai/conditions/nearby/`
   - `_v2/domain/ai/conditions/memory/`
   - `_v2/domain/ai/conditions/inventory/`
   - `_v2/domain/ai/conditions/rules/`
2. **Domain purity hardening:**
   - Remove remaining infra type references from domain-facing interfaces where feasible (e.g., `ISettlementsVillager` depending on `BaseVillager`).

### Suggested verification commands

- `./gradlew.bat compileJava`
- `./gradlew.bat test`


