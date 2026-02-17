package dev.breezes.settlements.inventory;

import dev.breezes.settlements.genetics.GeneType;
import dev.breezes.settlements.genetics.GeneticsProfile;
import lombok.CustomLog;
import net.minecraft.util.Mth;

/**
 * Provides inventory size based on genetic attributes.
 */
@CustomLog
public class GeneticInventoryProvider {

    public static final int MINIMUM_INVENTORY_SIZE = 6;
    public static final int MAXIMUM_INVENTORY_SIZE = 54;

    // Curve tuning
    public static final double GROWTH_EXPONENT = 1.5;
    public static final double CONSTITUTION_BONUS = 0.24;

    public VillagerInventory provideDefault() {
        return new VillagerInventory(MINIMUM_INVENTORY_SIZE);
    }

    public VillagerInventory provide(GeneticsProfile profile) {
        double strength = profile.getGeneValue(GeneType.STRENGTH);
        double constitution = profile.getGeneValue(GeneType.CONSTITUTION);

        int size = this.calculateInventorySize(strength, constitution);
        log.debug("Creating inventory of size {} based on genetics (STR: {}, CON: {})", size, strength, constitution);
        return new VillagerInventory(size);
    }

    private int calculateInventorySize(double strength, double constitution) {
        double effective = Mth.clamp(strength + CONSTITUTION_BONUS * constitution * (1.0 - strength), 0, 1);

        int range = MAXIMUM_INVENTORY_SIZE - MINIMUM_INVENTORY_SIZE;
        double curved = Math.pow(effective, GROWTH_EXPONENT);

        return MINIMUM_INVENTORY_SIZE + (int) Math.round(curved * range);
    }

}
