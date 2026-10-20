package dev.breezes.settlements.domain.generation.model.profile;

import org.apache.commons.lang3.Validate;

/**
 * How developed a settlement is.
 */
public enum ScaleTier {

    // TODO: add a city tier for populations past town's range. Until one exists, a headcount far beyond town's maximum
    //  is still observed as a town.
    HAMLET(1, 2, 4, 7, 1, 2, 5, 8, 32),
    VILLAGE(2, 4, 8, 19, 2, 4, 10, 20, 56),
    TOWN(4, 5, 20, 50, 3, 5, 25, 40, 80);

    private final int minTraitSlots;
    private final int maxTraitSlots;
    private final int minPopulation;
    private final int maxPopulation;
    private final int minHistoryEvents;
    private final int maxHistoryEvents;
    private final int minBuildings;
    private final int maxBuildings;
    private final int areaRadius;

    ScaleTier(int minTraitSlots, int maxTraitSlots, int minPopulation, int maxPopulation,
              int minHistoryEvents, int maxHistoryEvents, int minBuildings, int maxBuildings,
              int areaRadius) {
        this.minTraitSlots = minTraitSlots;
        this.maxTraitSlots = maxTraitSlots;
        this.minPopulation = minPopulation;
        this.maxPopulation = maxPopulation;
        this.minHistoryEvents = minHistoryEvents;
        this.maxHistoryEvents = maxHistoryEvents;
        this.minBuildings = minBuildings;
        this.maxBuildings = maxBuildings;
        this.areaRadius = areaRadius;
    }

    /**
     * Returns the appropriate tier of a settlement with the given number of villagers.
     * A headcount below every minimum is a hamlet.
     *
     * @param headcount the villagers counted; not negative
     */
    public static ScaleTier forHeadcount(int headcount) {
        Validate.isTrue(headcount >= 0, "Headcount must not be negative: %d", headcount);
        ScaleTier observed = HAMLET;
        for (ScaleTier tier : values()) {
            if (headcount >= tier.minPopulation && tier.minPopulation > observed.minPopulation) {
                observed = tier;
            }
        }
        return observed;
    }

    public int minTraitSlots() {
        return this.minTraitSlots;
    }

    public int maxTraitSlots() {
        return this.maxTraitSlots;
    }

    /**
     * Returns the fewest villagers a generated settlement of this tier starts with. A settlement whose headcount is
     * observed instead reaches this tier at this count.
     */
    public int minPopulation() {
        return this.minPopulation;
    }

    /**
     * Returns the most villagers a generated settlement of this tier starts with.
     */
    public int maxPopulation() {
        return this.maxPopulation;
    }

    public int minHistoryEvents() {
        return this.minHistoryEvents;
    }

    public int maxHistoryEvents() {
        return this.maxHistoryEvents;
    }

    public int minBuildings() {
        return this.minBuildings;
    }

    public int maxBuildings() {
        return this.maxBuildings;
    }

    public int areaRadius() {
        return this.areaRadius;
    }

}
