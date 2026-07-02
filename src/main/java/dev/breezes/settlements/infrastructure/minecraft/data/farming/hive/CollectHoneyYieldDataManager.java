package dev.breezes.settlements.infrastructure.minecraft.data.farming.hive;

import dev.breezes.settlements.infrastructure.minecraft.data.framework.WeightedYieldDataManager;

import javax.inject.Inject;

public class CollectHoneyYieldDataManager extends WeightedYieldDataManager {

    private static final String DIRECTORY_PATH = "settlements/farming/collect_honey";
    private static final String LABEL = "collect honey yield";

    @Inject
    public CollectHoneyYieldDataManager() {
        super(DIRECTORY_PATH, LABEL);
    }

}
