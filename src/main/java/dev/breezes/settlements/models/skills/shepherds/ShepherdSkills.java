package dev.breezes.settlements.models.skills.shepherds;

public enum ShepherdSkills {

    /**
     * Each level increases the number of sheep that can be managed
     * Default: 3
     * Increment: 1
     * Max level: 7
     */
    FLOCK_MANAGEMENT,

    /**
     * Each level increases the amount of wool collected per shear
     * Default: min 1, max 1
     * Increment: max += 0.1 (rounded up)
     * Max level: 15
     */
    SHEARING_MASTERY,

    /**
     * Heals the sheep when fed, each level increases the amount healed
     * If baby sheep are fed, they grow up faster
     * Default healing amount: 2 HP
     * Default speed-up amount: 10 seconds
     * Increment: +1 HP, +5 seconds
     * Max level: 5
     */
    VETERINARY,

    /*
     * Textile artisan
     * - Focuses on crafting advanced textiles and decorations related to wool
     */
    /**
     * Each level unlocks new banner patterns for trading
     * Default: ?
     * Increment: ?
     * Max level: ?
     */
    ADVANCED_LOOM_TECHNIQUES,


    ;

}
