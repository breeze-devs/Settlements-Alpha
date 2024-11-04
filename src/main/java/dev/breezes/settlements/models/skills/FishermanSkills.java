package dev.breezes.settlements.models.skills;

public enum FishermanSkills {

    /**
     * Each level shortens the time it takes to catch a fish
     * Default: min 30 seconds, max 180 seconds
     * - Level 1: min 30 seconds, max 160 seconds
     * - Level 2: min 30 seconds, max 140 seconds
     * - Level 3: min 30 seconds, max 120 seconds
     * - Level 4: min 30 seconds, max 100 seconds
     * - Level 5: min 20 seconds, max 80 seconds
     */
    ANGLING,

    /**
     * Each level increases the chance of catching rare fish or treasure
     * <p>
     * Luck: each 100% luck allows catching of items of the next rarity tier
     * - i.e. 0% < x <= 100% = common, 100% < x <= 200% = uncommon, 200% < x <= 300% = rare, etc
     * <p>
     * Default: 2% chance; 40% luck
     * - Level 1: 3% chance; 80% luck
     * - Level 2: 4% chance; 120% luck
     * - Level 3: 5% chance; 160% luck
     * - Level 4: 7% chance; 200% luck
     * - Level 5: 10% chance; 250% luck
     */
    BOUNTIES_OF_THE_SEAS,

    /**
     * Nearby players and villagers gain a minor speed boost and haste when it's raining
     * - Level 1: Speed I
     * - Level 2: Speed II
     * - Level 3: Speed II, Haste I
     */
    AQUA_AFFINITY,

    /*
     * Specialization: Pirate (defensive specialization)
     * - focuses on defending the village using available water bodies
     */
    /**
     * Allows the use of tridents as a weapon
     * - Level 1: Allows the villager to use tridents as a projectile weapon
     * - Level 2: Enhances trident damage
     * - Level 3: Further enhances trident damage, nearby players gain Strength II in raids
     */
    AQUA_AGGRESSION,

    /**
     * Allows the use of boats
     * - Single level
     */
    SAILING,

    /*
     * Specialization: Aquaculturist (trading specialization)
     * - focuses on obtaining aquatic resources and can breed aquatic mobs
     */
    /**
     * Each level grants new trades related to aquatic resources
     * - Level 1: Low rarity items such as kelp and sea pickles
     * - Level 2: Medium rarity items such as coral and heart of the sea
     * - Level 3: High rarity items such as turtle eggs and dolphin's token? (can be taken to clerics for dolphin's grace potions)
     */
    AQUATIC_FORAGING,

    /**
     * Allows the breeding of aquatic mobs such as turtles and dolphins
     * - Single level
     */
    MARINE_HUSBANDRY,

    /*
     * Specialization:
     *  (economic specialization)
     * - focuses on catching and selling fish
     */
    /**
     * Unlocks taming of cats. Each level increases the drops from cats every night
     * - Level 1: Allows the villager to tame 1 cat as a companion
     * - Level 2: Cats may bring back fish or other items at night
     * - Level 3: Cats may bring back rare items at night
     */
    ANGLERS_ALLY,

    /**
     * Allows the trading of all fish-in-a-bucket items
     * - Single level
     */
    AQUATIC_PRESERVATION,

}
