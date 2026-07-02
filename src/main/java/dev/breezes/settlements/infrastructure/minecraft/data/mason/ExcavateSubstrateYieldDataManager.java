package dev.breezes.settlements.infrastructure.minecraft.data.mason;

import dev.breezes.settlements.infrastructure.minecraft.data.framework.WeightedYieldDataManager;

import javax.inject.Inject;

public class ExcavateSubstrateYieldDataManager extends WeightedYieldDataManager {

    private static final String DIRECTORY_PATH = "settlements/mason/excavate_substrate";
    private static final String LABEL = "mason shovel yield";

    @Inject
    public ExcavateSubstrateYieldDataManager() {
        super(DIRECTORY_PATH, LABEL);
    }

}
