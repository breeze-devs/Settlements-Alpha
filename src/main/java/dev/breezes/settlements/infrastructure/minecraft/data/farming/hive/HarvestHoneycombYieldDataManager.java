package dev.breezes.settlements.infrastructure.minecraft.data.farming.hive;

import dev.breezes.settlements.infrastructure.minecraft.data.framework.WeightedYieldDataManager;

import jakarta.inject.Inject;

public class HarvestHoneycombYieldDataManager extends WeightedYieldDataManager {

    private static final String DIRECTORY_PATH = "settlements/farming/harvest_honeycomb";
    private static final String LABEL = "harvest honeycomb yield";

    @Inject
    public HarvestHoneycombYieldDataManager() {
        super(DIRECTORY_PATH, LABEL);
    }

}
