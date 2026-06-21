package dev.breezes.settlements.domain.ai.catalog;

import org.apache.commons.lang3.StringUtils;

/**
 * Type-safe, stable identifier for a registered behavior.
 * <p>
 * Constants are grouped by the theme of the behavior, not by which profession uses them.
 * Profession → behavior mapping lives in {@link ProfessionBehaviorPool}.
 */
public record BehaviorKey(String id) {

    private static final String DISPLAY_NAME_KEY_PREFIX = "ui.settlements.behavior.behavior.";

    // ---- Universal ----
    public static final BehaviorKey EAT_FOOD = new BehaviorKey("eat_food");

    // ---- Logistics (universal) ----
    public static final BehaviorKey TAKE_FROM_CHEST = new BehaviorKey("take_from_chest");
    public static final BehaviorKey DEPOSIT_SURPLUS = new BehaviorKey("deposit_surplus");
    public static final BehaviorKey COLLECT_DEMANDED_ITEM = new BehaviorKey("collect_demanded_item");

    // ---- Social (universal) ----
    public static final BehaviorKey TRADE_INITIATE = new BehaviorKey("trade_initiate");
    public static final BehaviorKey TRADE_ACCEPT = new BehaviorKey("trade_accept");
    public static final BehaviorKey COURTSHIP_INITIATE = new BehaviorKey("courtship_initiate");
    public static final BehaviorKey COURTSHIP_ACCEPT = new BehaviorKey("courtship_accept");

    // ---- Investigate (universal, triggered by planner or override lane) ----
    public static final BehaviorKey INVESTIGATE = new BehaviorKey("investigate");

    // ---- Support / Village ----
    public static final BehaviorKey REPAIR_IRON_GOLEM = new BehaviorKey("repair_iron_golem");

    // ---- Smithing ----
    public static final BehaviorKey BLAST_ORE = new BehaviorKey("blast_ore");

    // ---- Butchering ----
    public static final BehaviorKey SMOKE_MEAT = new BehaviorKey("smoke_meat");
    public static final BehaviorKey BUTCHER_LIVESTOCK = new BehaviorKey("butcher_livestock");

    // ---- Cleric ----
    public static final BehaviorKey THROW_POTIONS = new BehaviorKey("throw_potions");
    public static final BehaviorKey HARVEST_NETHER_WART = new BehaviorKey("harvest_nether_wart");

    // ---- Farming ----
    public static final BehaviorKey HARVEST_SUGARCANE = new BehaviorKey("harvest_sugarcane");
    public static final BehaviorKey COLLECT_HONEY = new BehaviorKey("collect_honey");
    public static final BehaviorKey HARVEST_HONEYCOMB = new BehaviorKey("harvest_honeycomb");
    public static final BehaviorKey HARVEST_PUMPKIN = new BehaviorKey("harvest_pumpkin");
    public static final BehaviorKey HARVEST_MELON = new BehaviorKey("harvest_melon");
    public static final BehaviorKey HARVEST_SWEET_BERRIES = new BehaviorKey("harvest_sweet_berries");
    public static final BehaviorKey HARVEST_RIPE_CROPS = new BehaviorKey("harvest_ripe_crops");

    // ---- Animal handling ----
    public static final BehaviorKey MILK_COW = new BehaviorKey("milk_cow");
    public static final BehaviorKey BREED_CHICKENS = new BehaviorKey("breed_chickens");
    public static final BehaviorKey BREED_COWS = new BehaviorKey("breed_cows");
    public static final BehaviorKey BREED_PIGS = new BehaviorKey("breed_pigs");
    public static final BehaviorKey BREED_SHEEP = new BehaviorKey("breed_sheep");
    public static final BehaviorKey SHEAR_SHEEP = new BehaviorKey("shear_sheep");
    public static final BehaviorKey DYE_SHEEP = new BehaviorKey("dye_sheep");
    public static final BehaviorKey TAME_CAT = new BehaviorKey("tame_cat");
    public static final BehaviorKey TAME_WOLF = new BehaviorKey("tame_wolf");
    public static final BehaviorKey WASH_WOLF = new BehaviorKey("wash_wolf");
    public static final BehaviorKey FEED_WOLF = new BehaviorKey("feed_wolf");

    // ---- Idle / Leisure ----
    public static final BehaviorKey WALK_DOG = new BehaviorKey("walk_dog");

    // ---- Nitwit / Mischief ----
    public static final BehaviorKey RING_BELL = new BehaviorKey("ring_bell");
    public static final BehaviorKey THROW_EGGS = new BehaviorKey("throw_eggs");
    public static final BehaviorKey CHASE_CHICKENS = new BehaviorKey("chase_chickens");

    // ---- Surveying ----
    public static final BehaviorKey SURVEY_LANDSCAPE = new BehaviorKey("survey_landscape");

    // ---- Fishing ----
    public static final BehaviorKey FISHING = new BehaviorKey("fishing");

    // ---- Crafting ----
    public static final BehaviorKey CUT_STONE = new BehaviorKey("cut_stone");
    public static final BehaviorKey HARVEST_ORE = new BehaviorKey("harvest_ore");
    public static final BehaviorKey EXCAVATE_SUBSTRATE = new BehaviorKey("excavate_substrate");

    // ---- Enchanting ----
    public static final BehaviorKey ENCHANT_ITEM = new BehaviorKey("enchant_item");

    // ---- Leatherworking ----
    public static final BehaviorKey WASH_LEATHER = new BehaviorKey("wash_leather");
    public static final BehaviorKey DYE_LEATHER = new BehaviorKey("dye_leather");

    public BehaviorKey {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("BehaviorKey id must not be blank");
        }
    }

    public static BehaviorKey of(String id) {
        return new BehaviorKey(id);
    }

    public String displayNameKey() {
        return DISPLAY_NAME_KEY_PREFIX + id;
    }

    @Override
    public String toString() {
        return id;
    }

}
