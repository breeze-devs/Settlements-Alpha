package dev.breezes.settlements.models;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.RepairIronGolemBehavior;
import dev.breezes.settlements.models.conditions.GameRuleBooleanCondition;
import dev.breezes.settlements.models.conditions.MemoryPresentCondition;
import dev.breezes.settlements.models.memory.MemoryTypeRegistry;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.GameRules;

import java.util.function.Predicate;

public final class Test {

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        RepairIronGolemBehavior behavior = new RepairIronGolemBehavior();
    }

    private static void testPreconditions() {
        GameRuleBooleanCondition<BaseVillager> mobGriefingEnabledCondition = GameRuleBooleanCondition.<BaseVillager>builder()
                .gameRule(GameRules.RULE_MOBGRIEFING)
                .expectedValue(true)
                .build();
        MemoryPresentCondition<BaseVillager, GlobalPos> sugarcaneMemoryConditionNoPredicate = MemoryPresentCondition.<BaseVillager, GlobalPos>builder()
                .memoryType(MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE)
                .build();
        Predicate<BaseVillager> sugarcaneMemoryConditionNoPredicateNegated = sugarcaneMemoryConditionNoPredicate.negate();

        MemoryPresentCondition<BaseVillager, GlobalPos> sugarcaneMemoryConditionWithPredicate = MemoryPresentCondition.<BaseVillager, GlobalPos>builder()
                .memoryType(MemoryTypeRegistry.NEAREST_HARVESTABLE_SUGARCANE)
                .memoryValuePredicate(pos -> pos.dimension().toString().equals("minecraft:overworld"))
                .build();


        System.out.println("mobGriefingEnabledCondition: " + mobGriefingEnabledCondition.test(null));
        System.out.println("sugarcaneMemoryConditionNoPredicate: " + sugarcaneMemoryConditionNoPredicate.test(null));
        System.out.println("sugarcaneMemoryConditionNoPredicateNegated: " + sugarcaneMemoryConditionNoPredicateNegated.test(null));
        System.out.println("sugarcaneMemoryConditionWithPredicate: " + sugarcaneMemoryConditionWithPredicate.test(null));
    }

}
