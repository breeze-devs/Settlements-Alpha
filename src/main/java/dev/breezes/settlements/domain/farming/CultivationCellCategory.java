package dev.breezes.settlements.domain.farming;

/**
 * Describes the cultivation state of one zone cell (ground + canopy pair).
 * <p>
 * The behavior uses this to decide which motion to perform on a cell; the BE uses it
 * as a coarse hint to advertise whether the zone contains actionable work at all.
 */
public enum CultivationCellCategory {

    /**
     * Ground is tillable (in {@code #settlements:tillable}); canopy is air or clearable foliage.
     * Action: optionally clear foliage, then till to farmland, then plant if a seed is available.
     */
    NEEDS_TILL,

    /**
     * Ground is tillable but canopy is blocked by a non-foliage solid.
     * Action: skip — the villager must never destroy non-foliage blocks.
     */
    BLOCKED,

    /**
     * Ground is farmland; canopy is air or clearable foliage (no crop present).
     * Action: optionally clear foliage, then plant if a seed is available.
     */
    NEEDS_PLANT,

    /**
     * Ground is farmland; canopy holds a crop that matches the totem filter (or no filter is set).
     * Action: leave it — global harvest handles ripe crops.
     */
    OCCUPIED,

    /**
     * Ground is farmland; canopy holds a crop that does NOT match the active filter.
     * Action: scythe the off-filter crop, then plant the filter crop if the villager carries its seed.
     */
    NEEDS_REPLANT,

    /**
     * Ground is something non-workable (stone, path, water, …).
     * Action: skip.
     */
    SKIP,

}
