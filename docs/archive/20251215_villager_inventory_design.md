# Villager Inventory Design (BACKPACK, MAIN_HAND, OFF_HAND)

- Date: 2025-12-15
- Status: Design in review (Simplified)
- Owner: Settlements-Alpha

## Goals
- **Simplified Storage**: 3 explicit compartments: `BACKPACK`, `MAIN_HAND`, `OFF_HAND`. No `BELT`.
- **Interface-Driven**: All compartments implement a common `Inventory` interface.
- **Dynamic Capacity**: BACKPACK capacity scales with traits; HANDs are 1 slot fixed.
- **No Auto-Routing**: Behaviors explicitly move items between compartments.

## Compartments
- **BACKPACK**
  - Purpose: General storage for everything (tools, food, etc.).
  - Capacity: Stack-based, dynamic (Base 6 + 3 × strengthTier, clamped to [6, 54]).
  - Filter: Accepts any item.
- **MAIN_HAND**
  - Purpose: Active item.
  - Capacity: 1 stack.
  - Filter: Accepts any item (behavior policy decides usage).
- **OFF_HAND**
  - Purpose: Secondary item.
  - Capacity: 1 stack.
  - Filter: Accepts any item (behavior policy decides usage).

## API & Data Structures
- **Interface**: `Inventory` (common operations)
  - `ItemStack insert(ItemStack stack)`
  - `ItemStack extract(Predicate<ItemStack> filter, int amount)`
  - `boolean has(Predicate<ItemStack> filter)`
  - `int count(Predicate<ItemStack> filter)`
  - `List<ItemStack> view()`

- **Implementation**: `SimpleInventory` (implements `Inventory`)
  - Used for generic storage with a fixed or dynamic slot count.

- **Villager Model**:
  - `SimpleInventory backpack`
  - `SimpleInventory mainHand` (Size 1)
  - `SimpleInventory offHand` (Size 1)

## Semantics
- **Insert**: Returns remainder. Fills existing stacks first, then empty slots.
- **Extract**: Removes types matching predicate.
- **Routing**: Explicit only. To equip a tool, behavior must:
  1. `backpack.extract(toolFilter)`
  2. `mainHand.insert(extractedTool)` (handling swap/overflow if hand full)

## Deadlock Resolution (Full Backpack)
- If a villager must swap Hand <-> Backpack but Backpack is full:
  - The item currently in Hand is **dropped** to the world.

## Decisions
1) **No Belt**: Removed to reduce complexity. Tools live in Backpack.
2) **Common Interface**: `Inventory` used for all compartments.
3) **SimpleInventory**: Concrete class for storage logic.
