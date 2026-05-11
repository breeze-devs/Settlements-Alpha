package dev.breezes.settlements.domain.ai.catalog;

import org.apache.commons.lang3.StringUtils;

/**
 * Type-safe, stable identifier for a registered behavior.
 * <p>
 * Constants are grouped by the theme of the behavior, not by which profession uses them.
 * Profession → behavior mapping lives in {@link ProfessionBehaviorPool}.
 */
public record BehaviorKey(String id) {

    // ---- Universal ----
    public static final BehaviorKey EAT_FOOD = new BehaviorKey("eat_food");

    // ---- Social (universal) ----
    public static final BehaviorKey TRADE_INITIATE = new BehaviorKey("trade_initiate");
    public static final BehaviorKey TRADE_ACCEPT = new BehaviorKey("trade_accept");

    // ---- Support / Village ----
    public static final BehaviorKey REPAIR_IRON_GOLEM = new BehaviorKey("repair_iron_golem");

    // ---- Smithing ----
    public static final BehaviorKey BLAST_ORE = new BehaviorKey("blast_ore");

    // ---- Butchering ----
    public static final BehaviorKey BREED_PIGS = new BehaviorKey("breed_pigs");
    public static final BehaviorKey SMOKE_MEAT = new BehaviorKey("smoke_meat");
    public static final BehaviorKey BUTCHER_LIVESTOCK = new BehaviorKey("butcher_livestock");

    // ---- Cleric ----
    public static final BehaviorKey THROW_POTIONS = new BehaviorKey("throw_potions");
    public static final BehaviorKey HARVEST_SOUL_SAND = new BehaviorKey("harvest_soul_sand");

    // ---- Farming ----
    public static final BehaviorKey HARVEST_SUGARCANE = new BehaviorKey("harvest_sugarcane");
    public static final BehaviorKey COLLECT_HONEY = new BehaviorKey("collect_honey");
    public static final BehaviorKey HARVEST_HONEYCOMB = new BehaviorKey("harvest_honeycomb");
    public static final BehaviorKey MILK_COW = new BehaviorKey("milk_cow");
    public static final BehaviorKey BREED_CHICKENS = new BehaviorKey("breed_chickens");
    public static final BehaviorKey BREED_COWS = new BehaviorKey("breed_cows");

    // ---- Animal handling ----
    public static final BehaviorKey TAME_WOLF = new BehaviorKey("tame_wolf");
    public static final BehaviorKey TAME_CAT = new BehaviorKey("tame_cat");
    public static final BehaviorKey SHEAR_SHEEP = new BehaviorKey("shear_sheep");

    // ---- Idle / Leisure ----
    public static final BehaviorKey WALK_DOG = new BehaviorKey("walk_dog");
    public static final BehaviorKey RING_BELL = new BehaviorKey("ring_bell");

    // ---- Fishing ----
    public static final BehaviorKey FISHING = new BehaviorKey("fishing");

    // ---- Crafting ----
    public static final BehaviorKey CUT_STONE = new BehaviorKey("cut_stone");
    public static final BehaviorKey HARVEST_ORE = new BehaviorKey("harvest_ore");

    // ---- Enchanting ----
    public static final BehaviorKey ENCHANT_ITEM = new BehaviorKey("enchant_item");

    public BehaviorKey {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("BehaviorKey id must not be blank");
        }
    }

    public static BehaviorKey of(String id) {
        return new BehaviorKey(id);
    }

    @Override
    public String toString() {
        return id;
    }

}
