# Implementation Plan - Simplified Villager Inventory

**Goal**: Implement the simplified inventory system using a common `Inventory` interface and `SimpleInventory` implementation for `BACKPACK`, `MAIN_HAND`, and `OFF_HAND`.

## User Review Required
> [!IMPORTANT]
> **Breaking Change**: This replaces the standard inventory logic for `BaseVillager`. Existing behaviors relying on vanilla `getInventory()` or `SimpleContainer` might break if they assume specific slot indices.

## Proposed Changes

### Core Inventory Logic [NEW]
#### [Modify] [Inventory.java](file:///c:/one/workspace/javaspace/Settlements-Alpha/src/main/java/dev/breezes/settlements/_v2/inventory/Inventory.java)
- Define standard operations:
  - `ItemStack insert(ItemStack stack)`
  - `ItemStack extract(Predicate<ItemStack> filter, int amount)`
  - `boolean has(Predicate<ItemStack> filter)`
  - `int count(Predicate<ItemStack> filter)`
  - `List<ItemStack> view()`
  - `int getCapacity()`
  - `boolean isEmpty()`

#### [NEW] [SimpleInventory.java](file:///c:/one/workspace/javaspace/Settlements-Alpha/src/main/java/dev/breezes/settlements/_v2/inventory/SimpleInventory.java)
- Implements `Inventory`.
- Fields: `List<ItemStack> stacks`, `int capacity`.
- Logic: Standard stack merging and slot management.

### Integration
#### [MODIFY] [BaseVillager.java](file:///c:/one/workspace/javaspace/Settlements-Alpha/src/main/java/dev/breezes/settlements/entities/villager/BaseVillager.java)
- **New Fields**:
  - `Inventory backpack` (Capacity: dynamic based on strength)
  - `Inventory mainHand` (Capacity: 1)
  - `Inventory offHand` (Capacity: 1)
- **Refactoring**:
  - `getInventory()` -> Redirect to `backpack` (or deprecate).
  - `setItemInHand` -> Redirect to `mainHand` / `offHand`.
  - `pickUpItem` -> Try `mainHand` (if empty/matches) -> `offHand` (if empty/matches) -> `backpack`.
  - **Deadlock Policy**: In `pickUpItem` or behavior-level swaps: if target inventory full, drop item.

## Verification Plan

### Automated Tests
- **Test File**: `src/test/java/dev/breezes/settlements/_v2/inventory/SimpleInventoryTest.java`
- **Scenarios**:
  - `testInsertStackMerging`: Verify items merge into existing stacks.
  - `testInsertCapacityLimit`: Verify remainder returned when full.
  - `testExtractPredicate`: Verify correct items removed.
  - `testSingleSlotBehavior`: Verify behavior for hand-like inventories (capacity=1).

### Manual Verification
1.  **Spawn Villager**: Verify it spawns without crashing.
2.  **Item Pickup**: Throw items. Verify they go to backpack.
3.  **Hand Interaction**: Verify behaviors can hold items.
4.  **Overflow**: Fill backpack, give item -> Verify pickup rejection or drop logic.
