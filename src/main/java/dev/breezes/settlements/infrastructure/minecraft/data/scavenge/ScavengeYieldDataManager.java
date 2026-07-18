package dev.breezes.settlements.infrastructure.minecraft.data.scavenge;

import dev.breezes.settlements.infrastructure.minecraft.data.framework.WeightedYieldDataManager;

import jakarta.inject.Inject;

public class ScavengeYieldDataManager extends WeightedYieldDataManager {

    private static final String DIRECTORY_PATH = "settlements/scavenge";
    private static final String LABEL = "scavenge forage yield";

    @Inject
    public ScavengeYieldDataManager() {
        super(DIRECTORY_PATH, LABEL);
    }

}
