# Villager Genetics System Design

## Overview

This document outlines the design for a genetics system for Minecraft villagers. The goal is to add depth by giving
villagers unique, heritable attributes (genes) that influence their capabilities and behavior.

## Core Mechanics

### 1. Gene Structure

Genes are distinct from vanilla Attributes. They are static "innate potential" values determined at birth.

* **Scale**: Normalized float value `0.0` to `1.0`.
* **Generation**:
    * **New Spawns**: Randomized using a bell curve (mean 0.5, variance 0.15), clamping to [0.0, 1.0].
    * **Inheritance**: Simple Crossover + Mutation (see below).

### 2. Proposed Attributes & Tiers

Each gene has tiers that unlock passive effects or debuffs.

| Gene                   | Impact                                        | Low Tier (< 0.2) [Debuff]                                 | High Tier (> 0.70) [Buff]              | Ultra Tier (> 0.90) [Legendary]                                          |
|:-----------------------|:----------------------------------------------|:----------------------------------------------------------|:---------------------------------------|:-------------------------------------------------------------------------|
| **Strength (STR)**     | Inventory Capacity (Weight/Slots), Attack Dmg | **Weakling**: -1 Inventory Slot. Flee priority increased. | **Heavy Lifter**: +Inventory Capacity. | **Enforcer**: Chance to stun enemies. Attacks monsters.                  |
| **Constitution (CON)** | Max HP Mod, Regen Speed                       | **Frailty**: -Max HP. Slower Regen.                       | **Robust**: +Max HP.                   | **Ironclad**: Natural Armor Points. Immune to Food Poisoning.            |
| **Agility (AGI)**      | Movement Speed Mod                            | **Lethargic**: -Speed. Slower work cycles.                | **Athletic**: +Speed.                  | **Sprinter**: Burst speed when fleeing. % Dodge chance.                  |
| **Intelligence (INT)** | XP Gain, Restock Rates                        | **Dim**: +Trade Prices. Slower XP.                        | **Astute**: -Trade Prices.             | **Savant**: Unlocks **Exclusive Trades** (Items not normally available). |

### 3. Inheritance (Crossover Algorithm)

Instead of complex Mendelian genetics, we use a "blending" approach with mutation.

* **Formula**:
  ```java
  // Simple Average Crossover
  float parentAvg = (parentA.gene + parentB.gene) / 2.0f;
  
  // Mutation: Small shift [-0.1, +0.1]
  float mutation = (random.nextFloat() * 0.2f) - 0.1f;
  
  // Chance for "Wild Mutation" (1% chance to randomize completely)
  if (random.nextFloat() < 0.01) {
      childFit = random.nextFloat(); 
  } else {
      childFit = clamp(parentAvg + mutation, 0.0, 1.0);
  }
  ```

### 4. Integration

* **Storage**:
    * Stored via NeoForge `DataAttachment` (Generic NBT) on `Villager` entities.
    * Data persists across world saves.
* **Visuals/UI**:
    * **Gene Analyzer**: Item (e.g., clickable on villager) to show a GUI or Chat readout of their genes.
    * **Visual Cues**: Optional (future) - particles or slight scale modifiers for extremes (giant strong villagers).

## Implementation Plan

1. **Data Structure**: Create `GeneticsData` record/class holding the float values.
2. **Attachment**: Register NeoForge attachment `GENETICS` for `EntityType.VILLAGER`.
3. **Logic**: Implement `GeneticsCalculator` for random generation and breeding crossover.
4. **Events**:
    * `EntityJoinLevelEvent`: Initialize genes for fresh spawns if missing.
    * `BabyEntitySpawnEvent`: Intercept breeding to apply inheritance logic.
5. **Effects**:
    * Register Attribute Modifiers for vanilla attributes (Max Health, Movement Speed) driven by Genes.
    * Custom logic for non-attribute effects (Inventory slots, Trades).
